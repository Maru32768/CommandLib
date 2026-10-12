package dev.marulabs.commandlib;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommandActorTest {
    @Test
    void player_actor_exposes_sender_capabilities() {
        UUID uuid = UUID.randomUUID();
        try (CommandTester tester = new CommandTester(new Command("actor") {{
            execute(ctx -> {
                CommandActor actor = ctx.getActor();
                ctx.sendMessage(actor.getName() + ":" + actor.isPlayer() + ":" + actor.isConsole() + ":" + actor.isOperator() + ":" + actor.hasPermission("test.actor.reveal") + ":" + actor.getUniqueId()
                                                                                                                                                                                            .orElseThrow() + ":" + actor.unwrap(Player.class)
                                                                                                                                                                                                                        .isPresent());
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alex")
                                          .uniqueId(uuid)
                                          .op(false)
                                          .permissions("test.command.actor", "test.actor.reveal");

            tester.execute("actor", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:true:false:false:true:" + uuid + ":true");
        }
    }

    @Test
    void console_actor_is_detected() {
        try (CommandTester tester = new CommandTester(new Command("actor") {{
            execute(ctx -> {
                CommandActor actor = ctx.getActor();
                ctx.sendMessage(actor.getName() + ":" + actor.getType() + ":" + actor.isConsole() + ":" + actor.isPlayer() + ":" + actor.isOperator());
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.console()
                                          .name("Server")
                                          .op(true)
                                          .permissions("test.command.actor");

            tester.execute("actor", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Server:CONSOLE:true:false:true");
        }
    }

    @Test
    void player_without_permission_is_not_granted() {
        try (CommandTester tester = new CommandTester(new Command("actor") {{
            execute(ctx -> ctx.sendMessage(String.valueOf(ctx.getActor()
                                                             .hasPermission("test.actor.reveal"))));
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alex")
                                          .permissions("test.command.actor");

            tester.execute("actor", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("false");
        }
    }
}
