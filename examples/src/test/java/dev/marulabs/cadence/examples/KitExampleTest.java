package dev.marulabs.cadence.examples;

import dev.marulabs.cadence.annotation.AnnotatedCommands;
import dev.marulabs.cadence.examples.kit.AnnotatedKitCommand;
import dev.marulabs.cadence.examples.kit.BuilderKitCommand;
import dev.marulabs.cadence.examples.kit.FluentKitCommand;
import dev.marulabs.cadence.examples.kit.KotlinKitCommandKt;
import org.junit.jupiter.api.Test;

class KitExampleTest {
    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("kit.snap",
                                           FluentKitCommand::new,
                                           BuilderKitCommand::create,
                                           () -> AnnotatedCommands.parse(new AnnotatedKitCommand()),
                                           KotlinKitCommandKt::kotlinKitCommand,
                                           t -> {
                                               t.run("kit help");
                                               t.run("kit give apple");
                                               t.run("kit give apple 5");
                                               t.run("kit give apple 99");
                                               t.run("kit send Steve");
                                               t.run("kit send Steve Alex");
                                               t.run("kit send");
                                           });
    }
}
