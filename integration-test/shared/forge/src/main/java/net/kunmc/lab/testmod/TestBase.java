package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TestBase {
    private static final String COMMAND_NOT_EXECUTED_MESSAGE = "Command was not executed.";
    private final Map<String, TestResult> resultMap = new ConcurrentHashMap<>();
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
    }

    protected final void putResult(String key, String actual, String expected) {
        if (expected.equals(actual)) {
            putResult(new TestResult(key, TestStatus.SUCCEEDED, actual));
            return;
        }

        putResult(new TestResult(key, TestStatus.FAILED, "Expected " + expected + " but was " + actual));
    }

    protected final void putException(String key, Throwable e) {
        putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
    }

    protected final String buildCommand(String subCommand) {
        return command.name() + " " + subCommand;
    }

    protected final String getMethodName() {
        StackTraceElement[] ste = Thread.currentThread()
                                        .getStackTrace();
        return ste[2].getMethodName();
    }

    protected final String getKey(String methodName) {
        return getClass().getSimpleName() + "." + methodName;
    }

    public final List<TestResult> results() {
        return new ArrayList<>(resultMap.values());
    }

    public final void clearResults() {
        resultMap.clear();
    }
}
