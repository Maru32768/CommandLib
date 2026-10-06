package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.StringArgument;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandContextTest {
    @Test
    void actor_type_is_detected_for_every_sender_kind() {
        List<String> types = new ArrayList<>();
        UUID zombieId = UUID.randomUUID();
        Zombie zombie = permitted(Zombie.class);
        Mockito.when(zombie.getUniqueId())
               .thenReturn(zombieId);

        try (CommandTester tester = new CommandTester(new Command("who") {{
            execute(ctx -> types.add(ctx.getActor()
                                        .getType() + ":" + ctx.getActor()
                                                              .isConsole() + ":" + ctx.getActor()
                                                                                      .getUniqueId()
                                                                                      .isPresent()));
        }}, "test.command")) {
            tester.execute("who", permitted(RemoteConsoleCommandSender.class));
            tester.execute("who", permitted(BlockCommandSender.class));
            tester.execute("who", zombie);
            tester.execute("who", permitted(CommandSender.class));
            tester.execute("who", FakeSender.console());
            tester.execute("who", FakeSender.player("Steve"));
        }

        assertThat(types).containsExactly("REMOTE_CONSOLE:true:false",
                                          "COMMAND_BLOCK:false:false",
                                          "ENTITY:false:true",
                                          "UNKNOWN:false:false",
                                          "CONSOLE:true:false",
                                          "PLAYER:false:true");
    }

    @Test
    void actor_unwraps_to_bukkit_sender_type() {
        List<Object> unwrapped = new ArrayList<>();
        FakeSender steve = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(new Command("who") {{
            execute(ctx -> {
                unwrapped.add(ctx.getActor()
                                 .unwrap(Player.class)
                                 .orElse(null));
                unwrapped.add(ctx.getActor()
                                 .unwrap(Zombie.class)
                                 .isPresent());
            });
        }}, "test.command")) {
            tester.execute("who", steve);
        }

        assertThat(unwrapped).containsExactly(steve.asSender(), false);
    }

    @Test
    void warn_message_is_yellow_and_success_handles_null() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> {
                ctx.sendWarn("careful");
                ctx.sendSuccess((String) null);
                ctx.sendFailure((String) null);
            });
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("careful", "null", "null");
        assertThat(sender.getSentMessages()).extracting(BaseComponent::getColor)
                                            .containsExactly(ChatColor.YELLOW, ChatColor.GREEN, ChatColor.RED);
    }

    @Test
    void message_with_option_applies_rgb_and_hover_text() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> ctx.sendMessageWithOption("hello",
                                                     option -> option.rgb(0x123456)
                                                                     .hoverText("details")));
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        BaseComponent message = sender.getSentMessages()
                                      .get(0);
        assertThat(message.toPlainText()).isEqualTo("hello");
        assertThat(message.getColor()).isEqualTo(ChatColor.of(new Color(0x123456)));
        assertThat(message.getHoverEvent()
                          .getAction()).isEqualTo(HoverEvent.Action.SHOW_TEXT);
        assertThat(((Text) message.getHoverEvent()
                                  .getContents()
                                  .get(0)).getValue()).isEqualTo("details");
    }

    @Test
    void component_is_sent_unchanged() {
        FakeSender sender = FakeSender.player("Alice");
        TextComponent component = new TextComponent("raw");
        component.setBold(true);

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> ctx.sendComponent(component));
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        assertThat(sender.getSentMessages()).containsExactly(component);
    }

    @Test
    void player_and_console_accessors_reject_other_senders() {
        List<String> errors = new ArrayList<>();

        try (CommandTester tester = new CommandTester(new Command("who") {{
            execute(ctx -> {
                try {
                    ctx.getPlayer();
                } catch (IllegalStateException e) {
                    errors.add(e.getMessage());
                }
                try {
                    ctx.getConsole();
                } catch (IllegalStateException e) {
                    errors.add(e.getMessage());
                }
            });
        }}, "test.command")) {
            tester.execute("who", FakeSender.console());
            tester.execute("who", FakeSender.player("Steve"));
        }

        assertThat(errors).containsExactly("sender is not a player", "sender is not a console");
    }

    @Test
    void denied_command_permission_rejects_execution() {
        FakeSender sender = FakeSender.player("Alice")
                                      .denyPermissions("test.command.secure");

        try (CommandTester tester = new CommandTester(new Command("secure") {{
            execute(ctx -> ctx.sendMessage("ok"));
        }}, "test.command")) {
            assertThatThrownBy(() -> tester.execute("secure", sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void denied_child_and_argument_permissions_reject_only_that_branch() {
        FakeSender sender = FakeSender.player("Alice")
                                      .denyPermissions("test.command.game.stop", "test.command.game.say.text");

        try (CommandTester tester = new CommandTester(new Command("game") {{
            addChildren(new Command("start") {{
                execute(ctx -> ctx.sendMessage("start"));
            }}, new Command("stop") {{
                execute(ctx -> ctx.sendMessage("stop"));
            }}, new Command("say") {{
                argument(new StringArgument("text")).execute((text, ctx) -> ctx.sendMessage(text));
            }});
        }}, "test.command")) {
            tester.execute("game start", sender);
            assertThatThrownBy(() -> tester.execute("game stop", sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("game say hi", sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("start");
    }

    private static <T extends CommandSender> T permitted(Class<T> type) {
        T sender = Mockito.mock(type);
        Mockito.when(sender.hasPermission(Mockito.anyString()))
               .thenReturn(true);
        Mockito.when(sender.getName())
               .thenReturn(type.getSimpleName());
        return sender;
    }
}
