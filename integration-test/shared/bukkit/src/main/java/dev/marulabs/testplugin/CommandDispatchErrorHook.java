package dev.marulabs.testplugin;

import dev.marulabs.integration.core.TestStatus;

public interface CommandDispatchErrorHook {
    void onCommandDispatchError(String commandLine, CommandDispatchResult dispatchResult);
}
