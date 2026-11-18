package dev.marulabs.cadence.examples;

import dev.marulabs.cadence.annotation.AnnotatedCommands;
import dev.marulabs.cadence.examples.score.AnnotatedScoreCommand;
import dev.marulabs.cadence.examples.score.BuilderScoreCommand;
import dev.marulabs.cadence.examples.score.FluentScoreCommand;
import dev.marulabs.cadence.examples.score.KotlinScoreCommandKt;
import org.junit.jupiter.api.Test;

class ScoreExampleTest {
    /**
     * The number after {@code ->} is what {@code /execute store result} stores.
     */
    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("score.snap",
                                           FluentScoreCommand::new,
                                           BuilderScoreCommand::create,
                                           () -> AnnotatedCommands.parse(new AnnotatedScoreCommand()),
                                           KotlinScoreCommandKt::kotlinScoreCommand,
                                           t -> {
                                               t.run("score help");
                                               t.run("score Steve");
                                               t.run("score Alex");
                                               t.run("score Herobrine");
                                           });
    }
}
