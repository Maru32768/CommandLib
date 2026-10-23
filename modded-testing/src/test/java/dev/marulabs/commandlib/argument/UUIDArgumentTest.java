package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDArgumentTest {
    private static CommandTester tester() {
        return new CommandTester(new Command("uuid") {{
            addChildren(new Command("one") {{
                argument(new UUIDArgument("id")).execute((ctx, id) -> ctx.sendMessage(id.toString()));
            }}, new Command("many") {{
                argument(new UUIDsArgument("ids")).execute((ctx, ids) -> ctx.sendMessage(ids.toString()));
            }});
        }}, "test");
    }

    @Test
    void a_uuid_parses_and_an_unknown_name_is_rejected() {
        UUID uuid = UUID.randomUUID();
        FakeSender console = FakeSender.console();
        try (CommandTester tester = tester()) {
            tester.execute("uuid one " + uuid, console);
            tester.execute("uuid one Nobody", console);
            tester.execute("uuid many Nobody", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly(uuid.toString(),
                                                                  "Nobody is neither a known player nor a valid UUID",
                                                                  "Nobody is neither a known player nor a valid UUID");
    }

    @Test
    void selectors_other_than_all_and_random_are_rejected() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = tester()) {
            tester.execute("uuid many @p", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("@p is not a valid selector");
    }

    @Test
    void all_and_random_need_an_online_player() {
        FakeSender console = FakeSender.console();
        FakeSender steve = FakeSender.player("Steve")
                                     .op(true);
        try (CommandTester tester = tester()) {
            tester.execute("uuid many @a", console);
            tester.execute("uuid many @r", console);
            tester.execute("uuid many @r", steve);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("No player was found", "No player was found");
        assertThat(steve.getSentMessageTexts()).containsExactly("[" + steve.getUniqueId()
                                                                           .orElseThrow()
                + "]");
    }
}
