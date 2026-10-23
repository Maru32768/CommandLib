package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBehaviorTest {
    @Test
    void argumentBranchPermissionFiltersSuggestions() {
        CommandTester tester = new CommandTester(new Command("config") {{
            argument(new StringArgument("key").addSuggestionProvider(sb -> sb.suggest("difficulty"))).permission("custom.config.key")
                                                                                                     .execute((ctx,
                                                                                                               key) -> ctx.sendMessage(key));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");
        Mockito.when(sender.asSender()
                           .hasPermission("custom.config.key"))
               .thenReturn(false);

        assertThat(tester.suggest("config ", sender)).containsExactly("help");
    }

    @Test
    void playerPrerequisiteAllowsPlayersAndRejectsConsole() {
        Command command = new Command("playeronly") {{
            addPrerequisite(Command.player());
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
    void playerExtractorPassesThePlayerToTheExecutor() {
        Command command = new Command("whoami") {{
            require(Command.player()).execute((ctx, player) -> ctx.sendSuccess(player.getName()));
        }};
        CommandTester tester = new CommandTester(command, "test.command");
        FakeSender player = FakeSender.player("Steve");
        FakeSender console = FakeSender.console();

        tester.execute("whoami", player);
        tester.execute("whoami", console);

        assertThat(player.getSentMessageTexts()).containsExactly("Steve");
        assertThat(console.getSentMessageTexts()).containsExactly("This command can only be executed by a player.");
    }

    @Test
    void consolePrerequisiteAllowsConsoleAndRejectsPlayers() {
        Command command = new Command("consoleonly") {{
            addPrerequisite(Command.console());
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

    @Test
    void confirmAndCooldownApplyToTheSender() {
        Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        Command command = new Command("reset") {{
            confirm(Confirmation.within(Duration.ofSeconds(10))
                                .clock(clock));
            cooldown(Cooldown.of(Duration.ofSeconds(30))
                             .clock(clock));
            execute(ctx -> ctx.sendMessage("reset"));
        }};
        CommandTester tester = new CommandTester(command, "test.command");
        FakeSender player = FakeSender.player("Steve");

        tester.execute("reset", player);
        tester.execute("reset", player);
        tester.execute("reset", player);

        assertThat(player.getSentMessageTexts()).containsExactly("Run the command again within 10s to confirm. [Confirm]",
                                                                 "reset",
                                                                 "You must wait 30s before using this command again.");
    }
}
