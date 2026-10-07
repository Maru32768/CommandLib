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

Typed NMS modules run BuildTools on the first build of each Spigot version and
set up the paperweight dev bundle (see `docs/agents/nms-build.md`):

```bash
./gradlew :nms:spigot-1.16.5:assemble :nms:spigot-1.20.4:assemble :nms:paper-1.20.6:assemble
```

Pass `-Pcommandlib.typedNms=false` to leave them out when the change does not
touch NMS.

Forge / NeoForge changes must compile every node, because code for nodes
other than the active one is only type-checked when that node compiles (see
`docs/agents/modded.md`):

```bash
./gradlew :modded:forge-1.16.5:compileJava :modded:forge-1.20.1:compileJava :modded:neoforge-1.21.1:compileJava
./gradlew :modded-testing:forge-1.16.5:test :modded-testing:forge-1.20.1:test :modded-testing:neoforge-1.21.1:test
```

Integration tests may require Docker or a local Minecraft server environment.
Run them only when the touched behavior requires it or the user asks for it:

```bash
./gradlew :integration-test:minecraftIntegrationTest
```

When adding or fixing command behavior, prefer narrow tests around parsing,
execution, options, permissions, and suggestions. Use `spigot-testing` utilities
for command-level Bukkit tests when a full server is not needed.
