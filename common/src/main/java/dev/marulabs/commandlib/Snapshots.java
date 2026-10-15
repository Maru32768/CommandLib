package dev.marulabs.commandlib;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares text with a golden snapshot file under {@code src/test/resources/snapshots}, relative to the working
 * directory, which Gradle and Maven set to the project directory when they run tests. A missing snapshot is written
 * and fails the test so that it is reviewed. With the system property {@code commandlib.updateSnapshots=true} or the
 * environment variable {@code COMMANDLIB_UPDATE_SNAPSHOTS=true}, every snapshot is rewritten. Gradle does not pass its
 * own {@code -D} options to the test JVM, but the test JVM inherits the environment.
 */
final class Snapshots {
    static final String UPDATE_PROPERTY = "commandlib.updateSnapshots";
    static final String UPDATE_ENV = "COMMANDLIB_UPDATE_SNAPSHOTS";

    private Snapshots() {
    }

    static void assertMatches(String snapshotName, String actual) {
        Path path = Path.of("src/test/resources/snapshots", snapshotName);
        String normalized = normalize(actual);
        try {
            if (Boolean.getBoolean(UPDATE_PROPERTY) || "true".equalsIgnoreCase(System.getenv(UPDATE_ENV))) {
                write(path, normalized);
                return;
            }
            if (!Files.exists(path)) {
                write(path, normalized);
                throw new AssertionError("Wrote the missing snapshot " + path.toAbsolutePath()
                        + "; review it and run the test again." + System.lineSeparator() + normalized);
            }
            String expected = normalize(Files.readString(path, StandardCharsets.UTF_8));
            if (!expected.equals(normalized)) {
                throw new AssertionError("Snapshot " + path.toAbsolutePath() + " does not match. Set " + UPDATE_ENV
                        + "=true to update it." + System.lineSeparator() + "Expected:" + System.lineSeparator()
                        + expected + System.lineSeparator() + "Actual:" + System.lineSeparator() + normalized);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not access snapshot " + path.toAbsolutePath(), e);
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n")
                   .stripTrailing();
    }

    private static void write(Path path, String text) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, text + "\n", StandardCharsets.UTF_8);
    }
}
