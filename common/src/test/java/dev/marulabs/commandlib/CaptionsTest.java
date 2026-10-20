package dev.marulabs.commandlib;

import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.util.text.RichText;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CaptionsTest {
    @Test
    void built_in_captions_are_english_and_japanese() {
        Captions captions = Captions.create();

        assertThat(captions.text(Locale.ENGLISH,
                                 Caption.PERMISSION_DENIED)).isEqualTo("You do not have permission to execute this command.");
        assertThat(captions.text(Locale.JAPAN, Caption.PERMISSION_DENIED)).isEqualTo("このコマンドを実行する権限がありません。");
        assertThat(captions.text(Locale.JAPANESE, Caption.PERMISSION_DENIED)).isEqualTo("このコマンドを実行する権限がありません。");
        // A language without a translation gets the English default.
        assertThat(captions.text(Locale.GERMAN,
                                 Caption.PERMISSION_DENIED)).isEqualTo("You do not have permission to execute this command.");
        assertThat(captions.text(Locale.ROOT,
                                 Caption.PERMISSION_DENIED)).isEqualTo("You do not have permission to execute this command.");
    }

    @Test
    void every_built_in_caption_is_in_the_english_and_japanese_bundles() {
        Set<String> keys = Caption.builtIns()
                                  .stream()
                                  .map(Caption::key)
                                  .collect(Collectors.toSet());
        Set<String> japanese = new HashSet<>(Caption.load(Caption.class::getResourceAsStream,
                                                          Captions.BUILT_IN_BUNDLE + "_ja.properties")
                                                    .stringPropertyNames());
        // Sent as vanilla's command.failed, which the client translates.
        japanese.add(Caption.UNEXPECTED_ERROR.key());

        assertThat(Caption.load(Caption.class::getResourceAsStream, Captions.BUILT_IN_BUNDLE + "_en.properties")
                          .stringPropertyNames()).isEqualTo(keys);
        assertThat(japanese).isEqualTo(keys);
    }

    @Test
    void a_text_set_for_a_locale_comes_before_the_bundles_and_the_built_in_one() {
        Captions captions = Captions.create()
                                    .bundle("dev/marulabs/commandlib/captions/messages", getClass().getClassLoader())
                                    .set(Locale.JAPAN, Caption.PERMISSION_DENIED, "権限なし (JP)");

        assertThat(captions.text(Locale.JAPAN, Caption.PERMISSION_DENIED)).isEqualTo("権限なし (JP)");
        // The bundle's ja file beats the built-in one.
        assertThat(captions.text(Locale.JAPANESE, Caption.PERMISSION_DENIED)).isEqualTo("権限がありません (bundle)");
        // The bundle's root file applies where no file of the language has the text.
        assertThat(captions.text(Locale.ENGLISH, Caption.PERMISSION_DENIED)).isEqualTo("No permission (bundle)");
        // A language's text, even CommandLib's own, beats a root text.
        assertThat(captions.text(Locale.JAPANESE, Caption.TIMEOUT)).isEqualTo("コマンドがタイムアウトしました。");
    }

    @Test
    void the_last_added_bundle_wins() throws Exception {
        Captions captions = Captions.create()
                                    .load(Locale.ROOT,
                                          new PropertyResourceBundle(new StringReader("commandlib.execution.timeout=first")))
                                    .load(Locale.ROOT,
                                          new PropertyResourceBundle(new StringReader("commandlib.execution.timeout=second")));

        assertThat(captions.text(Locale.ENGLISH, Caption.TIMEOUT)).isEqualTo("second");
    }

    @Test
    void placeholders_are_replaced_and_unknown_braces_are_kept() {
        Captions captions = Captions.create();
        Caption caption = Caption.of("test.greeting", "{a}, {b} and {missing} {a}{ } {");

        assertThat(captions.format(Locale.ENGLISH,
                                   caption,
                                   Map.of("a", "x", "b", 1))).isEqualTo("x, 1 and {missing} x{ } {");
        assertThat(captions.format(Locale.ENGLISH, caption, Map.of())).isEqualTo("{a}, {b} and {missing} {a}{ } {");
        assertThat(captions.format(Locale.JAPANESE,
                                   Caption.OPTION_MISSING,
                                   Map.of("option", "--tag"))).isEqualTo("必須オプション --tag がありません。");
    }

    @Test
    void the_command_sends_captions_in_the_default_locale_of_a_sender_without_one() throws Exception {
        TestCommand command = new TestCommand("deny") {{
            execute(ctx -> {
                throw new CommandPrerequisiteException();
            });
        }};

        assertThat(inLocale(Locale.JAPAN, () -> run(command))).containsExactly("このコマンドを実行する権限がありません。");
        assertThat(inLocale(Locale.US,
                            () -> run(command))).containsExactly("You do not have permission to execute this command.");
    }

    @Test
    void child_commands_use_the_captions_of_their_parent() throws Exception {
        TestCommand command = new TestCommand("parent") {{
            captions(Captions.create()
                             .set(Caption.PERMISSION_DENIED, "Nope."));
            addChildren(new TestCommand("child") {{
                execute(ctx -> {
                    throw new CommandPrerequisiteException();
                });
            }});
        }};

        assertThat(new TestCommandRunner(command).execute("parent child")
                                                 .messages()).containsExactly("Nope.");
    }

    @Test
    void the_sender_formats_each_caption() throws Exception {
        TestCommand command = new TestCommand("deny") {{
            captions(Captions.create()
                             .sender((ctx,
                                      caption,
                                      message,
                                      send) -> send.accept(RichText.text("[" + caption.key() + "] ")
                                                                   .append(message))));
            execute(ctx -> ctx.sendFailure(Caption.of("test.custom", "Hello {name}"), Map.of("name", "Steve")));
        }};

        assertThat(new TestCommandRunner(command).execute("deny")
                                                 .messages()).containsExactly("[test.custom] Hello Steve");
    }

    @Test
    void the_unexpected_error_is_vanilla_command_failed_unless_set() throws Exception {
        TestCommand command = new TestCommand("boom") {{
            execute(ctx -> {
                throw new IllegalStateException("boom");
            });
        }};
        TestCommand custom = new TestCommand("boom") {{
            captions(Captions.create()
                             .set(Caption.UNEXPECTED_ERROR, "Oops."));
            execute(ctx -> {
                throw new IllegalStateException("boom");
            });
        }};

        assertThat(new TestCommandRunner(command).execute("boom")
                                                 .messages()).containsExactly("command.failed",
                                                                              "Check the console for details.");
        assertThat(new TestCommandRunner(custom).execute("boom")
                                                .messages()).containsExactly("Oops.", "Check the console for details.");
    }

    @Test
    void properties_files_are_read_as_utf_8() {
        Properties properties = Caption.load(Caption.class::getResourceAsStream,
                                             Captions.BUILT_IN_BUNDLE + "_ja.properties");

        assertThat(properties.getProperty("commandlib.help.usage")).isEqualTo("使い方:");
    }

    private static java.util.List<String> run(TestCommand command) {
        try {
            return new TestCommandRunner(command).execute("deny")
                                                 .messages();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T inLocale(Locale locale, Supplier<T> action) {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return action.get();
        } finally {
            Locale.setDefault(original);
        }
    }
}
