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
class HealCommandTest {
    @Test
    void heal_sends_message() {
        try (CommandTester tester = new CommandTester(HealCommand::new, "myplugin.cmd")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("heal Alice 10", sender);
            assertThat(sender.getSentMessageTexts()).contains("Healed Alice for 10");
        }
    }
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
class XxxCommandTest {
    @Test
    void xxx_test() {
        try (CommandTester tester = CommandTester.builder()
                                                 .mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)
                                                 .command(() -> new XxxCommand())
                                                 .permissionPrefix("prefix")
                                                 .build()) {
            // ...
        }
    }
}
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

## NMSClassRegistry Design

`NMSClassRegistry` finds the implementation of each NMS wrapper in the typed NMS modules bundled into the spigot jar
(see `docs/agents/nms-build.md`). The generated `TypedNmsModules` lists each module's package and the Minecraft
versions its server jar matches, and each module's generated `TypedNmsClasses` lists its classes. A class implements
the wrapper it extends.

```java
final class TypedNmsModules {
    static final List<NMSClassRegistry.TypedModule> MODULES = List.of(
            new NMSClassRegistry.TypedModule("spigot_1_16_5", "1.16.4", "1.16.5"),
            // ...
            new NMSClassRegistry.TypedModule("spigot_1_20_6", "1.20.5", "1.20.6"));
}
```

At runtime, `NMSClassRegistry.findClass()` uses `BukkitUtil.getMinecraftVersion()` to pick the implementation from the
modules whose range matches. In tests, it is replaced with `Mockito.mockStatic`.

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
- `[x]` Add a JUnit extension (for downstream users). See "Downstream Test Helpers" below.
- `[x]` Add Forge testing. Real-server integration targets (Forge Targets in `integration-test/README.md`) and the serverless `forge-testing-<version>` have been added.

---

## Downstream Test Helpers

`common-testing`, `spigot-testing`, `paper-testing`, `modded-testing` (`forge-testing-<version>`,
`neoforge-testing-<version>`, `fabric-testing-<version>`) and `velocity-testing` add three helpers on top of
`CommandTester` / `FakeSender`.
`docs/testing.md` documents their use.

- **`CommandLibExtension`**: a JUnit 5 `ParameterResolver` registered with `@RegisterExtension` and a
  `Supplier<CommandTester>`. It creates the tester lazily in the store of the method's `ExtensionContext`, so a
  `@BeforeEach` method and its test share one tester and the store closes it after the test. `@Sender` configures
  injected `FakeSender`s; outside `common-testing` an injected player is registered with the tester
  (`withFakePlayer`, or `withPlayer` on modded). A field with a factory was chosen over `@ExtendWith` plus a
  `@RegisterCommand` annotation, because commands often take constructor arguments and the Spigot tester must build them while its NMS mocks are active. The artifacts declare
  `junit-jupiter-api` as `compileOnly`, so they add no test framework to downstream classpaths.
- **`CommandTester#suggest`**: returns the suggestion texts, so tests assert on a `List<String>` with their own
  assertion library. No CommandLib assertion classes are provided; AssertJ (or any list assertion) already covers
  messages (`getSentMessageTexts()`), results (`execute` returns an `int`) and suggestions.
- **Command tree snapshots**: `CommandTreeRenderer` in `common` renders the CommandLib command model (not the
  Brigadier tree, whose option permutations would bury the structure) with aliases, permission nodes, default
  permissions, options and argument branches. `Snapshots` in `common` compares text with
  `src/test/resources/snapshots/<name>`, writes a missing snapshot and fails, and rewrites snapshots when
  `COMMANDLIB_UPDATE_SNAPSHOTS=true` or `-Dcommandlib.updateSnapshots=true` is set. Both are package-private; the
  testing artifacts reach them from the same package, as they do `CommandNodeCreator`. The help snapshot tests of
  `common` use the same `Snapshots`.

The testing artifacts each have their own copy of `CommandLibExtension` and `@Sender`, because each has its own
`CommandTester` and `FakeSender` with the same names, and one test classpath holds only one of them.
