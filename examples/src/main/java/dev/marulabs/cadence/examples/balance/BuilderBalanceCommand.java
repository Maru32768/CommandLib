package dev.marulabs.cadence.examples.balance;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.IntegerArgument;
import dev.marulabs.cadence.argument.PlayerArgument;

/**
 * {@link FluentBalanceCommand} written with the Java builder.
 */
public final class BuilderBalanceCommand {
    private BuilderBalanceCommand() {
    }

    public static Command create(Economy economy) {
        return Command.builder("balance")
                      .description("Show and spend your coins")
                      .provide(Economy.Account.class, ctx -> economy.account(ctx.getSender()))
                      .execute(ctx -> ctx.sendMessage("You have " + ctx.get(Economy.Account.class)
                                                                       .balance()
                              + " coins"))
                      .addChild(Command.builder("pay")
                                       .description("Pay a player")
                                       .argument(new PlayerArgument("target"),
                                                 new IntegerArgument("amount", 1, 1000),
                                                 branch -> branch.execute((ctx, target, amount) -> {
                                                     Economy.Account account = ctx.get(Economy.Account.class);
                                                     economy.pay(account, target.getName(), amount);
                                                     ctx.sendSuccess("Paid " + amount + " to " + target.getName() + ", "
                                                             + account.balance() + " left");
                                                 })))
                      .build();
    }
}
