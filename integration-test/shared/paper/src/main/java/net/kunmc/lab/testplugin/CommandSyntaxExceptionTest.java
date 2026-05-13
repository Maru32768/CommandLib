package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.List;

public class CommandSyntaxExceptionTest extends TestBase {
    public CommandSyntaxExceptionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(testConvertCommandSyntaxException());

        return commands;
    }

    public List<String> testConvertCommandSyntaxException() {
        String name = getMethodName();
        String key = getKey();

        command.addChildren(new Command(name) {{
            argument(new ThrowingBoolArg("a",
                                         (e, ctx) -> putResult(new TestResult(key,
                                                                              TestStatus.FAILED,
                                                                              ExceptionUtil.stackTraceToString(e))))).execute(
                    (a, ctx) -> {
                        putResult(new TestResult(key, TestStatus.FAILED, "CommandSyntaxException was not thrown."));
                    });
        }});
        putResult(new TestResult(key, TestStatus.SUCCEEDED, "Succeeded converting CommandSyntaxException"));

        return List.of(buildCommand(command, name + " true"));
    }

    private static final class ThrowingBoolArg extends Argument<Object, ThrowingBoolArg> {
        ThrowingBoolArg(String name, UncaughtExceptionHandler<CommandContext> handler) {
            super(name, BoolArgumentType.bool());
            addUncaughtExceptionHandler(handler);
        }

        @Override
        public Object cast(Object parsedArgument) {
            return parsedArgument;
        }

        @Override
        protected Object parseImpl(CommandContext ctx) throws CommandSyntaxException {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh()
                                                            .create(1, 2);
        }
    }
}
