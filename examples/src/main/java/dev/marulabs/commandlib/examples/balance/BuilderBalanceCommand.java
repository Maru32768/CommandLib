package dev.marulabs.commandlib.examples.balance;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * {@link FluentBalanceCommand} written with the Java builder.
 */
public final class BuilderBalanceCommand {
    private BuilderBalanceCommand() {
    }

    public static Command create(Economy economy) {
        return Command.builder("balance")
                      .description("Show and spend your coins")
                      .require(ctx -> economy.account(ctx.getSender()), branch -> branch.execute((account, ctx) -> {
                          ctx.sendMessage("You have " + account.balance() + " coins");
                      }))
                      .addChild(Command.builder("pay")
                                       .description("Pay a player")
                                       .argument(new PlayerArgument("target"),
                                                 new IntegerArgument("amount", 1, 1000),
                                                 branch -> branch.require(ctx -> economy.account(ctx.getSender()))
                                                                 .execute((target, amount, account, ctx) -> {
                                                                     account.pay(target.getName(), amount);
                                                                     ctx.sendSuccess("Paid " + amount + " to "
                                                                             + target.getName() + ", "
                                                                             + account.balance() + " left");
                                                                 })))
                      .build();
    }
}
