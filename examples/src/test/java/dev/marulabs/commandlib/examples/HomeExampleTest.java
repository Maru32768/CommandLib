package dev.marulabs.commandlib.examples;

import dev.marulabs.commandlib.FakeSender;
import dev.marulabs.commandlib.examples.home.AnnotatedHomeCommand;
import dev.marulabs.commandlib.examples.home.BuilderHomeCommand;
import dev.marulabs.commandlib.examples.home.FluentHomeCommand;
import dev.marulabs.commandlib.examples.home.KotlinHomeCommandKt;
import org.junit.jupiter.api.Test;

class HomeExampleTest {
    @Test
    void every_style_gives_the_same_tree_help_and_results_in_each_locale() {
        StyleParity.assertSameInEveryStyle("home.snap",
                                           FluentHomeCommand::new,
                                           BuilderHomeCommand::create,
                                           AnnotatedHomeCommand::create,
                                           KotlinHomeCommandKt::kotlinHomeCommand,
                                           t -> {
                                               FakeSender japanese = FakeSender.player("Taro")
                                                                               .locale("ja_jp");
                                               t.line("## en_us");
                                               t.run("home help");
                                               t.run("home set base");
                                               t.run("home go base");
                                               t.run("home go mine");
                                               t.line("## ja_jp");
                                               t.run("home help", japanese);
                                               t.run("home set base", japanese);
                                               t.run("home go base", japanese);
                                               t.run("home go mine", japanese);
                                           });
    }
}
