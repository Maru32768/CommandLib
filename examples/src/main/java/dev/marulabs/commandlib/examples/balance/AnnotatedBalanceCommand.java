package dev.marulabs.commandlib.examples.balance;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.annotation.Range;
import dev.marulabs.commandlib.annotation.Subcommand;
import org.bukkit.entity.Player;

/**
 * {@link FluentBalanceCommand} written with annotations. The parser resolves {@link Economy.Account} parameters from
 * the context with {@code registerContextValue}, the annotation counterpart of {@code require(...)}.
 */
@Command("balance")
@Description("Show and spend your coins")
public final class AnnotatedBalanceCommand {
    public static dev.marulabs.commandlib.Command create(Economy economy) {
        return AnnotatedCommands.parser()
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
        account.pay(target.getName(), amount);
        ctx.sendSuccess("Paid " + amount + " to " + target.getName() + ", " + account.balance() + " left");
    }
}
