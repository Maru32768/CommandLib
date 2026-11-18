package dev.marulabs.cadence.examples;

import dev.marulabs.cadence.examples.team.AnnotatedTeamCommand;
import dev.marulabs.cadence.examples.team.BuilderTeamCommand;
import dev.marulabs.cadence.examples.team.FluentTeamCommand;
import dev.marulabs.cadence.examples.team.KotlinTeamCommandKt;
import dev.marulabs.cadence.examples.team.Teams;
import org.junit.jupiter.api.Test;

class TeamExampleTest {
    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("team.snap",
                                           () -> new FluentTeamCommand(new Teams()),
                                           () -> BuilderTeamCommand.create(new Teams()),
                                           () -> AnnotatedTeamCommand.create(new Teams()),
                                           () -> KotlinTeamCommandKt.kotlinTeamCommand(new Teams()),
                                           t -> {
                                               t.suggest("team ");
                                               t.suggest("team red ");
                                               t.run("team");
                                               t.run("team help");
                                               t.run("team red add Steve");
                                               t.run("team red add Alex");
                                               t.run("team red remove Steve");
                                               t.run("team red");
                                               t.run("team red help");
                                               t.run("team green add Steve");
                                           });
    }
}
