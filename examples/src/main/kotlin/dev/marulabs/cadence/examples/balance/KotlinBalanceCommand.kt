package dev.marulabs.cadence.examples.balance

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.argument.IntegerArgument
import dev.marulabs.cadence.argument.PlayerArgument
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.get
import dev.marulabs.cadence.kotlin.provide
import dev.marulabs.cadence.kotlin.subcommand

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
