package dev.marulabs.commandlib;

/**
 * Opens the package-private {@link Snapshots} of {@code common} to the example tests.
 */
public final class ExampleSnapshots {
    private ExampleSnapshots() {
    }

    public static void assertMatches(String snapshotName, String actual) {
        Snapshots.assertMatches(snapshotName, actual);
    }
}
