package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.TestStatus;

public interface CommandDispatchErrorHook {
    void onCommandDispatchError(String commandLine, CommandDispatchResult dispatchResult);
}
