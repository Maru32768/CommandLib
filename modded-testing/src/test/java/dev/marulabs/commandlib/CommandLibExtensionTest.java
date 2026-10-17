package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.util.Optional;

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
                                           @Sender(value = "Guest", permissions = "mymod.command.heal") FakeSender guest,
                                           @Sender(value = "Healer", permissions = {"mymod.command.heal", "mymod.command.heal.target.amount"}) FakeSender healer) {
        // Without the node of the argument branch, the arguments are not part of the guest's command tree.
        assertThatThrownBy(() -> tester.execute("heal Guest 5", guest)).hasMessageStartingWith("Command syntax error")
                                                                       .hasCauseInstanceOf(CommandSyntaxException.class);

        tester.execute("heal Guest 5", healer);
        assertThat(healer.getSentMessageTexts()).containsExactly("Healed Guest by 5");
    }

    @Test
    void two_injected_players_with_the_same_name_are_refused(CommandTester tester,
                                                             FakeSender player) throws NoSuchMethodException {
        ParameterContext parameter = Mockito.mock(ParameterContext.class);
        Mockito.when(parameter.findAnnotation(Sender.class))
               .thenReturn(Optional.empty());
        Mockito.when(parameter.getParameter())
               .thenReturn(getClass().getDeclaredMethod("two_injected_players_with_the_same_name_are_refused",
                                                        CommandTester.class,
                                                        FakeSender.class)
                                     .getParameters()[1]);
        ExtensionContext.Store store = Mockito.mock(ExtensionContext.Store.class);
        Mockito.when(store.getOrComputeIfAbsent(Mockito.any(), Mockito.any(), Mockito.eq(CommandTester.class)))
               .thenReturn(tester);
        ExtensionContext context = Mockito.mock(ExtensionContext.class);
        Mockito.when(context.getTestMethod())
               .thenReturn(Optional.of(getClass().getDeclaredMethod("two_injected_players_with_the_same_name_are_refused",
                                                                    CommandTester.class,
                                                                    FakeSender.class)));
        Mockito.when(context.getStore(Mockito.any()))
               .thenReturn(store);

        // A second un-annotated sender is also named Player.
        assertThatThrownBy(() -> commandLib.resolveParameter(parameter,
                                                             context)).isInstanceOf(ParameterResolutionException.class)
                                                                      .hasMessageContaining(player.getName());
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
