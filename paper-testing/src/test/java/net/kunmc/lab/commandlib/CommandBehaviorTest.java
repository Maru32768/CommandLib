package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBehaviorTest {
    @Test
    void argumentBranchPermissionFiltersSuggestions() throws Exception {
        CommandTester tester = new CommandTester(new Command("config") {{
            argument(new StringArgument("key").addSuggestionAction(sb -> sb.suggest("difficulty"))).permission(
                                                                                                           "custom.config.key")
                                                                                                   .execute((key, ctx) -> ctx.sendMessage(
                                                                                                           key));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");
        Mockito.when(sender.asSender()
                           .hasPermission("custom.config.key"))
               .thenReturn(false);

        assertThat(tester.suggestions("config ", sender)
                         .get()
                         .getList()).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                    .containsExactly("help");
    }

    @Test
    void requirePlayerAllowsPlayersAndRejectsConsole() {
        Command command = new Command("playeronly") {{
            requirePlayer();
            execute(ctx -> ctx.sendSuccess("player"));
        }};
        CommandTester tester = new CommandTester(command, "test.command");
        FakeSender player = FakeSender.player("Steve");
        FakeSender console = FakeSender.console();

        tester.execute("playeronly", player);
        tester.execute("playeronly", console);

        assertThat(player.getSentMessageTexts()).containsExactly("player");
        assertThat(console.getSentMessageTexts()).containsExactly("This command can only be executed by a player.");
    }

    @Test
    void requireConsoleAllowsConsoleAndRejectsPlayers() {
        Command command = new Command("consoleonly") {{
            requireConsole();
            execute(ctx -> ctx.sendSuccess("console"));
        }};
        CommandTester tester = new CommandTester(command, "test.command");
        FakeSender console = FakeSender.console();
        FakeSender player = FakeSender.player("Steve");

        tester.execute("consoleonly", console);
        tester.execute("consoleonly", player);

        assertThat(console.getSentMessageTexts()).containsExactly("console");
        assertThat(player.getSentMessageTexts()).containsExactly("This command can only be executed from the console.");
    }
}
