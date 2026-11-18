package dev.marulabs.cadence.examples.balance;

import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.annotation.AnnotatedCommands;
import dev.marulabs.cadence.annotation.Command;
import dev.marulabs.cadence.annotation.Dependency;
import dev.marulabs.cadence.annotation.Description;
import dev.marulabs.cadence.annotation.Execute;
import dev.marulabs.cadence.annotation.Range;
import dev.marulabs.cadence.annotation.Subcommand;
import org.bukkit.entity.Player;

/**
 * {@link FluentBalanceCommand} written with annotations: the parser sets the {@link Dependency} field, and gives
 * {@link Economy.Account} parameters the sender's account.
 */
@Command("balance")
@Description("Show and spend your coins")
public final class AnnotatedBalanceCommand {
    @Dependency
    private Economy economy;

    public static dev.marulabs.cadence.Command create(Economy economy) {
        return AnnotatedCommands.parser()
                                .registerDependency(Economy.class, economy)
                                .registerContextValue(Economy.Account.class, ctx -> economy.account(ctx.getSender()))
                                .parse(new AnnotatedBalanceCommand());
    }

    @Execute
    public void show(CommandContext ctx, Economy.Account account) {
        ctx.sendMessage("You have " + account.balance() + " coins");
    }

    @Subcommand("pay")
    @Description("Pay a player")
    public void pay(CommandContext ctx,
                    Economy.Account account,
                    Player target,
                    @Range(min = 1, max = 1000) int amount) {
        economy.pay(account, target.getName(), amount);
        ctx.sendSuccess("Paid " + amount + " to " + target.getName() + ", " + account.balance() + " left");
    }
}
