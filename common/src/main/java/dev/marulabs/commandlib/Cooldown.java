package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A per-sender cooldown for {@link CommonCommand#cooldown(Cooldown)}.
 * <p>
 * The cooldown starts when the command passes its middlewares and is about to execute, and keeps the same sender from
 * executing it again until it ends. While an execution that passed the check is still in the middlewares, such as one
 * waiting for an asynchronous lookup, the sender's other executions are rejected as well. Senders are told apart by their unique id, so only senders that have one, such as
 * players and entities, are throttled; the console, remote consoles and command blocks are never on cooldown. Commands
 * given the same instance share one cooldown. Keep the instance to query or end a sender's cooldown with
 * {@link #remaining(UUID)} and {@link #reset(UUID)}. The state lives in memory and is lost when the server stops.
 */
public final class Cooldown {
    private final Duration duration;
    private final Map<UUID, Instant> endsAt = new ConcurrentHashMap<>();
    // The executions that passed the check and have not reached the start yet, by sender, with the time they passed.
    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();
    private @Nullable String bypassPermission = null;
    private BiConsumer<CommonCommandContext<?, ?>, Duration> message = (ctx,
                                                                        remaining) -> ctx.sendFailure(Caption.COOLDOWN,
                                                                                                      Map.of("remaining",
                                                                                                             format(remaining,
                                                                                                                    ctx.captions(),
                                                                                                                    ctx.getLocale())));
    private Clock clock = Clock.systemUTC();

    private Cooldown(Duration duration) {
        this.duration = duration;
    }

    /**
     * Creates a cooldown of the given length.
     *
     * @throws IllegalArgumentException if {@code duration} is zero or negative
     */
    @NotNull
    public static Cooldown of(@NotNull Duration duration) {
        Objects.requireNonNull(duration);
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
        return new Cooldown(duration);
    }

    /**
     * Lets senders with the given permission skip the cooldown.
     */
    @NotNull
    public Cooldown bypassPermission(@NotNull String permission) {
        this.bypassPermission = Objects.requireNonNull(permission);
        return this;
    }

    /**
     * Replaces the message sent to a sender on cooldown. The consumer receives the time left.
     */
    @NotNull
    public Cooldown message(@NotNull BiConsumer<CommonCommandContext<?, ?>, Duration> message) {
        this.message = Objects.requireNonNull(message);
        return this;
    }

    /**
     * Replaces the clock the cooldown reads the time from, such as a fixed clock in tests.
     */
    @NotNull
    public Cooldown clock(@NotNull Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        return this;
    }

    /**
     * Returns the time left before the sender with {@code id} may execute the commands again, zero when it is not on
     * cooldown.
     */
    @NotNull
    public Duration remaining(@NotNull UUID id) {
        Instant end = endsAt.get(Objects.requireNonNull(id));
        Instant now = clock.instant();
        return end == null || !now.isBefore(end) ? Duration.ZERO : Duration.between(now, end);
    }

    /**
     * Returns the time left for the sender of {@code ctx}, zero when it is not on cooldown, has no unique id, or has
     * the bypass permission.
     */
    @NotNull
    public Duration remaining(@NotNull CommonCommandContext<?, ?> ctx) {
        return throttledId(ctx).map(this::remaining)
                               .orElse(Duration.ZERO);
    }

    /**
     * Ends the cooldown of the sender with {@code id}, so it may execute the commands again at once.
     */
    public void reset(@NotNull UUID id) {
        endsAt.remove(Objects.requireNonNull(id));
        reservations.remove(id);
    }

    /**
     * Ends the cooldown of every sender.
     */
    public void resetAll() {
        endsAt.clear();
        reservations.clear();
    }

    /**
     * Returns false, after sending the message, when the sender is on cooldown or another of its executions passed the
     * check and has not started the cooldown yet. Otherwise reserves the cooldown for this execution until
     * {@link #start} or {@link #release}.
     */
    boolean reserve(CommonCommandContext<?, ?> ctx) {
        Duration remaining = remaining(ctx);
        if (!remaining.isZero()) {
            message.accept(ctx, remaining);
            return false;
        }
        Optional<UUID> id = throttledId(ctx);
        if (id.isEmpty()) {
            return true;
        }
        Instant now = clock.instant();
        Reservation reservation = new Reservation(ctx, now);
        // A reservation as old as the cooldown is dropped, so a middleware that never finishes does not keep the
        // sender waiting forever.
        Reservation held = reservations.merge(id.get(),
                                              reservation,
                                              (old, x) -> now.isBefore(old.at.plus(duration)) ? old : x);
        if (held != reservation) {
            message.accept(ctx, duration);
            return false;
        }
        return true;
    }

    void start(CommonCommandContext<?, ?> ctx) {
        Optional<UUID> id = throttledId(ctx);
        if (id.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        // Drops the ended cooldowns so senders that never come back do not stay in the map.
        endsAt.values()
              .removeIf(end -> !now.isBefore(end));
        endsAt.put(id.get(), now.plus(duration));
        release(ctx);
    }

    /**
     * Drops the reservation of this execution, which ends without reaching {@link #start}, or has started.
     */
    void release(CommonCommandContext<?, ?> ctx) {
        throttledId(ctx).ifPresent(id -> reservations.computeIfPresent(id,
                                                                       (key, held) -> held.owner == ctx ? null : held));
    }

    private static final class Reservation {
        private final CommonCommandContext<?, ?> owner;
        private final Instant at;

        private Reservation(CommonCommandContext<?, ?> owner, Instant at) {
            this.owner = owner;
            this.at = at;
        }
    }

    private Optional<UUID> throttledId(CommonCommandContext<?, ?> ctx) {
        CommandActor actor = ctx.getActor();
        if (bypassPermission != null && actor.hasPermission(bypassPermission)) {
            return Optional.empty();
        }
        return actor.getUniqueId();
    }

    /**
     * Formats a duration rounded up to whole seconds, such as {@code 1h 2m 3s}, in the sender's locale.
     */
    static String format(Duration duration, Captions captions, Locale locale) {
        long seconds = duration.getSeconds() + (duration.getNano() > 0 ? 1 : 0);
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(captions.format(locale, Caption.DURATION_HOURS, Map.of("value", hours)))
              .append(' ');
        }
        if (minutes > 0) {
            sb.append(captions.format(locale, Caption.DURATION_MINUTES, Map.of("value", minutes)))
              .append(' ');
        }
        if (seconds % 60 > 0 || sb.length() == 0) {
            sb.append(captions.format(locale, Caption.DURATION_SECONDS, Map.of("value", seconds % 60)));
        }
        return sb.toString()
                 .trim();
    }
}
