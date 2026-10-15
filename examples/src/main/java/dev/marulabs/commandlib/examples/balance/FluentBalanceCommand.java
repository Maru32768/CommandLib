package dev.marulabs.commandlib.examples.balance;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * Use case 5, dependency injection and sender mapping: the {@link Economy} comes through the constructor, and the
 * command provides the sender's {@link Economy.Account} to itself and its children, which read it with
 * {@code ctx.get(Economy.Account.class)}. {@code /balance} and {@code /balance pay <target> <amount>}.
 */
public final class FluentBalanceCommand extends Command {
    public FluentBalanceCommand(Economy economy) {
        super("balance");
        description("Show and spend your coins");
        provide(Economy.Account.class, ctx -> economy.account(ctx.getSender()));

        execute(ctx -> ctx.sendMessage("You have " + ctx.get(Economy.Account.class)
                                                        .balance()
                + " coins"));
        addChildren(new Command("pay") {{
            description("Pay a player");
            argument(new PlayerArgument("target"),
                     new IntegerArgument("amount", 1, 1000)).execute((target, amount, ctx) -> {
                         Economy.Account account = ctx.get(Economy.Account.class);
                         economy.pay(account, target.getName(), amount);
                         ctx.sendSuccess("Paid " + amount + " to " + target.getName() + ", " + account.balance()
                                 + " left");
                     });
        }});
    }
}
