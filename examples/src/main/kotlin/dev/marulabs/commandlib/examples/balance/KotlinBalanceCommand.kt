package dev.marulabs.commandlib.examples.balance

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.subcommand

/** [FluentBalanceCommand] written with the Kotlin DSL; the service is a parameter of the factory. */
fun kotlinBalanceCommand(economy: Economy): Command =
    command("balance") {
        description("Show and spend your coins")
        require { ctx -> economy.account(ctx.sender) }
            .execute { account, ctx -> ctx.sendMessage("You have ${account.balance()} coins") }
        subcommand("pay") {
            description("Pay a player")
            argument(PlayerArgument("target"), IntegerArgument("amount", 1, 1000))
                .require { ctx -> economy.account(ctx.sender) }
                .execute { target, amount, account, ctx ->
                    account.pay(target.name, amount)
                    ctx.sendSuccess("Paid $amount to ${target.name}, ${account.balance()} left")
                }
        }
    }
