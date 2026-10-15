package dev.marulabs.commandlib.examples.balance;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * Use case 5, dependency injection: the {@link Economy} comes through the constructor, and {@code require(...)} maps
 * the sender to its {@link Economy.Account}, which the executors receive typed. {@code /balance} and
 * {@code /balance pay <target> <amount>}.
 */
public final class FluentBalanceCommand extends Command {
    public FluentBalanceCommand(Economy economy) {
        super("balance");
        description("Show and spend your coins");

        require(ctx -> economy.account(ctx.getSender())).execute((account, ctx) -> {
            ctx.sendMessage("You have " + account.balance() + " coins");
        });
        addChildren(new Command("pay") {{
            description("Pay a player");
            argument(new PlayerArgument("target"),
                     new IntegerArgument("amount", 1, 1000)).require(ctx -> economy.account(ctx.getSender()))
                                                            .execute((target, amount, account, ctx) -> {
                                                                account.pay(target.getName(), amount);
                                                                ctx.sendSuccess("Paid " + amount + " to "
                                                                        + target.getName() + ", "
                                                                        + account.balance() + " left");
                                                            });
        }});
    }
}
