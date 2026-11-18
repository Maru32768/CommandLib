package dev.marulabs.cadence.examples;

import dev.marulabs.cadence.FakeSender;
import dev.marulabs.cadence.annotation.AnnotatedCommands;
import dev.marulabs.cadence.examples.ban.AnnotatedBanCommand;
import dev.marulabs.cadence.examples.ban.BuilderBanCommand;
import dev.marulabs.cadence.examples.ban.FluentBanCommand;
import dev.marulabs.cadence.examples.ban.KotlinBanCommandKt;
import org.junit.jupiter.api.Test;

class BanExampleTest {
    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("ban.snap",
                                           FluentBanCommand::new,
                                           BuilderBanCommand::create,
                                           () -> AnnotatedCommands.parse(new AnnotatedBanCommand()),
                                           KotlinBanCommandKt::kotlinBanCommand,
                                           t -> {
                                               t.run("ban help");
                                               t.run("ban Steve");
                                               t.run("ban -s -r griefing Steve");
                                               t.run("ban --silent --reason \"being rude\" Alex");
                                               t.run("ban Steve -s");
                                               t.run("ban Steve",
                                                     FakeSender.player("Guest")
                                                               .denyPermissions("example.command.ban"));
                                           });
    }
}
