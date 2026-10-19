# Review round 2: C (modded / velocity / velocity-kotlin / common-kotlin)

Range: `2c8ffd7a..HEAD` (HEAD = `0ccd8a27`), with a focus on the round-1 fixes `6b898637..HEAD` in `modded/**`,
`velocity/**`, `velocity-kotlin/**`, `common-kotlin/src/main/**` and `gradle/modded-node.gradle.kts`.

Method: I reviewed the code by hand and applied the `code-review` recipe to the range. To compare behaviour across
platforms I also read the connected code: `CommandNodeCreator`, `Cooldown`, the Spigot and Paper `PlatformAdapterImpl`
and `CommandPermissions`, Spigot's `registerPermissions`, and the modded tests. I checked vanilla's behaviour by
disassembling `ServerFunctionLibrary` from the 1.20.1 and 1.21.1 Mojang-mapped jars in the Loom cache. I ran no
modded build. `:velocity:test :velocity-kotlin:test :common-kotlin:test` ran (result at the end).

Every Stonecutter branch changed in round 1 is fine on every node: `ForgeCommandActor`, `PlatformAdapterImpl`,
`CommandLib.refresh*`, `LocationArgument` and `CommandContext`. The unconditional imports (`MinecraftServer`,
`PlayerList`, `Vec2`) exist on 1.16.5 to 1.21.4, and the `>=1.18` / `<1.18` permission split still puts Fabric on the
`ServerPlayer` path. No caller of the allowLogging overloads or `sendComponent` is left in the code. The only mention
left is the historical "State before Phase 1" table in `docs/design/rich-text-messages.md`.

---

## R2C-1: regression, high: data pack functions can no longer compile CommandLib commands on Forge / NeoForge / Fabric

**Files:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:61-66, 97-101`,
`modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:193-206`

**What is wrong.** Round-1 fix f654f2ee (C-2) now treats only a `MinecraftServer` raw source as `CONSOLE`. Every other
source without an entity is `UNKNOWN`, and its permission comes from the node's default and the source's level.
Vanilla compiles data pack functions with this source:

```
new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, functionCompilationLevel, "", EMPTY, null, null)
```

The level is `function-permission-level`, which defaults to 2. While the function is parsed, the `requires` of every
CommandLib node calls `PlatformAdapterImpl.hasPermission` → `ForgeCommandActor.hasPermission`. For this source,
`getType()` returns `UNKNOWN`, so `hasDefaultPermission` runs. An `OP` node needs level 4 and a `NONE` node always
fails. Root commands default to `OP`. A function that calls a CommandLib command with an `OP` or `NONE` node therefore
fails to load ("Unknown or incomplete command"). This affects every modded node, both at startup and on `/reload`.
Before round 1, `CommandSource.NULL` fell through to `CONSOLE`, which passes every check.

The docs still promise the old behaviour:

- `docs/errors.md:196`: data pack functions are "Always allowed".
- `docs/permissions.md:130`: data pack functions "pass every check".
- `CHANGELOG.md:81`: "The console, RCON and data pack functions still pass every check".

Spigot (`isCraftBukkitNullSender`, and the `UnsupportedOperationException` catch) and Paper (`NullCommandSender`)
explicitly let the function compiler's source pass. Modded is now the only platform that does not.

**Evidence.** Confirmed by reading the code and the bytecode. I did not run it.

- `javap -c net/minecraft/server/ServerFunctionLibrary.class` (1.20.1 and 1.21.1 merged Mojang jars) shows
  `getstatic CommandSource.NULL` … `getfield functionCompilationLevel` … `CommandSourceStack.<init>` with a null level,
  server and entity.
- `ForgeCommandActorTest.another_source_is_unknown_and_judged_by_its_permission_level` builds exactly this kind of
  source, `source(CommandSource.NULL, level)` with a null server and entity, and asserts that it is denied at level 0.
- No modded test compiles a function. Spigot and Paper have `DataPackFunctionTest` in the integration fixtures, and
  `integration-test/shared/modded` has none, so nothing caught the regression.

**Suggested fix.** In `ForgeCommandActor.hasPermission` (or `getType`), let the function compiler's source pass, as
Spigot and Paper do. Two conditions identify it: the raw source is `CommandSource.NULL` (an identity check survives
remapping), and `source.getServer() == null`. Every source that executes a command has a server, so a chat-bridge mod
that runs commands as `CommandSource.NULL` with a server and level 0 stays denied, which keeps the purpose of C-2.

The test helper in `ForgeCommandActorTest` must then give the "guest" source a server mock. Add a test that compiles a
function line, or at least checks `source(NULL, 2)` without a server against an `OP` node. Add a modded
`DataPackFunctionTest` to the integration mod.

---

## R2C-2: bug, medium: on Forge / NeoForge 1.18+, `actor.hasPermission` of an undeclared node ignores the permission handler, so `Cooldown.bypassPermission` cannot be granted

**Files:** `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:331-335` (Forge ≥1.18 `hasPermission`),
`common/src/main/java/dev/marulabs/commandlib/Cooldown.java:63-66, 185`

**What is wrong.** On Forge / NeoForge 1.18+, a player's check looks up `PERMISSIONS.get("commandlib." + node)`. A node
that no command declared has no `PermissionNode`, so the check returns `source.hasPermission(4)`, and LuckPerms or any
other handler is never asked. `Cooldown.bypassPermission("myplugin.cooldown.bypass")`, the form in `README.md:1073`
and the skill's user guide, checks exactly such a node through `actor.hasPermission`. The node is also not part of
`permissionConfigs`, so it is never registered in `PermissionGatherEvent.Nodes` either.

The other platforms ask the handler for any node:

- Spigot / Paper: `CommandSender#hasPermission`.
- Velocity: `getPermissionValue`.
- Fabric: `Permissions.check` with a level-4 fallback.
- Forge 1.16.5: `PermissionAPI.hasPermission(player, String)`.

So on Forge / NeoForge 1.18+ a granted bypass node does nothing, and only level-4 operators bypass the cooldown. The
same applies to any `ctx.getActor().hasPermission("custom.node")` in user code.

**Evidence.** Confirmed by reading the code. I did not run it. Neither `Cooldown` nor `CommonCommand` adds the bypass
node to `permissionConfigs` (`CommonCommand.java:926-937`).

**Suggested fix.** Add the cooldown's bypass node to the command's `permissionConfigs`, with default `OP`, so that
every platform registers it: a Bukkit `Permission`, a Forge `PermissionNode`, the Velocity and modded defaults, and the
permission export. At minimum, state the limit in `docs/permissions.md`: Forge 1.18+ can only answer nodes that
CommandLib registered.

---

## R2C-3: spec-question, low: the policy for one node declared with two defaults differs by platform, and on Velocity a re-registration keeps the stale default

**Files:** `velocity/src/main/java/dev/marulabs/commandlib/VelocityPermissions.java:22-28`,
`velocity/src/main/java/dev/marulabs/commandlib/CommandLib.java:149, 181-185`,
`modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:89-90`, compared with
`paper/src/main/java/dev/marulabs/commandlib/CommandPermissions.java:23-26, 48-56` and
`spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java:817-829`

**What is wrong.**

- After 7be577e9, Velocity keeps the first default, as modded does. Spigot and Paper keep the last one:
  `removePermissionCompletely` + `addPermission`, `CommandPermissions.add` replaces, and `BOOTSTRAP_DEFAULTS.put`. The
  new Javadoc reasons that "a later one cannot open an `OP` node to everyone", but on Spigot and Paper a later
  registration can.
- On Velocity, `unregister()` still never removes defaults (the pending half of C-6). Combined with `putIfAbsent`,
  this creates a new failure. A plugin that rebuilds its commands on a config reload (`unregister()` then `register()`
  with a changed default, for example `OP` → `ALL`) now keeps the old default until the proxy restarts. Before the fix,
  the re-registration overwrote it.
- None of the docs state any of these policies. `docs/permissions.md` "Velocity" only says the defaults are kept.

**Evidence.** Confirmed by reading the code.

**Suggested fix.** Pick one policy for all platforms and document it in `docs/permissions.md`. For Velocity, resolve
the pending C-6 part in the same change: store the defaults per registration, remove a registration's defaults in
`unregister()`, and resolve a node from the remaining live registrations.

---

## R2C-4: spec-question, low: the console differs across platforms for `NONE` nodes and for `requireConsole`

**Files:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:70-72, 97-100`,
`modded/src/main/java/dev/marulabs/commandlib/Command.java:53-65`,
`velocity/src/main/java/dev/marulabs/commandlib/VelocityPermissions.java:34-40`,
`spigot|paper/src/main/java/dev/marulabs/commandlib/Command.java:36-43`

**What is wrong.**

- **`NONE` nodes.** On modded and Velocity, the console passes `NONE` nodes. On modded, `isConsole()` returns `true`;
  on Velocity, the console's `getPermissionValue` is TRUE. On Spigot and Paper, the console fails them, because Bukkit's
  `PermissionDefault.FALSE` applies to an operator too. `docs/permissions.md:92` defines `NONE` as "No one, operators
  included". The exception for the modded and Velocity console is only implied, in lines 129-131 and in the
  `docs/errors.md:196` table.
- **`requireConsole`.** Modded's `consoleRequirer` accepts RCON (`isConsole()` includes `REMOTE_CONSOLE`). On Spigot
  and Paper it requires `ConsoleCommandSender`, which `RemoteConsoleCommandSender` is not, so RCON is rejected there.

**Evidence.** Confirmed by reading the code.

**Suggested fix.** Decide whether these are intended. If so, state them in the per-platform table of
`docs/permissions.md` (a "console" row) and in the `requireConsole` Javadoc. If not, align them: for example, make
modded's `consoleRequirer` accept only `CONSOLE`, or make the Bukkit requirers accept `RemoteConsoleCommandSender`.

---

## R2C-5: doc-wrong, low: the CHANGELOG misses the removed modded `allowLogging` overloads and the actor changes

**Files:** `CHANGELOG.md:55-70, 79-81`

**What is wrong.**

- 0.17.2's Forge `CommandContext` had `sendMessage(String|ITextComponent, boolean allowLogging)`,
  `sendWarn(String, boolean)` and `sendFailure(String, boolean)`. c9cad70a removed them with no CHANGELOG entry. Calls
  now fail to compile and the CHANGELOG offers no replacement: `sendSuccess(..., true)` to broadcast.
- f654f2ee changed what users can observe, and the CHANGELOG does not say so:
  - `ctx.getActor().getType()` is now `UNKNOWN`, not `CONSOLE`, for sources other than the server, RCON and command
    blocks. Such sources no longer pass `requireConsole()`.
  - `ctx.getActor().hasPermission(..)` is now `true` for the console. It was `false` for every non-player.
- Line 81 also needs the R2C-1 correction.

**Evidence.** `git show 0.17.2:forge/src/main/java/net/kunmc/lab/commandlib/CommandContext.java`, and
`grep allowLogging CHANGELOG.md` finds nothing.

**Suggested fix.** Add the entries under "Messages" and the modded permission bullet.

---

## Checks with no finding

- **Removal of `sendComponent`.** Common, Velocity and modded are consistent, and the Kotlin DSL does not reference
  it.
- **Removal of `allowLogging`.** The bridge is now a private `sendFeedback(Component, boolean broadcastToOps)`.
  `sendWarn` and `sendFailure` behave the same as before with `false`. The design table was updated.
- **`refresh` / `refreshAll`.** `player.getServer()` exists on every node. The null check of the player list covers
  Fabric's `SERVER_STARTING`, which fires on the server thread, so `runOnServerThread` runs inline. All three
  constructors now share `refreshAll()`.
- **`LocationArgument`.** The rotation now comes from `CommandSourceStack.getRotation()`, with yaw = `y` and
  pitch = `x`, which matches `CommandContext.getLocation()`.
- **Velocity `testerDefaults`.** The seam is set and cleared only by velocity-testing. Production behaviour is the
  same when it is empty.
- **`common-kotlin`.** It uses none of the APIs removed in round 1 (`addPreprocess(Consumer)`, the `CommandOption`
  constructor, the branch permission overloads that took a parent).

Build: `:velocity:test :velocity-kotlin:test :common-kotlin:test` pass.

Not checked: modded-testing and velocity-testing internals (area E), and real-server behaviour. R2C-1 and R2C-2 are
not reproduced on a server.
