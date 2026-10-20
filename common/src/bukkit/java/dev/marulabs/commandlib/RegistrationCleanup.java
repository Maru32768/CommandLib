package dev.marulabs.commandlib;

/**
 * The steps that remove a registration, shared by the Spigot and Paper {@code CommandLib}.
 */
final class RegistrationCleanup {
    private RegistrationCleanup() {
    }

    /**
     * Runs every step, also after one throws, since unregister() cannot be retried, and then throws the first failure,
     * the root cause, with the later ones suppressed.
     */
    static void runAll(Runnable... steps) {
        Throwable failure = null;
        for (Runnable step : steps) {
            try {
                step.run();
            } catch (RuntimeException | Error e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure != null) {
            throw (RuntimeException) failure;
        }
    }
}
