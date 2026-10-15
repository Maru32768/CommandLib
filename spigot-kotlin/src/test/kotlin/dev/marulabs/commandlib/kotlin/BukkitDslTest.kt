package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.CommandTester
import dev.marulabs.commandlib.FakeSender
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import org.assertj.core.api.Assertions.assertThat
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.junit.jupiter.api.Test

class BukkitDslTest {
    // Like the example of docs/kotlin.md, with Bukkit types in the block and in the references.
    private fun heal(): Command =
        command("heal") {
            description("Heal players")
            permission(PermissionDefault.TRUE)
            val silent by flag("silent", 's', "Do not tell the players")

            argument(IntegerArgument("amount"), PlayerArgument("targets").variadic()) { amount, targets ->
                execute { ctx ->
                    val players: List<Player> = targets.get()
                    if (!silent) ctx.sendSuccess("Healed ${players.joinToString(",") { it.name }} by ${amount.get()}")
                }
            }
            subcommand("self") {
                argument(PlayerArgument("target").optional { ctx -> ctx.player }) { target ->
                    execute { ctx -> ctx.sendSuccess("Healed ${target.get().name}") }
                }
            }
        }

    @Test
    fun `the DSL takes the Bukkit types of the platform`() {
        val admin = FakeSender.player("Admin")
        val steve = FakeSender.player("Steve")

        CommandTester(::heal, "test.command").use { tester ->
            tester.withFakePlayer(admin.asSender() as Player)
            tester.withFakePlayer(steve.asSender() as Player)
            tester.execute("heal 20 Admin Steve", admin)
            tester.execute("heal -s 5 Steve", admin)
            tester.execute("heal self", admin)
        }

        assertThat(admin.sentMessageTexts).containsExactly("Healed Admin,Steve by 20", "Healed Admin")
    }
}
