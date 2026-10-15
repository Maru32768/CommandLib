package dev.marulabs.commandlib;

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
                                                                                           "mymod.command"));

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
        assertThat(tester).isSameAs(beforeEachTester);
    }

    @Test
    void annotation_configures_permissions(CommandTester tester,
                                           @Sender(value = "Guest", permissions = "mymod.command.heal") FakeSender guest) {
        assertThatThrownBy(() -> tester.execute("heal Guest 5", guest)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void annotation_configures_console_and_operators(@Sender(console = true) FakeSender console,
                                                     @Sender(value = "Op", op = true) FakeSender op,
                                                     FakeSender player) {
        assertThat(console.isPlayer()).isFalse();
        assertThat(console.isOp()).isTrue();
        assertThat(op.isOp()).isTrue();
        assertThat(player.getName()).isEqualTo("Player");
        assertThat(player.isOp()).isFalse();
    }

    @Test
    void suggest_returns_suggestion_texts(CommandTester tester, @Sender("Steve") FakeSender steve) {
        assertThat(tester.suggest("heal -", steve)).containsExactly("--silent", "-s");
    }

    private static Command healCommand() {
        return new Command("heal") {{
            addAliases("h");
            option(Options.flag("silent", 's'));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((target, amount, ctx) -> {
                ctx.sendMessage("Healed " + target.getName()
                                                  .getString()
                        + " by " + amount);
            });
        }};
    }
}
