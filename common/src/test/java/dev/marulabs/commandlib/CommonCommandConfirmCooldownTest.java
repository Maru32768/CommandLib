package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonCommandConfirmCooldownTest {
    private final MutableClock clock = new MutableClock();

    @Test
    void confirm_runs_the_second_execution_of_the_same_input() throws Exception {
        TestCommand command = new TestCommand("wipe") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock));
            argument(new CommonStringArgument<>("world")).execute((world,
                                                                   ctx) -> ctx.sendMessage("wiped " + world));
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("wipe a")
                         .messages()).containsExactly("Run the command again within 10s to confirm. [Confirm]");
        // A different input replaces the pending one instead of confirming it.
        assertThat(runner.execute("wipe b")
                         .messages()).containsExactly("Run the command again within 10s to confirm. [Confirm]");
        assertThat(runner.execute("wipe b")
                         .messages()).containsExactly("wiped b");
        // The confirmation is used up.
        assertThat(runner.execute("wipe b")
                         .messages()).containsExactly("Run the command again within 10s to confirm. [Confirm]");
    }

    // As the targets of /execute as @a ... run, which Brigadier runs from one parse of the input.
    @Test
    void the_targets_of_one_forked_execution_do_not_confirm_each_other() throws Exception {
        AtomicInteger wiped = new AtomicInteger();
        TestCommand command = new TestCommand("wipe") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock));
            execute(ctx -> wiped.incrementAndGet());
        }};
        TestCommandSource steve = player("Steve");
        TestCommandRunner runner = new TestCommandRunner(command, steve);
        com.mojang.brigadier.CommandDispatcher<TestCommandSource> dispatcher = new com.mojang.brigadier.CommandDispatcher<>(runner.root());
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<TestCommandSource>literal("all")
                                                                                .fork(dispatcher.getRoot(),
                                                                                      ctx -> List.of(ctx.getSource(),
                                                                                                     ctx.getSource(),
                                                                                                     ctx.getSource(),
                                                                                                     ctx.getSource())));

        dispatcher.execute("all wipe", steve);
        assertThat(wiped).hasValue(0);
        // A later execution of the same input confirms.
        runner.execute("wipe");
        assertThat(wiped).hasValue(0);
        runner.execute("wipe");
        assertThat(wiped).hasValue(1);
    }

    @Test
    void confirm_expires_after_the_timeout() throws Exception {
        TestCommand command = new TestCommand("wipe") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock)
                                .message((ctx, input) -> ctx.sendWarn("confirm " + input)));
            execute(ctx -> ctx.sendMessage("wiped"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("wipe")
                         .messages()).containsExactly("confirm /wipe");
        clock.advance(Duration.ofSeconds(10));
        assertThat(runner.execute("wipe")
                         .messages()).containsExactly("confirm /wipe");
        clock.advance(Duration.ofSeconds(9));
        assertThat(runner.execute("wipe")
                         .messages()).containsExactly("wiped");
    }

    @Test
    void confirm_is_kept_per_sender() throws Exception {
        TestCommand command = new TestCommand("wipe") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock)
                                .message((ctx, input) -> ctx.sendWarn("confirm")));
            execute(ctx -> ctx.sendMessage("wiped"));
        }};
        TestCommandRunner steve = new TestCommandRunner(command, player("Steve"));
        TestCommandRunner alex = new TestCommandRunner(command, player("Alex"));
        TestCommandRunner console = new TestCommandRunner(command, console());

        steve.execute("wipe");
        assertThat(alex.execute("wipe")
                       .messages()).containsExactly("confirm");
        assertThat(console.execute("wipe")
                          .messages()).containsExactly("confirm");
        assertThat(console.execute("wipe")
                          .messages()).containsExactly("wiped");
        assertThat(steve.execute("wipe")
                        .messages()).containsExactly("wiped");
    }

    @Test
    void confirm_ignores_the_leading_slash() throws Exception {
        TestCommand command = new TestCommand("wipe") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock)
                                .message((ctx, input) -> ctx.sendWarn("confirm " + input)));
            execute(ctx -> ctx.sendMessage("wiped"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.executeWithLeadingSlash("/wipe")
                         .messages()).containsExactly("confirm /wipe");
        assertThat(runner.execute("wipe")
                         .messages()).containsExactly("wiped");
    }

    @Test
    void cooldown_rejects_until_it_ends() throws Exception {
        TestCommand command = new TestCommand("heal") {{
            cooldown(Cooldown.of(Duration.ofSeconds(90))
                             .clock(clock));
            execute(ctx -> ctx.sendMessage("healed"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("heal")
                         .messages()).containsExactly("healed");
        clock.advance(Duration.ofMillis(500));
        assertThat(runner.execute("heal")
                         .messages()).containsExactly("You must wait 1m 30s before using this command again.");
        clock.advance(Duration.ofSeconds(89));
        assertThat(runner.execute("heal")
                         .messages()).containsExactly("You must wait 1s before using this command again.");
        clock.advance(Duration.ofMillis(500));
        assertThat(runner.execute("heal")
                         .messages()).containsExactly("healed");
    }

    @Test
    void cooldown_can_be_queried_and_reset() throws Exception {
        UUID steve = UUID.randomUUID();
        UUID alex = UUID.randomUUID();
        Cooldown cooldown = Cooldown.of(Duration.ofSeconds(30))
                                    .clock(clock);
        Duration[] seen = new Duration[1];
        TestCommand command = new TestCommand("heal") {{
            cooldown(cooldown);
            execute(ctx -> seen[0] = cooldown.remaining(ctx));
        }};
        TestCommandRunner steveRunner = new TestCommandRunner(command, source("Steve", steve, CommandActorType.PLAYER));
        TestCommandRunner alexRunner = new TestCommandRunner(command, source("Alex", alex, CommandActorType.PLAYER));

        steveRunner.execute("heal");
        alexRunner.execute("heal");
        // The executor runs after the cooldown starts.
        assertThat(seen[0]).isEqualTo(Duration.ofSeconds(30));
        clock.advance(Duration.ofSeconds(10));
        assertThat(cooldown.remaining(steve)).isEqualTo(Duration.ofSeconds(20));
        assertThat(cooldown.remaining(UUID.randomUUID())).isZero();

        cooldown.reset(steve);
        assertThat(cooldown.remaining(steve)).isZero();
        assertThat(cooldown.remaining(alex)).isEqualTo(Duration.ofSeconds(20));
        assertThat(steveRunner.execute("heal")
                              .messages()).isEmpty();

        cooldown.resetAll();
        assertThat(cooldown.remaining(steve)).isZero();
        assertThat(cooldown.remaining(alex)).isZero();
        assertThat(alexRunner.execute("heal")
                             .messages()).isEmpty();
    }

    @Test
    void cooldown_skips_senders_without_unique_id_and_bypassing_senders() throws Exception {
        TestCommand command = new TestCommand("heal") {{
            cooldown(Cooldown.of(Duration.ofSeconds(30))
                             .clock(clock)
                             .bypassPermission("heal.bypass"));
            execute(ctx -> ctx.sendMessage("healed"));
        }};
        TestCommandRunner console = new TestCommandRunner(command, console());
        TestCommandRunner admin = new TestCommandRunner(command, player("Admin", "heal.bypass"));

        console.execute("heal");
        admin.execute("heal");
        assertThat(console.execute("heal")
                          .messages()).containsExactly("healed");
        assertThat(admin.execute("heal")
                        .messages()).containsExactly("healed");
    }

    @Test
    void cooldown_starts_only_after_confirmation_and_preprocess() throws Exception {
        boolean[] allow = {false};
        TestCommand command = new TestCommand("reset") {{
            cooldown(Cooldown.of(Duration.ofSeconds(30))
                             .clock(clock)
                             .message((ctx, remaining) -> ctx.sendFailure("wait")));
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock)
                                .message((ctx, input) -> ctx.sendWarn("confirm")));
            addPreprocess(ctx -> allow[0]);
            execute(ctx -> ctx.sendMessage("reset"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("reset")
                         .messages()).containsExactly("confirm");
        assertThat(runner.execute("reset")
                         .messages()).isEmpty();
        allow[0] = true;
        assertThat(runner.execute("reset")
                         .messages()).containsExactly("confirm");
        assertThat(runner.execute("reset")
                         .messages()).containsExactly("reset");
        // On cooldown, the sender is not asked to confirm.
        assertThat(runner.execute("reset")
                         .messages()).containsExactly("wait");
    }

    @Test
    void cooldown_rejects_another_execution_while_one_waits_in_a_middleware() throws Exception {
        List<Runnable> waiting = new ArrayList<>();
        AtomicInteger healed = new AtomicInteger();
        boolean[] allow = {true};
        TestCommand command = new TestCommand("heal") {{
            cooldown(Cooldown.of(Duration.ofSeconds(30))
                             .clock(clock)
                             .message((ctx, remaining) -> ctx.sendFailure("wait " + remaining.toSeconds())));
            // A lookup that finishes later, such as one on the async executor.
            addMiddleware((ctx, next) -> {
                CompletableFuture<Void> looked = new CompletableFuture<>();
                waiting.add(() -> looked.complete(null));
                return looked.thenCompose(x -> allow[0] ? next.proceed() : CompletableFuture.completedFuture(null));
            });
            execute(ctx -> healed.incrementAndGet());
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("heal")
                         .messages()).isEmpty();
        assertThat(runner.execute("heal")
                         .messages()).containsExactly("wait 30");
        waiting.forEach(Runnable::run);
        waiting.clear();
        assertThat(healed).hasValue(1);
        assertThat(runner.execute("heal")
                         .messages()).containsExactly("wait 30");

        // A run that the middleware skips does not keep the sender waiting.
        clock.advance(Duration.ofSeconds(30));
        allow[0] = false;
        runner.execute("heal");
        waiting.forEach(Runnable::run);
        waiting.clear();
        allow[0] = true;
        runner.execute("heal");
        waiting.forEach(Runnable::run);
        assertThat(healed).hasValue(2);
    }

    @Test
    void child_commands_share_the_parent_cooldown_unless_they_disable_the_parent_preprocess() throws Exception {
        TestCommand command = new TestCommand("kit") {{
            cooldown(Cooldown.of(Duration.ofSeconds(30))
                             .clock(clock)
                             .message((ctx, remaining) -> ctx.sendFailure("wait")));
            addChildren(new TestCommand("food") {{
                execute(ctx -> ctx.sendMessage("food"));
            }}, new TestCommand("tools") {{
                execute(ctx -> ctx.sendMessage("tools"));
            }}, new TestCommand("free") {{
                disableParentMiddleware();
                execute(ctx -> ctx.sendMessage("free"));
            }});
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));

        assertThat(runner.execute("kit food")
                         .messages()).containsExactly("food");
        assertThat(runner.execute("kit tools")
                         .messages()).containsExactly("wait");
        assertThat(runner.execute("kit free")
                         .messages()).containsExactly("free");
    }

    @Test
    void rejects_non_positive_durations() {
        assertThatThrownBy(() -> Cooldown.of(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Confirmation.within(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formats_durations_rounded_up_to_seconds() {
        assertThat(Cooldown.format(Duration.ofMillis(1), Captions.DEFAULT, Locale.ENGLISH)).isEqualTo("1s");
        assertThat(Cooldown.format(Duration.ofMinutes(1), Captions.DEFAULT, Locale.ENGLISH)).isEqualTo("1m");
        assertThat(Cooldown.format(Duration.ofSeconds(3723), Captions.DEFAULT, Locale.ENGLISH)).isEqualTo("1h 2m 3s");
        assertThat(Cooldown.format(Duration.ofHours(1), Captions.DEFAULT, Locale.ENGLISH)).isEqualTo("1h");
        assertThat(Cooldown.format(Duration.ofSeconds(3723), Captions.DEFAULT, Locale.JAPAN)).isEqualTo("1時間 2分 3秒");
    }

    private static TestCommandSource console() {
        return source("CONSOLE", null, CommandActorType.CONSOLE);
    }

    private static TestCommandSource player(String name, String... permissions) {
        return source(name, UUID.randomUUID(), CommandActorType.PLAYER, permissions);
    }

    private static TestCommandSource source(String name, UUID uniqueId, CommandActorType type, String... permissions) {
        return new TestCommandSource(name, uniqueId, type, false, Set.of(permissions)) {
            @Override
            public boolean hasPermission(String permission) {
                // Every command permission is granted; only the listed extra permissions count otherwise.
                return permission.startsWith("test.command") || super.hasPermission(permission);
            }
        };
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
