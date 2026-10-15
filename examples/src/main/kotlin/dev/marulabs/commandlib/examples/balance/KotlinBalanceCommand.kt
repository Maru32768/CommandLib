package dev.marulabs.commandlib.examples.balance

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.get
import dev.marulabs.commandlib.kotlin.provide
import dev.marulabs.commandlib.kotlin.subcommand

/** [FluentBalanceCommand] written with the Kotlin DSL; the service is a parameter of the factory. */
fun kotlinBalanceCommand(economy: Economy): Command =
    command("balance") {
        description("Show and spend your coins")
        provide { ctx -> economy.account(ctx.sender) }
        execute { ctx -> ctx.sendMessage("You have ${ctx.get<Economy.Account>().balance()} coins") }
        subcommand("pay") {
            description("Pay a player")
            argument(PlayerArgument("target"), IntegerArgument("amount", 1, 1000)) { target, amount ->
                execute { ctx ->
                    val account = ctx.get<Economy.Account>()
                    economy.pay(account, target.get().name, amount.get())
                    ctx.sendSuccess("Paid ${amount.get()} to ${target.get().name}, ${account.balance()} left")
                }
            }
        }
    }
