# Testing Strategy

This document defines CommandLib's testing strategy. In particular, it covers how NMS / reflection code is tested and the role and policy of each test layer.

## Test Layer Overview

```
Layer 1: unit tests  (spigot:test, common:test)
Layer 2: spigot-testing (command-level unit tests with NMS mocks)
Layer 3: integration-test (Docker + real Minecraft servers)
```

Each layer can run independently. Layer 3 is opt-in (`COMMANDLIB_RUN_MINECRAFT_IT=true`).

---

## Layer 1: Unit Tests (spigot:test, common:test)

Scope: parser logic, command tree, options, permissions, help, validation.

- Pure logic tests that do not depend on Bukkit / NMS.
- Tests for the `common` module mostly live here.
- Keep Mockito stubs to a minimum.

---

## Layer 2: Command-Level Tests (spigot-testing)

Scope: argument parsing, validation, and suggestions, and the results of command execution.

### Basic Design

`CommandTester` fully stubs NMS.

```java
try (CommandTester tester = new CommandTester(command, "myplugin.cmd")) {
    FakeSender sender = FakeSender.player("Alice");
    tester.execute("heal Alice 10", sender);
    assertThat(sender.getSentMessages()).contains("Healed Alice for 10");
}
```

### How the NMS Stubs Work

The `CommandTester` constructor installs a static mock.

| Mock target | Effect |
|---|---|
| `NMSClassRegistry.findClass()` | Throws `UnsupportedOperationException` by default. Overridden per argument to return a mock implementation class |

Mock implementation classes pass `Object.class` as the wrapped server class.

The mock implementation classes swapped in by the `NMSClassRegistry` stub live in `spigot-testing`.
Each mock class extends the corresponding `NMSClass` subclass and implements argument parsing and value return without real NMS.

```
NMSArgumentPlayer (spigot/nms/argument)
  └─ MockNMSArgumentPlayer (spigot-testing/nms/argument)
       parse() → CommandTester.getFakeEntity(name)
```

### Steps for Adding a New Argument

1. Add `spigot/nms/argument/NMSArgumentXxx.java`.
2. Add `spigot-testing/nms/argument/MockNMSArgumentXxx.java`.
3. If it is a shared mock that is always used, add it to the default mock registrations in `CommandTester`. If it is only used by individual tests, register it with `CommandTester.builder().mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)`.
4. Add `spigot-testing/argument/XxxArgumentTest.java`, covering parse / suggestion / validation / error.

**When adding an argument, always create the mock and the test together.**

### CommandTester Extensibility: Issues and Policy

In addition to the default NMS mock registrations, `CommandTester` lets tests inject their own mocks through the builder.

```java
CommandTester tester = CommandTester.builder()
    .mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)
    .command(() -> new XxxCommand())
    .permissionPrefix("prefix")
    .build();
```

Keep the existing `Mockito.mockStatic` approach.
CommandLib does not need the "real NMS instances + `Bootstrap.bootStrap()` + Mockito spies" approach used by CommandAPI.
For test speed and less environment dependency, keep the full-stub approach based on mock implementation classes.

---

## Layer 3: Integration Tests (integration-test)

Scope: command registration, execution, tab completion, and permissions on real Minecraft servers.

### How It Works

- Testcontainers starts a Minecraft server inside a Docker container.
- MCProtocolLib connects over TCP and sends / receives commands over the real protocol.
- Each target (`integration-test/targets/<platform-version>/test-plugin/`) holds a test plugin and `server.jar`.
- The Gradle `IntegrationTarget` manages the Minecraft version, Java version, and MCProtocolLib version.

### Currently Supported Versions

| ID | MC version | Platform |
|---|---|---|
| 1165 | 1.16.5 | Spigot |
| 1165Mohist | 1.16.5 | Mohist |
| 1194 | 1.19.4 | Spigot |
| 1201 | 1.20.1 | Spigot |
| 1201Mohist | 1.20.1 | Mohist |
| 1204 | 1.20.4 | Spigot |
| 1205 | 1.20.5 | Spigot |
| 1206 | 1.20.6 | Spigot |
| 1210 | 1.21.0 | Spigot |

`IntegrationTarget` does not have a `platform` field yet (see the Roadmap Checklist in `testing-module-structure.md`).

### How to Run

```bash
# All versions
./gradlew :integration-test:minecraftIntegrationTest

# A specific version
./gradlew :integration-test:minecraftIntegrationTest1165

# Can also be controlled with an environment variable
COMMANDLIB_RUN_MINECRAFT_IT=true ./gradlew :integration-test:test
```

### When to Add Integration Tests

Add an integration test when any of the following applies:

- The parse result of an NMS argument may differ between versions.
- You want to verify the side effects of command registration / unregistration.
- You want to confirm that permission registration and reload work correctly.
- You want to guarantee interaction with `/execute`, command blocks, or functions.

Do not add an integration test if Layer 2 (spigot-testing) can verify the behavior.

---

## NMSClassRegistry and NMSClassResolver Design

### NMSClassRegistry

A registry that statically maps version ranges to classes.

```java
NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayerImpl_v1_20.class, "1.20", "1.20.4");
NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayerImpl_v1_20_5.class, "1.20.5", "");
```

At runtime, `BukkitUtil.getMinecraftVersion()` resolves the implementation class whose range matches.
In tests, it is replaced with `Mockito.mockStatic`.

### NMSClassResolver

Supports both the `net.minecraft.server.{version}` format and the unversioned packages of 1.20.5+.
Uses `Bukkit.getServer().getClass().getClassLoader()` to get the runtime class loader.

---

## Comparison with CommandAPI

CommandAPI uses a heavyweight testing strategy: "real NMS + Mockito spies + `Bootstrap.bootStrap()`".
CommandLib's current strategy (mock implementation classes + `Mockito.mockStatic`) is lighter:

- No runtime dependency on the Minecraft server jar.
- No registry initialization through `Bootstrap.bootStrap()`.
- Tests start faster.
- No need to split modules per version.

In exchange, the mock implementation classes have a maintenance cost.
Integration tests (Layer 3) guarantee real NMS behavior, so this division of work is reasonable.

---

## Future Direction

### Short Term (before P1 is complete)

- Design the `CommandTester` extensibility improvement (builder pattern).
- Add a `platform` field to `IntegrationTarget`.
- Follow this document's "create the mock and the test together" rule when adding new arguments.

### Medium Term (P2: CommandAPI parity)

- When implementing new arguments (NamespacedKey, Biome, Sound, Advancement, etc.), add the matching `MockNMSArgument*` and `ArgumentTest` at the same time.
- Add spigot-testing coverage for suggestions (sync / async) and tooltips.
- Add integration test coverage for `/execute`, command blocks, and functions.

### Long Term (P3 and later)

- **Paper module tests**: `paper-testing` verifies command-level behavior, Adventure components, suggestions / tooltips, and the fake paths for Paper `ArgumentTypes` / selectors / registry-backed arguments. Official Paper lifecycle registration is guaranteed by the real Paper server targets in `integration-test`, so `paper-testing` does not reproduce it.
- **JUnit extension**: let `CommandTester` be used declaratively with `@ExtendWith(CommandLibExtension.class)` + the `@RegisterCommand` annotation (for downstream users).
- **Forge / Fabric**: `NMSClassRegistry` is Bukkit-only. Forge / Fabric use Brigadier directly, so they need a separate test design (to be considered in P4).

---

## Roadmap Checklist

- `[x]` Set up `CommandTester` / `FakeSender` in `spigot-testing`.
- `[x]` Set up the NMS mock classes (arguments, command listener, world-related).
- `[ ]` Turn `CommandTester` into a builder so mock registrations can be injected from outside.
- `[ ]` Add a `platform` field to `IntegrationTarget`.
- `[ ]` Add spigot-testing coverage for suggestions / tooltips.
- `[ ]` Add integration tests for `/execute` and command blocks.
- `[x]` Start the `paper-testing` design (after the Paper module API is settled).
- `[ ]` Add a JUnit extension (for downstream users).
- `[x]` Add Forge testing. Real-server integration targets (Forge Targets in `integration-test/README.md`) and the serverless `forge-testing-<version>` have been added.
