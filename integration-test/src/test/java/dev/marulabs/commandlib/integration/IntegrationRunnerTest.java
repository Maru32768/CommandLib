package dev.marulabs.commandlib.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class IntegrationRunnerTest {
    @TempDir
    Path plugins;

    @Test
    void accepts_bukkit_and_mohist_player_join_logs() {
        assertThat(IntegrationTest.isPlayerJoinLog("[12:47:42 INFO]: Maru32768 joined the game")).isTrue();
        assertThat(IntegrationTest.isPlayerJoinLog("[12:47:42 INFO]: Maru32768[/172.17.0.1:52648] logged in with entity id 127 at (-245.5, 63, 153.5)")).isTrue();
        assertThat(IntegrationTest.isPlayerJoinLog("[12:47:42 INFO]: OtherPlayer joined the game")).isFalse();
    }

    @Test
    void selects_only_the_fixture_jar() throws Exception {
        Path fixture = Files.createFile(plugins.resolve("TestPlugin-1.jar"));
        Files.createFile(plugins.resolve("helper.jar"));
        Files.createDirectory(plugins.resolve("TestPlugin-directory.jar"));
        assertThat(IntegrationTest.findTestPluginJar(plugins, "TestPlugin.*\\.jar")).isEqualTo(fixture);
    }

    @Test
    void refuses_missing_or_ambiguous_fixtures_instead_of_running_stale_code() throws Exception {
        assertThatIllegalStateException().isThrownBy(() -> IntegrationTest.findTestPluginJar(plugins,
                                                                                             "TestMod.*\\.jar"))
                                         .withMessageContaining("Expected exactly one test plugin jar");
        Files.createFile(plugins.resolve("TestMod-old.jar"));
        Files.createFile(plugins.resolve("TestMod-new.jar"));
        assertThatIllegalStateException().isThrownBy(() -> IntegrationTest.findTestPluginJar(plugins,
                                                                                             "TestMod.*\\.jar"))
                                         .withMessageContaining("TestMod-old.jar")
                                         .withMessageContaining("TestMod-new.jar");
    }
}
