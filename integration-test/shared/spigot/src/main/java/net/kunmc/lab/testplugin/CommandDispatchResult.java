package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandDispatchResult {
    private final boolean dispatched;
    private final Throwable throwable;
    private final List<String> logMessages;

    public CommandDispatchResult(boolean dispatched, Throwable throwable, List<String> logMessages) {
        this.dispatched = dispatched;
        this.throwable = throwable;
        this.logMessages = Collections.unmodifiableList(new ArrayList<>(logMessages));
    }

    public String describe(String commandLine) {
        List<String> lines = new ArrayList<>();
        lines.add("Command: " + commandLine);
        lines.add("Bukkit.dispatchCommand returned: " + dispatched);

        if (throwable != null) {
            lines.add("Dispatch exception:");
            lines.add(ExceptionUtil.stackTraceToString(throwable));
        }

        if (!logMessages.isEmpty()) {
            lines.add("Server log records captured during dispatch:");
            lines.add(logMessages.stream()
                                 .collect(Collectors.joining("\n")));
        }

        if (throwable == null && logMessages.isEmpty()) {
            lines.add("No exception or server log record was captured during dispatch.");
        }

        return String.join("\n", lines);
    }
}
