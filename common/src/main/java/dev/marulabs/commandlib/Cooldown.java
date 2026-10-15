package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A per-sender cooldown for {@link CommonCommand#cooldown(Cooldown)}.
 * <p>
 * The cooldown starts when the command passes its preprocess and is about to execute, and keeps the same sender from
 * executing it again until it ends. Senders are told apart by their unique id, so only senders that have one, such as
 * players and entities, are throttled; the console, remote consoles and command blocks are never on cooldown. Commands
 * given the same instance share one cooldown.
 */
public final class Cooldown {
    private final Duration duration;
    private final Map<UUID, Instant> endsAt = new ConcurrentHashMap<>();
    private @Nullable String bypassPermission = null;
    private BiConsumer<CommonCommandContext<?, ?>, Duration> message = (ctx,
                                                                        remaining) -> ctx.sendFailure("You must wait "
                                                                                + format(remaining)
                                                                                + " before using this command again.");
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
     * Returns false, after sending the message, when the sender is on cooldown.
     */
    boolean check(CommonCommandContext<?, ?> ctx) {
        Optional<UUID> id = throttledId(ctx);
        if (id.isEmpty()) {
            return true;
        }
        Instant end = endsAt.get(id.get());
        Instant now = clock.instant();
        if (end == null || !now.isBefore(end)) {
            return true;
        }
        message.accept(ctx, Duration.between(now, end));
        return false;
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
    }

    private Optional<UUID> throttledId(CommonCommandContext<?, ?> ctx) {
        CommandActor actor = ctx.getActor();
        if (bypassPermission != null && actor.hasPermission(bypassPermission)) {
            return Optional.empty();
        }
        return actor.getUniqueId();
    }

    /**
     * Formats a duration rounded up to whole seconds, such as {@code 1h 2m 3s}.
     */
    static String format(Duration duration) {
        long seconds = duration.getSeconds() + (duration.getNano() > 0 ? 1 : 0);
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours)
              .append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes)
              .append("m ");
        }
        if (seconds % 60 > 0 || sb.length() == 0) {
            sb.append(seconds % 60)
              .append('s');
        }
        return sb.toString()
                 .trim();
    }
}
