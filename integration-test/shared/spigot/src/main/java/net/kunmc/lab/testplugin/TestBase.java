package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TestBase {
    private static final String COMMAND_NOT_EXECUTED_MESSAGE = "Command was not executed.";
    private final Map<String, TestResult> resultMap = new ConcurrentHashMap<>();
    private final Map<String, List<CommandDispatchErrorHook>> commandDispatchErrorHooks = new ConcurrentHashMap<>();
    protected final Command command;

    public TestBase(Command command) {
        this.command = command;
    }

    public abstract List<String> build();

    protected final void putResult(TestResult result) {
        resultMap.put(result.key(), result);
    }

    protected final void putCommandNotExecutedResult(String key) {
        putResult(new TestResult(key, TestStatus.FAILED, COMMAND_NOT_EXECUTED_MESSAGE));
        addCommandDispatchErrorHook(key,
                                    (commandLine, dispatchResult) -> resultMap.computeIfPresent(key,
                                                                                                (ignored, result) -> {
                                                                                                    if (result.status() != TestStatus.FAILED || !COMMAND_NOT_EXECUTED_MESSAGE.equals(
                                                                                                            result.message())) {
                                                                                                        return result;
                                                                                                    }

                                                                                                    return new TestResult(
                                                                                                            key,
                                                                                                            TestStatus.FAILED,
                                                                                                            COMMAND_NOT_EXECUTED_MESSAGE + "\n" + dispatchResult.describe(
                                                                                                                    commandLine));
                                                                                                }));
    }

    protected final void addCommandDispatchErrorHook(String key, CommandDispatchErrorHook hook) {
        commandDispatchErrorHooks.computeIfAbsent(key, ignored -> Collections.synchronizedList(new ArrayList<>()))
                                 .add(hook);
    }

    protected final void putResult(String key, String actual, String expected) {
        if (expected.equals(actual)) {
            putResult(new TestResult(key, TestStatus.SUCCEEDED, actual));
            return;
        }

        putResult(new TestResult(key, TestStatus.FAILED, "Expected " + expected + " but was " + actual));
    }

    protected final String buildCommand(Command command, String subCommand) {
        return command.name() + " " + subCommand;
    }

    protected final String getMethodName() {
        return getMethodName(1);
    }

    protected final String getMethodName(int depth) {
        final StackTraceElement[] ste = Thread.currentThread()
                                              .getStackTrace();
        return ste[2 + depth].getMethodName();
    }

    protected final String getKey() {
        return getClass().getSimpleName() + "." + getMethodName(1);
    }

    public final List<TestResult> results() {
        return new ArrayList<>(Collections.unmodifiableCollection(resultMap.values()));
    }

    public final void clearResults() {
        resultMap.clear();
    }

    public final void hookCommandDispatchError(String commandLine, CommandDispatchResult dispatchResult) {
        String methodName = extractSubCommand(commandLine);
        if (methodName == null) {
            return;
        }

        String key = getClass().getSimpleName() + "." + methodName;
        List<CommandDispatchErrorHook> hooks = commandDispatchErrorHooks.get(key);
        if (hooks == null) {
            return;
        }
        hooks.forEach(hook -> hook.onCommandDispatchError(commandLine, dispatchResult));
    }

    private String extractSubCommand(String commandLine) {
        String[] parts = commandLine.trim()
                                    .split("\\s+", 3);
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }
}
