package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.PlatformAdapter;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

        // Fails unless the conversion ran without failing or reporting a broken NMS bridge, so neither a missing command
        // nor a silent fallback to plain text can pass. An unexpected execution reaches the executor and overrides it.
        putResult(new TestResult(key, TestStatus.FAILED, "parseImpl was not invoked."));
        command.addChildren(new Command(name) {{
            argument(new ThrowingBoolArg("a", result -> putResult(new TestResult(key,
                                                                                 result == null ? TestStatus.SUCCEEDED : TestStatus.FAILED,
                                                                                 result == null ? "Succeeded converting CommandSyntaxException" : result)),
                                         (e, ctx) -> putResult(new TestResult(key,
                                                                              TestStatus.FAILED,
                                                                              ExceptionUtil.stackTraceToString(e))))).execute(
                    (a, ctx) -> {
                        putResult(new TestResult(key, TestStatus.FAILED, "CommandSyntaxException was not thrown."));
                    });
        }});

        return List.of(buildCommand(command, name + " true"));
    }

    private static final class ThrowingBoolArg extends Argument<Object, ThrowingBoolArg> {
        private final Consumer<String> onConverted;

        /**
         * @param onConverted receives null when the conversion succeeded, or a failure description
         */
        ThrowingBoolArg(String name, Consumer<String> onConverted, UncaughtExceptionHandler<CommandContext> handler) {
            super(name, BoolArgumentType.bool());
            this.onConverted = onConverted;
            addUncaughtExceptionHandler(handler);
        }

        @Override
        public Object cast(Object parsedArgument) {
            return parsedArgument;
        }

        @Override
        protected Object parseImpl(CommandContext ctx) throws CommandSyntaxException {
            CommandSyntaxException e = CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh()
                                                                                 .create(1, 2);
            onConverted.accept(checkConversion(e));
            throw e;
        }

        /**
         * Converts the exception the same way CommandLib does after parseImpl throws it, and reports a conversion that
         * throws or logs a warning about falling back to plain text.
         */
        private static String checkConversion(CommandSyntaxException e) {
            PlatformAdapter<?, ?, ?, ?> adapter = PlatformAdapter.get();
            Logger logger = Logger.getLogger(adapter.getClass()
                                                    .getName());
            List<LogRecord> warnings = new ArrayList<>();
            Handler handler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (record.getLevel()
                              .intValue() >= Level.WARNING.intValue()) {
                        warnings.add(record);
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            logger.addHandler(handler);
            try {
                adapter.convertCommandSyntaxException(e);
            } catch (Throwable t) {
                return ExceptionUtil.stackTraceToString(t);
            } finally {
                logger.removeHandler(handler);
            }
            if (!warnings.isEmpty()) {
                LogRecord warning = warnings.get(0);
                return warning.getMessage() + (warning.getThrown() == null ? "" : "\n" + ExceptionUtil.stackTraceToString(
                        warning.getThrown()));
            }
            return null;
        }
    }
}
