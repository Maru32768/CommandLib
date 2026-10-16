package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.annotation.Name;
import dev.marulabs.commandlib.annotation.Subcommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedCommandsTest {
    @dev.marulabs.commandlib.annotation.Command("hub")
    public static final class AnnotatedHubCommand {
        @Execute
        public void run(CommandContext ctx) {
            ctx.sendSuccess("hub");
        }

        @Subcommand("send")
        public void send(CommandContext ctx, @Name("server") RegisteredServer server, @Name("times") int times) {
            ctx.sendSuccess("send " + server.getServerInfo()
                                            .getName()
                    + " x" + times);
            ctx.setResult(times);
        }
    }

    @Test
    void annotated_command_runs_its_executor_and_parses_its_arguments() {
        CommandTester tester = new CommandTester(AnnotatedCommands.parse(new AnnotatedHubCommand()), "proxy.command");
        tester.withFakeServer("lobby");
        FakeSender sender = FakeSender.player("Alice");

        tester.execute("hub", sender);
        int result = tester.execute("hub send lobby 3", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("hub", "send lobby x3");
        assertThat(result).isEqualTo(3);
        assertThat(tester.suggest("hub send ", sender)).containsExactly("help", "lobby");
    }
}
