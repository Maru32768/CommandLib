package dev.marulabs.commandlib;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotsTest {
    private static final String NAME = "snapshots-test.snap";
    private static final Path PATH = Path.of("src/test/resources/snapshots", NAME);

    @AfterEach
    void cleanUp() throws IOException {
        System.clearProperty(Snapshots.UPDATE_PROPERTY);
        Files.deleteIfExists(PATH);
    }

    @Test
    void missing_snapshot_is_written_and_fails() throws IOException {
        assertThatThrownBy(() -> Snapshots.assertMatches(NAME, "a\nb")).isInstanceOf(AssertionError.class)
                                                                       .hasMessageContaining("Wrote the missing snapshot");

        assertThat(Files.readString(PATH, StandardCharsets.UTF_8)).isEqualTo("a\nb\n");
        Snapshots.assertMatches(NAME, "a\r\nb\n");
    }

    @Test
    void mismatch_fails_and_update_property_rewrites() throws IOException {
        Files.writeString(PATH, "old\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> Snapshots.assertMatches(NAME, "new")).isInstanceOf(AssertionError.class)
                                                                      .hasMessageContaining("does not match");

        System.setProperty(Snapshots.UPDATE_PROPERTY, "true");
        Snapshots.assertMatches(NAME, "new");
        assertThat(Files.readString(PATH, StandardCharsets.UTF_8)).isEqualTo("new\n");
    }
}
