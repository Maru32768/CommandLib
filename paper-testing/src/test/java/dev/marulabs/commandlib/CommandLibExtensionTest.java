package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLibExtensionTest {
    // The supplier builds the command while the ArgumentTypes fakes are active, which PlayerArgument needs.
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(CommandLibExtensionTest::healCommand,
                                                                                           "myplugin.command"));

    @Test
    void injected_players_resolve_as_player_arguments(CommandTester tester,
                                                      @Sender("Steve") FakeSender steve,
                                                      @Sender("Admin") FakeSender admin) {
        tester.execute("heal Steve 5", admin);

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve by 5");
        assertThat(steve.getSentMessageTexts()).isEmpty();
    }

    @Test
    void annotation_configures_permissions(CommandTester tester,
                                           @Sender(value = "Guest", permissions = "myplugin.command.heal") FakeSender guest,
                                           @Sender("Medic") FakeSender medic) {
        // Without the branch node Brigadier hides the branch; a sender with every node runs the same input.
        assertThatThrownBy(() -> tester.execute("heal Guest 5",
                                                guest)).hasCauseInstanceOf(CommandSyntaxException.class);
        tester.execute("heal Guest 5", medic);

        assertThat(medic.getSentMessageTexts()).containsExactly("Healed Guest by 5");
    }

    @Test
    void suggest_returns_suggestion_texts(CommandTester tester, @Sender("Steve") FakeSender steve) {
        assertThat(tester.suggest("heal -", steve)).containsExactly("--silent", "-s");
    }

    @Test
    void command_tree_matches_snapshot(CommandTester tester) {
        tester.assertCommandTreeMatchesSnapshot("command-tree-heal.snap");
    }

    private static Command healCommand() {
        return new Command("heal") {{
            addAliases("h");
            option(Options.flag("silent", 's'));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((ctx, target, amount) -> {
                ctx.sendMessage("Healed " + target.getName() + " by " + amount);
            });
        }};
    }
}
