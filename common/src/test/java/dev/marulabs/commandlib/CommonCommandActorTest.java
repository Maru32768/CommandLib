package dev.marulabs.commandlib;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommonCommandActorTest {
    @Test
    void command_context_returns_actor_from_source() throws Exception {
        UUID uuid = UUID.randomUUID();
        TestCommandSource source = new TestCommandSource("Alex",
                                                         uuid,
                                                         CommandActorType.PLAYER,
                                                         false,
                                                         Set.of("test.command.actor", "test.actor.reveal"));
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("actor") {{
            execute(ctx -> {
                CommandActor actor = ctx.getActor();
                ctx.sendMessage(actor.getName() + ":" + actor.isPlayer() + ":"
                        + actor.hasPermission("test.actor.reveal") + ":" + actor.getUniqueId()
                                                                                .orElseThrow());
            });
        }}, source);

        TestCommandContext ctx = runner.execute("actor");

        assertThat(ctx.messages()).containsExactly("Alex:true:true:" + uuid);
    }

    @Test
    void console_actor_can_be_detected() throws Exception {
        TestCommandSource source = new TestCommandSource("Server",
                                                         null,
                                                         CommandActorType.CONSOLE,
                                                         true,
                                                         Set.of("test.command.actor"));
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("actor") {{
            execute(ctx -> ctx.sendMessage(String.valueOf(ctx.getActor()
                                                             .isConsole())));
        }}, source);

        TestCommandContext ctx = runner.execute("actor");

        assertThat(ctx.messages()).containsExactly("true");
    }
}
