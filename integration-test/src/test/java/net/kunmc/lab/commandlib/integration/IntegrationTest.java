package net.kunmc.lab.commandlib.integration;

import org.awaitility.Awaitility;
import com.github.dockerjava.api.model.HostConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * This test keeps a real server process in the loop and only automates the
 * manual "start server, join player, inspect result file" workflow.</p>
 */
class IntegrationTest {
    private static final String TEST_PLAYER_NAME = "Maru32768";
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    @Test
    void commandlib_test_plugin_succeeds_on_target_version_with_a_real_player() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("commandlib.runMinecraftIntegration"),
                               "Minecraft integration test is disabled. Enable an integration test task.");
        assertDockerAvailable();

        String targetName = System.getProperty("commandlib.targetName");
        String reportFileName = System.getProperty("commandlib.reportFileName",
                                                   "TEST-commandlib-" + targetName + ".xml");
        String serverJarName = System.getProperty("commandlib.serverJarName", "server.jar");
        String serverLaunchArgs = System.getProperty("commandlib.serverLaunchArgs", "-jar " + serverJarName);
        String dockerImageName = System.getProperty("commandlib.dockerImageName");
        String serverDirectory = System.getProperty("commandlib.serverDirectory", "server");
        String testPluginSubdirectory = System.getProperty("commandlib.testPluginSubdirectory", "plugins");
        String testPluginJarPattern = System.getProperty("commandlib.testPluginJarPattern", "TestPlugin.*\\.jar");

        Path testPluginDir = Path.of(System.getProperty("commandlib.testPluginDir"));
        Path testPluginJar = findTestPluginJar(testPluginDir.resolve(serverDirectory)
                                                            .resolve(testPluginSubdirectory), testPluginJarPattern);
        Path sharedDir = Path.of(System.getProperty("commandlib.sharedDir"));
        Path reportDir = sharedDir.resolve("test-results");
        Path reportFile = reportDir.resolve(reportFileName);
        double containerCpuLimit = Double.parseDouble(System.getProperty("commandlib.containerCpuLimit", "2.0"));
        Files.createDirectories(reportDir);
        Files.deleteIfExists(reportFile);

        try (GenericContainer<?> container = createContainer(targetName,
                                                             dockerImageName,
                                                             testPluginJar,
                                                             testPluginSubdirectory,
                                                             reportDir,
                                                             serverLaunchArgs,
                                                             reportFileName,
                                                             containerCpuLimit)) {
            try {
                container.start();
            } catch (Exception e) {
                assertPluginEnabled(container);
                throw e;
            }
            assertPluginEnabled(container);

            BotSession client = connectBot(container);
            try {
                Awaitility.await()
                          .atMost(Duration.ofSeconds(30))
                          .until(() -> {
                              client.assertNotDisconnected();
                              return client.isConnected();
                          });

                waitForPlayerJoin(container, client);
                waitForStableConnection(client, Duration.ofSeconds(3));

                // Send a tab-complete request before runTests so the server-side suggestion action
                // fires and captures getLatestInput() before verifySuggestionCapture runs.
                client.sendTabCompleteRequest(1, "/commandlibtest suggestionCapture abc");
                Thread.sleep(300);
                client.assertNotDisconnected();

                client.sendCommand("commandlibtest runTests");

                Awaitility.await()
                          .atMost(Duration.ofMinutes(2))
                          .pollInterval(Duration.ofSeconds(1))
                          .until(() -> {
                              client.assertNotDisconnected();
                              return Files.exists(reportFile) && Files.size(reportFile) > 0;
                          });
                client.assertNotDisconnected();

                assertJUnitReportSucceeded(reportFile);

                // Bot-side checks need the messages a player receives, which the console-driven report cannot see.
                // Help message prefix: the usage must start with the typed command, not a token like an argument name.
                assertBotReceives(client,
                                  "commandlibtest helpMessageRoot",
                                  "help usage prefix",
                                  m -> m.contains("commandlibtest helpMessageRoot"));
                // Invalid argument: the translated "incorrect argument" message points at the rejected token.
                assertBotReceives(client,
                                  "commandlibtest enumArgument not_a_material",
                                  "incorrect argument message",
                                  m -> m.contains("command.unknown.argument") || m.contains("Incorrect argument"));
                // CommandSyntaxException thrown by an argument is converted into a failure message for the sender.
                assertBotReceives(client,
                                  "commandlibtest testConvertCommandSyntaxException true",
                                  "converted CommandSyntaxException message",
                                  m -> m.contains("argument.integer.big") || m.contains("Integer must not be more than"));
                // A node with PermissionDefault.FALSE is not usable even by an operator.
                List<String> deniedMessages = assertBotReceives(client,
                                                                "commandlibtest deniedProbe",
                                                                "unknown command message for a node without permission",
                                                                // The parent command is usable, so Brigadier reports the
                                                                // hidden child as an incorrect argument.
                                                                m -> m.contains("command.unknown.command") || m.contains(
                                                                        "command.unknown.argument") || m.contains(
                                                                        "Unknown or incomplete command") || m.contains(
                                                                        "Incorrect argument") || m.contains(
                                                                        "Unknown command"));
                assertThat(deniedMessages).noneMatch(m -> m.contains("deniedProbe executed"));
            } finally {
                client.disconnect("Test completed");
            }
        }
    }

    @Test
    void player_join_log_detection_accepts_and_mohist_formats() {
        assertThat(isPlayerJoinLog("[12:47:42 INFO]: Maru32768 joined the game")).isTrue();
        assertThat(isPlayerJoinLog(
                "[12:47:42 INFO]: Maru32768[/172.17.0.1:52648] logged in with entity id 127 at (-245.5, 63, 153.5)")).isTrue();
        assertThat(isPlayerJoinLog("[12:47:42 INFO]: OtherPlayer joined the game")).isFalse();
    }

    private static List<String> assertBotReceives(BotSession client,
                                                  String command,
                                                  String description,
                                                  java.util.function.Predicate<String> expected) {
        client.clearReceivedPackets();
        client.sendCommand(command);
        List<String> received = new ArrayList<>();
        try {
            Awaitility.await()
                      .atMost(Duration.ofSeconds(10))
                      .pollInterval(Duration.ofMillis(200))
                      .until(() -> {
                          client.assertNotDisconnected();
                          received.addAll(client.drainSystemMessages());
                          return received.stream()
                                         .anyMatch(expected);
                      });
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            fail("Bot did not receive " + description + " for /" + command + ". Received: " + received);
        }
        return received;
    }

    private static void waitForPlayerJoin(GenericContainer<?> container, BotSession client) {
        Awaitility.await()
                  .atMost(Duration.ofSeconds(30))
                  .pollInterval(Duration.ofMillis(500))
                  .until(() -> {
                      client.assertNotDisconnected();
                      return container.getLogs()
                                      .lines()
                                      .anyMatch(IntegrationTest::isPlayerJoinLog);
                  });
    }

    private static boolean isPlayerJoinLog(String line) {
        if (!line.contains(TEST_PLAYER_NAME)) {
            return false;
        }

        return line.contains("joined the game") || line.contains("logged in with entity id");
    }

    private static void waitForStableConnection(BotSession client, Duration duration) {
        long deadline = System.nanoTime() + duration.toNanos();
        while (System.nanoTime() < deadline) {
            client.assertNotDisconnected();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException("Interrupted while waiting for the MCProtocolLib session to stabilize.",
                                                e);
            }
        }
        client.assertNotDisconnected();
    }

    private static void assertJUnitReportSucceeded(Path reportFile) throws ParserConfigurationException, IOException, SAXException {
        Document report = DocumentBuilderFactory.newInstance()
                                                .newDocumentBuilder()
                                                .parse(reportFile.toFile());
        Element testSuite = report.getDocumentElement();
        int tests = Integer.parseInt(testSuite.getAttribute("tests"));
        int failures = Integer.parseInt(testSuite.getAttribute("failures"));
        int errors = Integer.parseInt(testSuite.getAttribute("errors"));

        assertThat(tests).isPositive();
        assertThat(failures).as("JUnit XML failure count in " + reportFile)
                            .isZero();
        assertThat(errors).as("JUnit XML error count in " + reportFile)
                          .isZero();
    }

    private static void assertPluginEnabled(GenericContainer<?> container) {
        boolean failed = container.getLogs()
                                  .lines()
                                  .anyMatch(line -> line.contains("COMMANDLIB_TEST_PLUGIN_ENABLE_FAILED"));
        if (failed) {
            fail("TestPlugin failed to enable. See server logs above for the stack trace.");
        }
    }

    private static void assertDockerAvailable() {
        try {
            DockerClientFactory.instance()
                               .client();
        } catch (Throwable t) {
            // Fail instead of skipping: these tasks are explicitly requested integration tests, and a missing
            // Docker daemon should be visible as infrastructure failure in CI and local verification.
            fail("Docker is required for IntegrationTest but is not available: " + t.getMessage(), t);
        }
    }

    private static Path findTestPluginJar(Path pluginsDir, String jarPattern) throws IOException {
        try (var files = Files.list(pluginsDir)) {
            return files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName()
                                            .toString()
                                            .matches(jarPattern))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Test plugin jar was not found in " + pluginsDir));
        }
    }

    private static GenericContainer<?> createContainer(String targetName,
                                                       String dockerImageName,
                                                       Path testPluginJar,
                                                       String testPluginSubdirectory,
                                                       Path reportDir,
                                                       String serverLaunchArgs,
                                                       String reportFileName,
                                                       double containerCpuLimit) {
        String containerWorkDir = "/workspace/server";
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + TEST_PLAYER_NAME).getBytes(StandardCharsets.UTF_8));
        String opsJson = "[{\"uuid\":\"" + offlineUuid + "\",\"name\":\"" + TEST_PLAYER_NAME + "\",\"level\":4,\"bypassesPlayerLimit\":false}]";

        return new GenericContainer<>(DockerImageName.parse(dockerImageName)).withExposedPorts(25565)
                                                                             .withCopyFileToContainer(MountableFile.forHostPath(
                                                                                                              testPluginJar),
                                                                                                      containerWorkDir + "/" + testPluginSubdirectory + "/" + testPluginJar.getFileName())
                                                                             .withFileSystemBind(reportDir.toAbsolutePath()
                                                                                                          .toString(),
                                                                                                 "/workspace/test-results",
                                                                                                 BindMode.READ_WRITE)
                                                                             .withCreateContainerCmdModifier(cmd -> {
                                                                                 if (containerCpuLimit <= 0) {
                                                                                     return;
                                                                                 }
                                                                                 HostConfig hostConfig = cmd.getHostConfig();
                                                                                 if (hostConfig == null) {
                                                                                     hostConfig = HostConfig.newHostConfig();
                                                                                 }
                                                                                 hostConfig.withNanoCPUs(Math.round(
                                                                                         containerCpuLimit * 1_000_000_000L));
                                                                                 cmd.withHostConfig(hostConfig);
                                                                             })
                                                                             .withWorkingDirectory(containerWorkDir)
                                                                             .withLogConsumer(new Slf4jLogConsumer(
                                                                                     LOGGER).withPrefix(targetName))
                                                                             .withCommand("sh",
                                                                                          "-lc",
                                                                                          "printf 'eula=true\n' > eula.txt && "
                                                                                                  // Write ops.json before the server starts so the player has operator permission on join.
                                                                                                  // Offline UUID is deterministic: UUID.nameUUIDFromBytes("OfflinePlayer:<name>").
                                                                                                  + "printf '" + opsJson + "' > ops.json && " + "java -Dplugin.env=CI -Dcommandlib.testReportName=" + reportFileName + " " + "-Dcommandlib.testReportDir=/workspace/test-results " + serverLaunchArgs + " nogui")
                                                                             .waitingFor(Wait.forLogMessage(
                                                                                     ".*Done \\(.*\\)! For help, type \"help\".*",
                                                                                     1))
                                                                             .withStartupTimeout(Duration.ofMinutes(8));
    }

    private static BotSession connectBot(GenericContainer<?> container) {
        try {
            return connectWithLegacyProtocol(container);
        } catch (ReflectiveOperationException ignored) {
            try {
                return connectWithModernProtocol(container);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create an MCProtocolLib client session.", e);
            }
        }
    }

    private static BotSession connectWithLegacyProtocol(GenericContainer<?> container) throws ReflectiveOperationException {
        Class<?> protocolClass = Class.forName("com.github.steveice10.mc.protocol.MinecraftProtocol");

        Object protocol = protocolClass.getConstructor(String.class)
                                       .newInstance(TEST_PLAYER_NAME);
        Object session = createLegacySession(container, protocolClass, protocol);
        Class<?> sessionClass = session.getClass();
        sessionClass.getMethod("connect", boolean.class)
                    .invoke(session, false);
        return createBotSession(session, sessionClass);
    }

    private static Object createLegacySession(GenericContainer<?> container,
                                              Class<?> protocolClass,
                                              Object protocol) throws ReflectiveOperationException {
        Class<?> sessionClass = Class.forName("com.github.steveice10.packetlib.tcp.TcpClientSession");
        Constructor<?> constructor = findNullableSessionConstructor(sessionClass, protocolClass, 3);
        if (constructor != null) {
            return constructor.newInstance(container.getHost(), container.getMappedPort(25565), protocol);
        }

        // Older PacketLib versions create sessions through Client + TcpSessionFactory instead of exposing a
        // direct TcpClientSession(host, port, protocol) constructor.
        Class<?> sessionFactoryClass = Class.forName("com.github.steveice10.packetlib.tcp.TcpSessionFactory");
        Object sessionFactory = sessionFactoryClass.getConstructor()
                                                   .newInstance();
        Class<?> clientClass = Class.forName("com.github.steveice10.packetlib.Client");
        Constructor<?> clientConstructor = findClientConstructor(clientClass, protocolClass, sessionFactoryClass);
        Object client = clientConstructor.newInstance(container.getHost(),
                                                      container.getMappedPort(25565),
                                                      protocol,
                                                      sessionFactory);
        return clientClass.getMethod("getSession")
                          .invoke(client);
    }

    private static BotSession connectWithModernProtocol(GenericContainer<?> container) throws ReflectiveOperationException {
        Class<?> protocolClass = Class.forName("org.geysermc.mcprotocollib.protocol.MinecraftProtocol");
        Object protocol = protocolClass.getConstructor(String.class)
                                       .newInstance(TEST_PLAYER_NAME);

        Class<?> sessionClass = Class.forName("org.geysermc.mcprotocollib.network.tcp.TcpClientSession");
        Constructor<?> constructor = findSessionConstructor(sessionClass, protocolClass);
        Object session = constructor.newInstance(container.getHost(), container.getMappedPort(25565), protocol);
        sessionClass.getMethod("connect", boolean.class)
                    .invoke(session, false);
        return createBotSession(session, sessionClass);
    }

    private static Constructor<?> findSessionConstructor(Class<?> sessionClass, Class<?> protocolClass) {
        Constructor<?> constructor = findNullableSessionConstructor(sessionClass, protocolClass, 3);
        if (constructor != null) {
            return constructor;
        }
        throw new IllegalStateException("Unable to find a compatible TcpClientSession constructor for " + sessionClass.getName());
    }

    private static Constructor<?> findNullableSessionConstructor(Class<?> sessionClass,
                                                                 Class<?> protocolClass,
                                                                 int parameterCount) {
        for (Constructor<?> constructor : sessionClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != parameterCount) {
                continue;
            }
            if (parameterTypes[0] != String.class || parameterTypes[1] != int.class) {
                continue;
            }
            // MCProtocolLib changed concrete protocol types between artifacts; assignability lets the same
            // test code work across those incompatible releases.
            if (parameterTypes[2].isAssignableFrom(protocolClass)) {
                return constructor;
            }
        }
        return null;
    }

    private static Constructor<?> findClientConstructor(Class<?> clientClass,
                                                        Class<?> protocolClass,
                                                        Class<?> sessionFactoryClass) {
        for (Constructor<?> constructor : clientClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 4) {
                continue;
            }
            if (parameterTypes[0] != String.class || parameterTypes[1] != int.class) {
                continue;
            }
            if (parameterTypes[2].isAssignableFrom(protocolClass) && parameterTypes[3].isAssignableFrom(
                    sessionFactoryClass)) {
                return constructor;
            }
        }
        throw new IllegalStateException("Unable to find a compatible PacketLib Client constructor for " + clientClass.getName());
    }

    private static BotSession createBotSession(Object session,
                                               Class<?> sessionClass) throws ReflectiveOperationException {
        Method isConnected = sessionClass.getMethod("isConnected");
        Method send = findSendMethod(sessionClass);
        Method disconnect = findDisconnectMethod(sessionClass);
        BotSession botSession = new BotSession(session, isConnected, send, disconnect);
        registerPacketListener(session, sessionClass, botSession);
        return botSession;
    }

    private static void registerPacketListener(Object session,
                                               Class<?> sessionClass,
                                               BotSession botSession) throws ReflectiveOperationException {
        // Find addListener(SessionListener) and determine the listener interface from its parameter type.
        Method addListener = null;
        Class<?> listenerInterface = null;
        for (Method m : sessionClass.getMethods()) {
            if (m.getName()
                 .equals("addListener") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInterface()) {
                addListener = m;
                listenerInterface = m.getParameterTypes()[0];
                break;
            }
        }
        if (addListener == null) {
            throw new IllegalStateException("No addListener(SessionListener) method found on " + sessionClass.getName());
        }

        Object proxy = Proxy.newProxyInstance(listenerInterface.getClassLoader(),
                                              new Class<?>[]{listenerInterface},
                                              (p, method, args) -> {
                                                  if (method.getName()
                                                            .equals("packetReceived") && args != null) {
                                                      if (args.length == 2) {
                                                          // Modern MCProtocolLib: packetReceived(Session, Packet)
                                                          botSession.receivedPackets.add(args[1]);
                                                      } else if (args.length == 1) {
                                                          // Legacy MCProtocolLib (1.16.5): packetReceived(PacketReceivedEvent)
                                                          try {
                                                              Object packet = args[0].getClass()
                                                                                     .getMethod("getPacket")
                                                                                     .invoke(args[0]);
                                                              if (packet != null) {
                                                                  botSession.receivedPackets.add(packet);
                                                              }
                                                          } catch (ReflectiveOperationException e) {
                                                              throw new IllegalStateException(
                                                                      "packetReceived event has no getPacket(): " + args[0].getClass()
                                                                                                                           .getName(),
                                                                      e);
                                                          }
                                                      } else {
                                                          throw new IllegalStateException(
                                                                  "Unexpected packetReceived signature: " + args.length + " arguments");
                                                      }
                                                  }
                                                  if (isDisconnectCallback(method.getName())) {
                                                      botSession.disconnectionReason.compareAndSet(null,
                                                                                                   describeDisconnectEvent(
                                                                                                           args));
                                                  }
                                                  return null;
                                              });
        addListener.invoke(session, proxy);
    }

    private static boolean isDisconnectCallback(String methodName) {
        return methodName.equals("disconnected") || methodName.equals("disconnecting");
    }

    private static String describeDisconnectEvent(Object[] args) {
        if (args == null || args.length == 0) {
            return "MCProtocolLib session disconnected.";
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            for (String methodName : List.of("getReason", "getCause", "getMessage")) {
                try {
                    Object value = arg.getClass()
                                      .getMethod(methodName)
                                      .invoke(arg);
                    if (value != null) {
                        return "MCProtocolLib session disconnected: " + value;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return "MCProtocolLib session disconnected: " + Arrays.toString(args);
    }

    private static Method findSendMethod(Class<?> sessionClass) {
        for (Method method : sessionClass.getMethods()) {
            if (method.getName()
                      .equals("send") && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("Unable to find a compatible send(Packet) method for " + sessionClass.getName());
    }

    private static Method findDisconnectMethod(Class<?> sessionClass) throws NoSuchMethodException {
        try {
            return sessionClass.getMethod("disconnect", String.class);
        } catch (NoSuchMethodException ignored) {
            return sessionClass.getMethod("disconnect", String.class, Throwable.class);
        }
    }

    private static final class BotSession {
        private final Object delegate;
        private final Method isConnectedMethod;
        private final Method sendMethod;
        private final Method disconnectMethod;
        final CopyOnWriteArrayList<Object> receivedPackets = new CopyOnWriteArrayList<>();
        final AtomicReference<String> disconnectionReason = new AtomicReference<>();
        private volatile boolean connectedOnce = false;

        private BotSession(Object delegate, Method isConnectedMethod, Method sendMethod, Method disconnectMethod) {
            this.delegate = delegate;
            this.isConnectedMethod = isConnectedMethod;
            this.sendMethod = sendMethod;
            this.disconnectMethod = disconnectMethod;
        }

        void clearReceivedPackets() {
            receivedPackets.clear();
        }

        List<String> drainSystemMessages() throws ReflectiveOperationException {
            List<Object> snapshot = new ArrayList<>(receivedPackets);
            receivedPackets.clear();
            List<String> messages = new ArrayList<>();
            for (Object packet : snapshot) {
                String className = packet.getClass()
                                         .getName();
                // 1.19+ uses SystemChatPacket, 1.17/1.18 use ClientboundChatPacket and 1.16.5 uses ServerChatPacket.
                if (!className.contains("SystemChatPacket") && !className.contains("ServerChatPacket") && !className.endsWith(
                        "ClientboundChatPacket")) {
                    continue;
                }
                // Try getContent() (1.19+) then getMessage() (1.16.5)
                Object component = null;
                for (String methodName : List.of("getContent", "getMessage")) {
                    try {
                        component = packet.getClass()
                                          .getMethod(methodName)
                                          .invoke(packet);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                if (component == null) {
                    throw new IllegalStateException("Chat packet " + className + " has neither getContent() nor getMessage().");
                }
                messages.add(serializeComponent(component));
            }
            return messages;
        }

        private static String serializeComponent(Object component) {
            try {
                // Reflective call to GsonComponentSerializer.gson().serialize(component)
                // to avoid compile-time Adventure version coupling with MCProtocolLib's bundled Adventure.
                Class<?> serializerClass = Class.forName(
                        "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
                Object serializer = serializerClass.getMethod("gson")
                                                   .invoke(null);
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                return (String) serializerClass.getMethod("serialize", componentClass)
                                               .invoke(serializer, component);
            } catch (Exception e) {
                return component.toString();
            }
        }

        boolean isConnected() {
            try {
                boolean connected = (boolean) isConnectedMethod.invoke(delegate);
                if (connected) {
                    connectedOnce = true;
                }
                return connected;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to query MCProtocolLib session state.", e);
            }
        }

        void assertNotDisconnected() {
            String reason = disconnectionReason.get();
            if (reason != null) {
                fail(reason);
            }
            if (connectedOnce && !isConnected()) {
                fail(reason != null ? reason : "MCProtocolLib session disconnected before the integration test completed.");
            }
        }

        void sendCommand(String command) {
            try {
                sendMethod.invoke(delegate, createCommandPacket(command));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to send MCProtocolLib command: " + command, e);
            }
        }

        void sendTabCompleteRequest(int transactionId, String text) {
            try {
                sendMethod.invoke(delegate, createTabCompletePacket(transactionId, text));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to send tab complete request: " + text, e);
            }
        }

        private static Object createTabCompletePacket(int transactionId,
                                                      String text) throws ReflectiveOperationException {
            // Try each known class name across MCProtocolLib versions.
            for (String className : List.of(
                    "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket",
                    "com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket",
                    "com.github.steveice10.mc.protocol.packet.ingame.client.ClientTabCompletePacket")) {
                try {
                    return Class.forName(className)
                                .getConstructor(int.class, String.class)
                                .newInstance(transactionId, text);
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                }
            }
            throw new IllegalStateException("No tab complete packet class found for the MCProtocolLib version in use.");
        }

        private Object createCommandPacket(String command) throws ReflectiveOperationException {
            String normalizedCommand = command.startsWith("/") ? command.substring(1) : command;
            Object modernCommandPacket = tryCreateStringPacket(
                    "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket",
                    normalizedCommand);
            if (modernCommandPacket != null) {
                return modernCommandPacket;
            }

            Object signedCommandPacket = tryCreateSignedCommandPacket(
                    "com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket",
                    normalizedCommand);
            if (signedCommandPacket != null) {
                return signedCommandPacket;
            }

            // Minecraft 1.17 and 1.18 send commands as chat text through the renamed serverbound chat packet.
            Object chatPacket = tryCreateStringPacket(
                    "com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket",
                    "/" + normalizedCommand);
            if (chatPacket != null) {
                return chatPacket;
            }

            // Minecraft 1.16.5 predates the dedicated command packet. Commands are sent as chat text.
            return Class.forName("com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket")
                        .getConstructor(String.class)
                        .newInstance("/" + normalizedCommand);
        }

        private Object tryCreateStringPacket(String className, String command) throws ReflectiveOperationException {
            try {
                return Class.forName(className)
                            .getConstructor(String.class)
                            .newInstance(command);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                return null;
            }
        }

        private Object tryCreateSignedCommandPacket(String className,
                                                    String command) throws ReflectiveOperationException {
            Class<?> packetClass;
            try {
                packetClass = Class.forName(className);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
            // The signed command packet changed its constructor between 1.19.x releases. An unsigned command only
            // needs the command text, so every other parameter gets an empty default.
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length < 2 || parameterTypes[0] != String.class) {
                    continue;
                }
                Object[] arguments = new Object[parameterTypes.length];
                arguments[0] = command;
                boolean timestampSet = false;
                for (int i = 1; i < parameterTypes.length; i++) {
                    Class<?> type = parameterTypes[i];
                    if (type == long.class) {
                        arguments[i] = timestampSet ? 0L : System.currentTimeMillis();
                        timestampSet = true;
                    } else if (type == int.class) {
                        arguments[i] = 0;
                    } else if (type == boolean.class) {
                        arguments[i] = false;
                    } else if (type == BitSet.class) {
                        arguments[i] = new BitSet();
                    } else if (java.util.List.class.isAssignableFrom(type)) {
                        arguments[i] = Collections.emptyList();
                    } else {
                        arguments[i] = null;
                    }
                }
                return constructor.newInstance(arguments);
            }
            return null;
        }

        void disconnect(String reason) {
            try {
                if (disconnectMethod.getParameterCount() == 1) {
                    disconnectMethod.invoke(delegate, reason);
                } else {
                    disconnectMethod.invoke(delegate, reason, null);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to disconnect MCProtocolLib session.", e);
            }
        }
    }
}
