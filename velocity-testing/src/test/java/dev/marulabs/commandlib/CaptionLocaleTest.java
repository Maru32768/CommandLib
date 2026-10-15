package dev.marulabs.commandlib;

import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaptionLocaleTest {
    private static final Caption GREETING = Caption.of("test.greeting", "Hello, {name}!");

    @Test
    void built_in_captions_follow_the_senders_locale() {
        try (CommandTester tester = new CommandTester(new Command("deny") {{
            execute(ctx -> {
                throw new CommandPrerequisiteException();
            });
        }}, "test.command")) {
            FakeSender japanese = FakeSender.player("Steve")
                                            .locale(Locale.JAPAN);
            FakeSender english = FakeSender.player("Alex")
                                           .locale(Locale.US);

            tester.execute("deny", japanese);
            tester.execute("deny", english);

            assertThat(japanese.getSentMessageTexts()).containsExactly("このコマンドを実行する権限がありません。");
            assertThat(english.getSentMessageTexts()).containsExactly("You do not have permission to execute this command.");
        }
    }

    @Test
    void captions_of_the_command_override_a_locale() {
        try (CommandTester tester = new CommandTester(new Command("greet") {{
            captions(Captions.create()
                             .set(Locale.JAPANESE, GREETING, "こんにちは、{name}さん"));
            execute(ctx -> ctx.sendSuccess(GREETING, Map.of("name", "Steve")));
        }}, "test.command")) {
            FakeSender japanese = FakeSender.player("Steve")
                                            .locale(Locale.JAPAN);
            FakeSender english = FakeSender.player("Alex")
                                           .locale(Locale.US);

            tester.execute("greet", japanese);
            tester.execute("greet", english);

            assertThat(japanese.getSentMessageTexts()).containsExactly("こんにちは、Steveさん");
            assertThat(english.getSentMessageTexts()).containsExactly("Hello, Steve!");
        }
    }
}
