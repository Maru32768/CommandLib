# Testing Notes

Use focused Gradle tasks for the modules affected by the change.

Common checks:

```bash
./gradlew :common:test
./gradlew :spigot:test
./gradlew :spigot-testing:test
```

Compile checks:

```bash
./gradlew :common:compileJava
./gradlew :spigot:compileJava
./gradlew :spigot-testing:compileJava
```

Integration tests may require Docker or a local Minecraft server environment.
Run them only when the touched behavior requires it or the user asks for it:

```bash
./gradlew :integration-test:minecraftIntegrationTest
```

When adding or fixing command behavior, prefer narrow tests around parsing,
execution, options, permissions, and suggestions. Use `spigot-testing` utilities
for command-level Bukkit tests when a full server is not needed.
