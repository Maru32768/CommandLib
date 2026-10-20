package dev.marulabs.commandlib.integration;

import com.github.dockerjava.api.model.HostConfig;
import dev.marulabs.commandlib.integration.IntegrationTest.BotSession;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Runs the velocity-3.4.0 target: a Velocity proxy with the CommandLib test plugin in front of a Paper backend, both
 * from the target's image, and the bot joining through the proxy as a real client.
 */
class VelocityIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(VelocityIntegrationTest.class);
    private static final String SERVER_DIR = "/workspace/server";
    private static final String TEST_PLAYER_NAME = "Maru32768";
    // Set on the wait strategy: waitingFor replaces the strategy, and with it a timeout set on the container before.
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(8);

    @Test
    void commandlib_commands_on_a_velocity_proxy_reach_a_player_through_the_proxy() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("commandlib.runMinecraftIntegration"),
                               "Minecraft integration test is disabled. Enable an integration test task.");
        IntegrationTest.assertDockerAvailable();

        String targetName = System.getProperty("commandlib.targetName");
        String image = System.getProperty("commandlib.dockerImageName");
        String testPluginSubdirectory = System.getProperty("commandlib.testPluginSubdirectory");
        Path testPluginJar = IntegrationTest.findTestPluginJar(Path.of(System.getProperty("commandlib.testPluginDir"))
                                                                   .resolve(System.getProperty("commandlib.serverDirectory"))
                                                                   .resolve(testPluginSubdirectory),
                                                               System.getProperty("commandlib.testPluginJarPattern"));
        double cpuLimit = Double.parseDouble(System.getProperty("commandlib.containerCpuLimit", "2.0"));

        try (Network network = Network.newNetwork();
             GenericContainer<?> backend = container(image, targetName + "-backend", cpuLimit).withNetwork(network)
                                                                                              .withNetworkAliases("backend")
                                                                                              .withWorkingDirectory(SERVER_DIR)
                                                                                              .withCommand("sh",
                                                                                                           "-lc",
                                                                                                           "printf 'eula=true\\n' > eula.txt && java "
                                                                                                                   + System.getProperty("commandlib.serverLaunchArgs")
                                                                                                                   + " nogui")
                                                                                              .waitingFor(Wait.forLogMessage(".*Done \\(.*\\)! For help, type \"help\".*",
                                                                                                                             1)
                                                                                                              .withStartupTimeout(STARTUP_TIMEOUT));
             GenericContainer<?> proxy = container(image, targetName + "-proxy", cpuLimit).withNetwork(network)
                                                                                          .withExposedPorts(25565)
                                                                                          .withCopyFileToContainer(MountableFile.forHostPath(testPluginJar),
                                                                                                                   SERVER_DIR
                                                                                                                           + "/"
                                                                                                                           + testPluginSubdirectory
                                                                                                                           + "/"
                                                                                                                           + testPluginJar.getFileName())
                                                                                          .withWorkingDirectory(SERVER_DIR
                                                                                                  + "/proxy")
                                                                                          .withCommand("java",
                                                                                                       "-jar",
                                                                                                       "velocity.jar")
                                                                                          .waitingFor(Wait.forLogMessage(".*Done \\(.*\\)!.*",
                                                                                                                         1)
                                                                                                          .withStartupTimeout(STARTUP_TIMEOUT))) {
            backend.start();
            proxy.start();

            BotSession client = IntegrationTest.connectBot(proxy);
            try {
                Awaitility.await()
                          .atMost(Duration.ofSeconds(60))
                          .until(() -> {
                              client.assertNotDisconnected();
                              return backend.getLogs()
                                            .contains(TEST_PLAYER_NAME + " joined the game");
                          });
                IntegrationTest.waitForCommandTree(client);

                // Velocity adds its commands to the tree the backend sends, leaving out the nodes the player may not
                // use.
                List<String> children = commandTreeChildren(client, "commandlibtest");
                LOGGER.info("Children of /commandlibtest in the bot's command tree: {}", children);
                assertThat(children).as("children of /commandlibtest in the command tree the bot received")
                                    .contains("echo", "server", "find", "value", "consoleOnly", "async")
                                    .doesNotContain("denied");

                client.clearReceivedPackets();
                client.sendTabCompleteRequest(1, "/commandlibtest server ");
                List<String> suggestions = awaitSuggestions(client);
                LOGGER.info("Suggestions the bot received for /commandlibtest server: {}", suggestions);
                assertThat(suggestions).as("suggestions of ServerArgument sent to the bot")
                                       .contains("lobby");
                client.clearReceivedPackets();
                client.sendTabCompleteRequest(2, "/commandlibtest find ");
                assertThat(awaitSuggestions(client)).as("suggestions of PlayerArgument sent to the bot")
                                                    .contains(TEST_PLAYER_NAME);

                receives(client,
                         "commandlibtest echo hello",
                         "the message of a child command with an argument",
                         m -> m.contains("echo hello"));
                receives(client,
                         "commandlibtest server lobby",
                         "the message of a command with a ServerArgument",
                         m -> m.contains("server lobby"));
                receives(client,
                         "commandlibtest find " + TEST_PLAYER_NAME.toLowerCase(Locale.ROOT),
                         "the message of a command with a PlayerArgument",
                         m -> m.contains("find " + TEST_PLAYER_NAME));
                receivesValue(client, "boolean", "true", "true");
                receivesValue(client, "double", "1.5", "1.5");
                receivesValue(client, "duration", "1m30s", "PT1M30S");
                receivesValue(client, "enum", "seconds", "SECONDS");
                receivesValue(client, "float", "2.5", "2.5");
                receivesValue(client, "integer", "42", "42");
                receivesValue(client, "long", "12345678901", "12345678901");
                receivesValue(client, "nameable", "apple", "Fruit[tabCompleteName=apple]");
                receivesValue(client, "object", "one", "1");
                receivesValue(client, "string", "word", "word");
                receives(client,
                         "commandlibtest server nowhere",
                         "the incorrect argument message of an unknown server",
                         m -> m.contains("command.unknown.argument"));
                List<String> prerequisite = receives(client,
                                                     "commandlibtest consoleOnly",
                                                     "the failure message of a console-only command",
                                                     m -> m.contains("can only be executed from the console"));
                assertThat(prerequisite).noneMatch(m -> m.contains("consoleOnly executed"));
                // The parent is the player's, so Brigadier reports the hidden child as an incorrect argument.
                List<String> denied = receives(client,
                                               "commandlibtest denied",
                                               "the refusal of a command with DefaultPermission.OP",
                                               m -> m.contains("Incorrect argument for command"));
                assertThat(denied).noneMatch(m -> m.contains("denied executed"));
                // The plugin runs it as the console at startup, so the refusal is not a node that never registered.
                assertThat(proxy.getLogs()).as("proxy log")
                                           .contains("denied executed");
                receives(client,
                         "commandlibtest async",
                         "the message of an executeAsync command",
                         m -> m.contains("async done, work off the calling thread: true"));
                // CommandLib logs the unexpected errors of executors and suggestion providers with their stack trace.
                // An Error such as NoSuchMethodError names its class; a bare "Error" may be a benign log line.
                assertThat(proxy.getLogs()).as("proxy log")
                                           .doesNotContain("Exception", "\tat ")
                                           .doesNotContainPattern("\\w+Error\\b");
            } finally {
                client.disconnect("Test completed");
            }
        }
    }

    private static List<String> receives(BotSession client,
                                         String command,
                                         String description,
                                         java.util.function.Predicate<String> expected) {
        List<String> messages = IntegrationTest.assertBotReceives(client, command, description, expected);
        LOGGER.info("/{} sent the bot {}", command, messages);
        return messages;
    }

    // /commandlibtest value <type> sends the value its argument parsed.
    private static void receivesValue(BotSession client, String type, String input, String expected) {
        receives(client,
                 "commandlibtest value " + type + " " + input,
                 "the value of a " + type + " argument",
                 m -> m.contains("value " + type + "=" + expected));
    }

    private static GenericContainer<?> container(String image, String logPrefix, double cpuLimit) {
        return new GenericContainer<>(DockerImageName.parse(image)).withCreateContainerCmdModifier(cmd -> {
            if (cpuLimit <= 0) {
                return;
            }
            HostConfig hostConfig = cmd.getHostConfig() == null ? HostConfig.newHostConfig() : cmd.getHostConfig();
            cmd.withHostConfig(hostConfig.withNanoCPUs(Math.round(cpuLimit * 1_000_000_000L)));
        })
                                                                   .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(logPrefix));
    }

    // The names of the children of the root literal `literal` in the last command tree the bot received.
    private static List<String> commandTreeChildren(BotSession client,
                                                    String literal) throws ReflectiveOperationException {
        Object packet = null;
        for (Object received : client.receivedPackets.snapshot()) {
            if (received.getClass()
                        .getSimpleName()
                        .equals("ClientboundCommandsPacket")) {
                packet = received;
            }
        }
        if (packet == null) {
            fail("The bot received no command tree.");
        }
        Object nodes = packet.getClass()
                             .getMethod("getNodes")
                             .invoke(packet);
        int root = (int) packet.getClass()
                               .getMethod("getFirstNodeIndex")
                               .invoke(packet);
        for (int child : childIndices(nodes, root)) {
            if (literal.equals(nodeName(nodes, child))) {
                List<String> names = new ArrayList<>();
                for (int grandchild : childIndices(nodes, child)) {
                    names.add(nodeName(nodes, grandchild));
                }
                return names;
            }
        }
        return fail("/" + literal + " is not in the command tree the bot received.");
    }

    private static int[] childIndices(Object nodes, int index) throws ReflectiveOperationException {
        Object node = Array.get(nodes, index);
        return (int[]) node.getClass()
                           .getMethod("getChildIndices")
                           .invoke(node);
    }

    private static String nodeName(Object nodes, int index) throws ReflectiveOperationException {
        Object node = Array.get(nodes, index);
        return (String) node.getClass()
                            .getMethod("getName")
                            .invoke(node);
    }

    private static List<String> awaitSuggestions(BotSession client) throws ReflectiveOperationException {
        try {
            Awaitility.await()
                      .atMost(Duration.ofSeconds(30))
                      .pollInterval(Duration.ofMillis(100))
                      .until(() -> {
                          client.assertNotDisconnected();
                          return client.hasReceived("ClientboundCommandSuggestionsPacket");
                      });
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            fail("The bot received no suggestions.");
        }
        for (Object packet : client.receivedPackets.snapshot()) {
            if (packet.getClass()
                      .getSimpleName()
                      .equals("ClientboundCommandSuggestionsPacket")) {
                return Arrays.asList((String[]) packet.getClass()
                                                      .getMethod("getMatches")
                                                      .invoke(packet));
            }
        }
        throw new IllegalStateException("The suggestions packet disappeared.");
    }
}
