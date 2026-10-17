package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLibExtensionTest {
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(CommandLibExtensionTest::healCommand,
                                                                                           "proxy.command"));

    private CommandTester beforeEachTester;

    @BeforeEach
    void setUp(CommandTester tester) {
        beforeEachTester = tester;
    }

    @Test
    void injected_players_resolve_as_player_arguments(CommandTester tester,
                                                      @Sender("Steve") FakeSender steve,
                                                      @Sender("Admin") FakeSender admin) {
        tester.execute("heal Steve 5", admin);

        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve by 5");
        assertThat(steve.getSentMessageTexts()).isEmpty();
    }

    @Test
    void lifecycle_methods_get_the_same_tester(CommandTester tester) {
        assertThat(tester).isSameAs(beforeEachTester);
    }

    @Test
    void annotation_configures_permissions(CommandTester tester,
                                           @Sender(value = "Guest", permissions = "proxy.command.heal") FakeSender guest,
                                           @Sender(value = "Denied", denyPermissions = "proxy.command.heal.target.amount") FakeSender denied,
                                           @Sender(value = "Medic", permissions = {"proxy.command.heal", "proxy.command.heal.target.amount"}) FakeSender medic,
                                           @Sender(value = "Both", permissions = {"proxy.command.heal", "proxy.command.heal.target.amount"}, denyPermissions = "proxy.command.heal.target.amount") FakeSender both) {
        assertThatThrownBy(() -> tester.execute("heal Guest 5",
                                                guest)).hasCauseInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> tester.execute("heal Guest 5",
                                                denied)).hasCauseInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> tester.execute("heal Guest 5", both)).hasCauseInstanceOf(CommandSyntaxException.class);

        tester.execute("heal Guest 5", medic);

        assertThat(medic.getSentMessageTexts()).containsExactly("Healed Guest by 5");
    }

    @Test
    void console_is_not_an_online_player(CommandTester tester, @Sender(console = true) FakeSender console) {
        assertThat(console.getName()).isEqualTo("CONSOLE");
        assertThat(tester.proxy()
                         .getPlayer("CONSOLE")).isEmpty();
    }

    @Test
    void default_sender_is_an_online_player(CommandTester tester, FakeSender player) {
        tester.execute("heal Player 1", player);

        assertThat(player.getSentMessageTexts()).containsExactly("Healed Player by 1");
    }

    private static Command healCommand() {
        return new Command("heal") {{
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((target, amount, ctx) -> {
                ctx.sendMessage("Healed " + target.getUsername() + " by " + amount);
            });
        }};
    }
}
