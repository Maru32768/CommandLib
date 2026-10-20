// Compiled by the tests of each <platform>-kotlin module against its platform and its testing module.
package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.CommandContext
import dev.marulabs.commandlib.annotation.AnnotatedCommands
import dev.marulabs.commandlib.annotation.Execute
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import dev.marulabs.commandlib.annotation.Command as CommandName

class AnnotatedKotlinTest {
    @CommandName("wait")
    class SuspendCommand {
        @Execute
        suspend fun run(ctx: CommandContext) {
            ctx.sendMessage("done")
        }
    }

    // Without the check, the Continuation parameter fails as an argument without a type.
    @Test
    fun a_suspend_method_fails_parsing() {
        assertThatThrownBy { AnnotatedCommands.parse(SuspendCommand()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(
                "SuspendCommand#run(CommandContext, Continuation) is a Kotlin suspend function, which the annotation " +
                    "API does not support; return a CompletionStage instead",
            )
    }
}
