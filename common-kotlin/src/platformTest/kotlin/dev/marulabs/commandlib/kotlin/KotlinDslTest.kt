// Compiled by the tests of each <platform>-kotlin module against its platform and its testing module.
package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.CommandContext
import dev.marulabs.commandlib.CommandScheduler
import dev.marulabs.commandlib.CommandTester
import dev.marulabs.commandlib.DefaultPermission
import dev.marulabs.commandlib.FakeSender
import dev.marulabs.commandlib.Options
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.exception.CommandPrerequisiteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.ArrayDeque

class KotlinDslTest {
    private fun dslKit(): Command =
        command("kit") {
            description("Manage kits")
            permission(DefaultPermission.ALL)
            addAliases("kits")
            subcommand("give") {
                description("Give a kit")
                val force by flag("force", 'f', "Skip the checks")
                val limit by option(Options.integer("limit", 'n', 10, 1, 100))
                argument(StringArgument("name"), IntegerArgument("count").optional(1)) { name, count ->
                    description("Give a kit to yourself")
                    execute { ctx -> ctx.sendMessage("${name.get()} x${count.get()} force=$force limit=$limit") }
                }
            }
            subcommand("heal") {
                argument(StringArgument("targets").variadic()) { targets ->
                    execute { ctx -> ctx.sendMessage("Healed " + targets.get().joinToString(",")) }
                }
            }
            argument(IntegerArgument("id")) { id ->
                permission("kit.id")
                subcommand("show") { execute { ctx -> ctx.sendMessage("Kit ${id.get()}") } }
            }
            subcommand("list") {
                description("List the kits")
                execute { ctx -> ctx.sendMessage("No kits") }
            }
        }

    // The same command written with the fluent API.
    private fun fluentKit(): Command =
        Command("kit").apply {
            description("Manage kits")
            permission(DefaultPermission.ALL)
            addAliases("kits")
            addChildren(
                Command("give").apply {
                    description("Give a kit")
                    val force = option(Options.flag<CommandContext>("force", 'f').description("Skip the checks"))
                    val limit = option(Options.integer<CommandContext>("limit", 'n', 10, 1, 100))
                    argument(StringArgument("name"), IntegerArgument("count").optional(1))
                        .description("Give a kit to yourself")
                        .execute { name, count, ctx ->
                            ctx.sendMessage("$name x$count force=${ctx.getOption(force)} limit=${ctx.getOption(limit)}")
                        }
                },
            )
            addChildren(
                Command("heal").apply {
                    argument(StringArgument("targets").variadic()).execute { targets, ctx ->
                        ctx.sendMessage("Healed " + targets.joinToString(","))
                    }
                },
            )
            argument(IntegerArgument("id"))
                .permission("kit.id")
                .child { id -> Command("show").apply { execute { ctx -> ctx.sendMessage("Kit ${id.get()}") } } }
            addChildren(
                Command("list").apply {
                    description("List the kits")
                    execute { ctx -> ctx.sendMessage("No kits") }
                },
            )
        }

    // Some arguments need the tester's stubs when they are created, so the testers create the commands.
    private fun run(
        command: () -> Command,
        vararg inputs: String,
    ): List<String> {
        val admin = FakeSender.player("Admin")
        CommandTester(command, "test.command").use { tester -> inputs.forEach { tester.execute(it, admin) } }
        return admin.sentMessageTexts
    }

    @Test
    fun `the DSL builds the tree of the fluent API`() {
        val dslTree = CommandTester(::dslKit, "test.command").use { it.commandTree() }
        val fluentTree = CommandTester(::fluentKit, "test.command").use { it.commandTree() }

        assertThat(dslTree).isEqualTo(fluentTree).contains("give", "heal", "show", "list", "--force")
    }

    @Test
    fun `the DSL gives the help and the results of the fluent API`() {
        val inputs =
            arrayOf(
                "kit help",
                "kit give help",
                "kit give apple",
                "kit give -f -n 5 apple 3",
                "kit heal Admin Steve",
                "kit 7 show",
                "kit list",
            )

        val dsl = run(::dslKit, *inputs)

        assertThat(dsl).isEqualTo(run(::fluentKit, *inputs))
        assertThat(dsl).contains(
            "apple x1 force=false limit=10",
            "apple x3 force=true limit=5",
            "Healed Admin,Steve",
            "Kit 7",
            "No kits",
        )
        assertThat(dsl).anyMatch { it.contains("/kit give [options] <name> [count]") }
    }

    @Test
    fun `a branch of eight arguments reads each value typed from its argument`() {
        val numbers = (1..7).map { IntegerArgument("n$it") }
        val label = StringArgument("label")
        val sum =
            command("sum") {
                arguments(numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], numbers[5], numbers[6], label) {
                    description("Sum seven numbers")
                    execute { ctx -> ctx.sendMessage(ctx[label] + ":" + numbers.sumOf { ctx[it] }) }
                    subcommand("first") { execute { ctx -> ctx.sendMessage("${ctx[label]}:${ctx[numbers[0]]}") } }
                }
            }
        val sender = FakeSender.player("Steve")

        CommandTester(sum, "test.command").use { tester ->
            tester.execute("sum 1 2 3 4 5 6 7 total", sender)
            tester.execute("sum 1 2 3 4 5 6 7 total first", sender)
        }

        assertThat(sender.sentMessageTexts).containsExactly("total:28", "total:1")
    }

    @Test
    fun `executeSuspend finishes on the sync thread after the async work`() {
        val asyncTasks = ArrayDeque<Runnable>()
        val sender = FakeSender.player("Steve")
        val stats =
            command("stats") {
                argument(StringArgument("player")) { player ->
                    executeSuspend { ctx ->
                        // The reference also resolves on the async executor.
                        val kills = withContext(ctx.asyncDispatcher) { player.get().length }
                        ctx.sendMessage("${player.get()} has $kills kills")
                    }
                }
            }

        CommandTester
            .builder()
            .command(stats)
            .permissionPrefix("test.command")
            .scheduler(CommandScheduler.of(Runnable::run, asyncTasks::add))
            .build()
            .use { tester ->
                tester.execute("stats Alex", sender)
                assertThat(sender.sentMessageTexts).isEmpty()

                generateSequence { asyncTasks.poll() }.forEach(Runnable::run)
            }

        assertThat(sender.sentMessageTexts).containsExactly("Alex has 4 kills")
    }

    @Test
    fun `executeSuspend reports a prerequisite failure`() {
        val sender = FakeSender.player("Steve")
        val denied = command("denied") { executeSuspend { throw CommandPrerequisiteException("Not now.") } }

        CommandTester(denied, "test.command").use { it.execute("denied", sender) }

        assertThat(sender.sentMessageTexts).containsExactly("Not now.")
    }

    @Test
    fun `a suspending suggestion provider adds suggestions after it resumes`() {
        val warp =
            command("warp") {
                val names =
                    StringArgument("name").addSuggestionProviderSuspend { sb ->
                        withContext(Dispatchers.Default) { listOf("spawn", "shop") }.forEach { sb.suggest(it) }
                    }
                argument(names) { execute { } }
            }

        val suggestions = CommandTester(warp, "test.command").use { it.suggest("warp s", FakeSender.player("Steve")) }

        assertThat(suggestions).containsExactlyInAnyOrder("spawn", "shop")
    }

    class GamePlayer(
        val name: String,
    )

    class Started(
        val tick: Long,
    )

    @Test
    fun `providers and middlewares give typed values to the executors`() {
        val sender = FakeSender.player("Steve")
        val team =
            command("team") {
                provide { ctx -> GamePlayer(ctx.actor.name) }
                addMiddleware { ctx, next ->
                    ctx.put(Started(20))
                    next.proceed()
                }
                argument(StringArgument("name")) { name ->
                    execute { ctx ->
                        val started = ctx.get<Started>().tick
                        ctx.sendMessage("${ctx.get<GamePlayer>().name} joins ${name.get()} at $started")
                        ctx.sendMessage("missing: ${ctx.getOrNull<String>()}")
                    }
                }
            }

        CommandTester(team, "test.command").use { it.execute("team red", sender) }

        assertThat(sender.sentMessageTexts).containsExactly("Steve joins red at 20", "missing: null")
    }
}
