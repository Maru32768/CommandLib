package net.kunmc.lab.testmod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;
import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import java.util.List;

public final class CommandSyntaxExceptionTest extends TestBase {
    public CommandSyntaxExceptionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return List.of(testConvertCommandSyntaxException());
    }

    private String testConvertCommandSyntaxException() {
        String name = getMethodName();
        String key = getKey(name);

        // Fails unless parseImpl actually ran, so a missing command cannot pass. A conversion failure reaches the
        // uncaught exception handler and an unexpected execution reaches the executor; both override the result.
        putResult(new TestResult(key, TestStatus.FAILED, "parseImpl was not invoked."));
        command.addChildren(new Command(name) {{
            argument(new ThrowingBoolArg("a",
                                         () -> putResult(new TestResult(key,
                                                                        TestStatus.SUCCEEDED,
                                                                        "Succeeded converting CommandSyntaxException")),
                                         (e, ctx) -> putResult(new TestResult(key,
                                                                              TestStatus.FAILED,
                                                                              ExceptionUtil.stackTraceToString(e))))).execute(
                    (a, ctx) -> {
                        putResult(new TestResult(key, TestStatus.FAILED, "CommandSyntaxException was not thrown."));
                    });
        }});

        return buildCommand(name + " true");
    }

    private static final class ThrowingBoolArg extends Argument<Object, ThrowingBoolArg> {
        private final Runnable onParse;

        ThrowingBoolArg(String name, Runnable onParse, UncaughtExceptionHandler<CommandContext> handler) {
            super(name, BoolArgumentType.bool());
            this.onParse = onParse;
            addUncaughtExceptionHandler(handler);
        }

        @Override
        public Object cast(Object parsedArgument) {
            return parsedArgument;
        }

        @Override
        protected Object parseImpl(CommandContext ctx) throws CommandSyntaxException {
            onParse.run();
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh()
                                                            .create(1, 2);
        }
    }
}
