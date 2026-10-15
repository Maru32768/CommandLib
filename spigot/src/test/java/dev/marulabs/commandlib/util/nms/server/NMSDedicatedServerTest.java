package dev.marulabs.commandlib.util.nms.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NMSDedicatedServerTest {
    @Test
    void keepPendingLoadUndoesTheLoadRunReplaceLibrarySchedules() {
        FunctionManager manager = new FunctionManager();

        NMSDedicatedServer.keepPendingLoad(manager, manager::replaceLibrary, false);

        assertThat(manager.replacements).isEqualTo(1);
        assertThat(manager.postReload).isFalse();
        assertThat(manager.isInFunction).isFalse();
    }

    @Test
    void keepPendingLoadKeepsALoadRunPendingFromADataReload() {
        FunctionManager manager = new FunctionManager();
        manager.postReload = true;

        NMSDedicatedServer.keepPendingLoad(manager, manager::replaceLibrary, false);

        assertThat(manager.postReload).isTrue();
    }

    @Test
    void keepPendingLoadKeepsTheLoadRunOfAReloadWhoseLoadTagTheServerMissed() {
        FunctionManager manager = new FunctionManager();

        NMSDedicatedServer.keepPendingLoad(manager, manager::replaceLibrary, true);

        assertThat(manager.postReload).isTrue();
        assertThat(manager.isInFunction).isFalse();
    }

    /**
     * The fields of 1.16.5's ServerFunctionManager that matter here: two booleans, and replaceLibrary sets postReload.
     */
    private static final class FunctionManager {
        private boolean isInFunction;
        private boolean postReload;
        private int replacements;

        void replaceLibrary() {
            postReload = true;
            replacements++;
        }
    }
}
