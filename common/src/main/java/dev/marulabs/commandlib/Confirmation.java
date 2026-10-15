package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.ClickAction;
import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A confirmation step for {@link CommonCommand#confirm(Confirmation)}.
 * <p>
 * The first execution does not run the command; it sends a message asking the sender to run the same command again.
 * Running exactly the same input again within the timeout runs it. Each sender has at most one pending confirmation:
 * a different input, or the same input after the timeout, asks again. Players and entities are told apart by their
 * unique id, other senders, such as the console and command blocks, by their type and name.
 */
public final class Confirmation {
    private final Duration timeout;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private BiConsumer<CommonCommandContext<?, ?>, String> message = this::sendDefaultMessage;
    private Clock clock = Clock.systemUTC();

    private Confirmation(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Creates a confirmation that waits {@code timeout} for the second execution.
     *
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    @NotNull
    public static Confirmation within(@NotNull Duration timeout) {
        Objects.requireNonNull(timeout);
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive: " + timeout);
        }
        return new Confirmation(timeout);
    }

    /**
     * Replaces the message that asks for the confirmation. The consumer receives the command to run again, with a
     * leading slash, such as {@code /ban Steve}.
     */
    @NotNull
    public Confirmation message(@NotNull BiConsumer<CommonCommandContext<?, ?>, String> message) {
        this.message = Objects.requireNonNull(message);
        return this;
    }

    /**
     * Replaces the clock the timeout is measured with, such as a fixed clock in tests.
     */
    @NotNull
    public Confirmation clock(@NotNull Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        return this;
    }

    /**
     * Returns true when this execution confirms a pending one, or else asks for the confirmation and returns false.
     */
    boolean confirm(CommonCommandContext<?, ?> ctx) {
        CommandActor actor = ctx.getActor();
        String sender = actor.getUniqueId()
                             .map(Object::toString)
                             .orElseGet(() -> actor.getType() + ":" + actor.getName());
        String input = ctx.getHandle()
                          .getInput();
        // A player's chat command keeps its leading slash in the input; a console command has none.
        String command = "/" + (input.startsWith("/") ? input.substring(1) : input);
        Instant now = clock.instant();
        // Drops the expired requests so senders that never confirm do not stay in the map.
        pending.values()
               .removeIf(x -> !now.isBefore(x.expiresAt));

        Pending previous = pending.remove(sender);
        if (previous != null && previous.command.equals(command)) {
            return true;
        }
        pending.put(sender, new Pending(command, now.plus(timeout)));
        message.accept(ctx, command);
        return false;
    }

    private void sendDefaultMessage(CommonCommandContext<?, ?> ctx, String command) {
        RichText button = RichText.text("[Confirm]")
                                  .color(Objects.requireNonNull(ChatColorUtil.GREEN.getRGB()))
                                  .hover(RichText.text(command))
                                  .click(ClickAction.runCommand(command));
        ctx.sendWarn(RichText.text("Run the command again within " + Cooldown.format(timeout) + " to confirm. ")
                             .append(button));
    }

    private static final class Pending {
        private final String command;
        private final Instant expiresAt;

        private Pending(String command, Instant expiresAt) {
            this.command = command;
            this.expiresAt = expiresAt;
        }
    }
}
