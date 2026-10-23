package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionalVariadicArgumentTest {
    @Test
    void variadic_player_argument_gives_every_player() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender alex = FakeSender.player("Alex");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("targets").variadic()).execute((ctx, targets) -> {
                List<Player> players = targets;
                ctx.sendMessage("Healed " + players.stream()
                                                   .map(Player::getName)
                                                   .collect(Collectors.joining(",")));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.withFakePlayer((Player) alex.asSender());
            tester.execute("heal Steve Alex", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve,Alex");
    }

    @Test
    void optional_player_argument_defaults_to_the_sender() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");
        FakeSender console = FakeSender.console();

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            argument(new PlayerArgument("target").optional(player())).execute((ctx, target) -> {
                ctx.sendMessage("Healed " + target.getName());
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("heal", admin);
            tester.execute("heal Steve", admin);
            tester.execute("heal", console);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Admin", "Healed Steve");
        assertThat(console.getSentMessageTexts()).containsExactly("This command can only be executed by a player.");
    }

    @Test
    void optional_argument_gives_default_and_shows_in_help() {
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(() -> new Command("give") {{
            argument(new StringArgument("item"),
                     new IntegerArgument("count").optional(1)).execute((ctx, item, count) -> {
                         ctx.sendMessage(item + "x" + count);
                     });
        }}, "test.command")) {
            tester.execute("give apple", sender);
            tester.execute("give apple 3", sender);
            tester.execute("give help", sender);
        }

        assertThat(sender.getSentMessageTexts()).startsWith("applex1", "applex3")
                                                .anyMatch(x -> x.contains("/give <item> [count]"));
    }

    @Test
    void denied_branch_permission_stops_variadic_branch() {
        FakeSender sender = FakeSender.player("Admin")
                                      .denyPermissions("test.command.say.words");

        try (CommandTester tester = new CommandTester(() -> new Command("say") {{
            argument(new StringArgument("words").variadic()).execute((ctx, words) -> ctx.sendMessage(words));
        }}, "test.command")) {
            assertThatThrownBy(() -> tester.execute("say hi there",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThat(sender.getSentMessageTexts()).isEmpty();

            FakeSender allowed = FakeSender.player("Bob");
            tester.execute("say hi there", allowed);
            assertThat(allowed.getSentMessageTexts()).containsExactly("[hi, there]");
        }
    }
}
