package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.text.ClickAction;
import dev.marulabs.commandlib.util.text.RichText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandContextTest {
    @Test
    void actor_type_is_detected_for_every_sender_kind() {
        List<String> types = new ArrayList<>();
        Zombie zombie = permitted(Zombie.class);
        Mockito.when(zombie.getUniqueId())
               .thenReturn(UUID.randomUUID());

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
    void messages_use_expected_colors_and_null_becomes_empty() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> {
                ctx.sendWarn("careful");
                ctx.sendSuccess((String) null);
                ctx.sendFailure("broken");
            });
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("careful", "", "broken");
        assertThat(sender.getSentMessages()).extracting(Component::color)
                                            .containsExactly(NamedTextColor.YELLOW,
                                                             NamedTextColor.GREEN,
                                                             NamedTextColor.RED);
    }

    @Test
    void rich_text_is_sent_as_adventure_component() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> {
                ctx.sendMessage(RichText.text("hello")
                                        .color(0x123456)
                                        .hover(RichText.text("details"))
                                        .click(ClickAction.suggestCommand("/say ")));
                ctx.sendMessage(RichText.text("plain")
                                        .hover(RichText.text("")));
                ctx.sendFailure(RichText.text("fail"));
                ctx.sendSuccess(RichText.text("own")
                                        .color(0x5555FF));
            });
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        Component message = sender.getSentMessages()
                                  .get(0);
        assertThat(message.color()).isEqualTo(TextColor.color(0x123456));
        assertThat(message.hoverEvent()).isEqualTo(HoverEvent.showText(Component.text("details")));
        assertThat(message.clickEvent()).isEqualTo(ClickEvent.suggestCommand("/say "));
        assertThat(sender.getSentMessages()
                         .get(1)
                         .hoverEvent()).isNull();
        assertThat(sender.getSentMessages()
                         .subList(2, 4)).extracting(Component::color)
                                        .containsExactly(NamedTextColor.RED, NamedTextColor.BLUE);
    }

    @Test
    void colored_component_sends_keep_an_existing_color() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            execute(ctx -> {
                ctx.sendSuccess(Component.text("plain"));
                ctx.sendWarn(Component.text("colored", NamedTextColor.AQUA));
                ctx.sendFailure(Component.text("failed"));
            });
        }}, "test.command")) {
            tester.execute("say", sender);
        }

        assertThat(sender.getSentMessages()).extracting(Component::color)
                                            .containsExactly(NamedTextColor.GREEN,
                                                             NamedTextColor.AQUA,
                                                             NamedTextColor.RED);
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

    @Test
    void syntax_exception_is_converted_to_failure_message() {
        FakeSender sender = FakeSender.player("Alice");
        CommandSyntaxException e = new SimpleCommandExceptionType(new LiteralMessage("custom failure")).create();
        ArgumentParseException converted = new PlatformAdapterImpl().convertCommandSyntaxException(e);

        try (CommandTester tester = new CommandTester(new Command("fail") {{
            execute(converted::sendMessage);
        }}, "test.command")) {
            tester.execute("fail", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("custom failure");
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
