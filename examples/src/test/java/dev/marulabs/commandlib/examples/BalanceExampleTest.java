package dev.marulabs.commandlib.examples;

import dev.marulabs.commandlib.FakeSender;
import dev.marulabs.commandlib.examples.balance.AnnotatedBalanceCommand;
import dev.marulabs.commandlib.examples.balance.BuilderBalanceCommand;
import dev.marulabs.commandlib.examples.balance.Economy;
import dev.marulabs.commandlib.examples.balance.FluentBalanceCommand;
import dev.marulabs.commandlib.examples.balance.KotlinBalanceCommandKt;
import org.junit.jupiter.api.Test;

class BalanceExampleTest {
    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("balance.snap",
                                           () -> new FluentBalanceCommand(new Economy()),
                                           () -> BuilderBalanceCommand.create(new Economy()),
                                           () -> AnnotatedBalanceCommand.create(new Economy()),
                                           () -> KotlinBalanceCommandKt.kotlinBalanceCommand(new Economy()),
                                           t -> {
                                               t.run("balance help");
                                               t.run("balance");
                                               t.run("balance pay Steve 30");
                                               t.run("balance");
                                               t.run("balance", FakeSender.player("Guest"));
                                               t.run("balance pay Steve 1", FakeSender.player("Guest"));
                                           });
    }
}
