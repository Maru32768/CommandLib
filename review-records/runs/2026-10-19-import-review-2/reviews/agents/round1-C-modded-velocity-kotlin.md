# Review round 1 — C: modded / velocity / velocity-kotlin / common-kotlin

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (HEAD = `6b898637`), restricted to `modded/**`,
`velocity/**`, `velocity-kotlin/**`, `common-kotlin/**`, `gradle/modded-node.gradle.kts`, `settings.gradle.kts`.

Method: the `code-review` skill does not take a commit range (it reviewed `HEAD~3..HEAD`, integration-test only,
which is outside this area), so the diff was reviewed by hand, along with the connected common code
(`CommandNodeCreator`, `CommonCommandContext`, `Cooldown`, `PlatformAdapter`) and the docs
(`docs/permissions.md`, `docs/packaging.md`, `docs/compatibility.md`). No Gradle build was run. Every finding comes
from reading the code; none needed a compile to show.

---

## C-1 — bug — medium

**File:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:234-247` (compare
`modded/src/main/java/dev/marulabs/commandlib/PlatformAdapterImpl.java:329-345`)

**Problem.** In this range, `PlatformAdapterImpl.hasPermission(CommandSourceStack, node)` changed. The console, RCON
and functions now pass every check, and other non-player sources (command blocks, `/execute as <entity>`) are
judged by the node's `DefaultPermission` and their permission level. `ForgeCommandActor.hasPermission` was not
changed, and it still returns `false` for every source that is not a `ServerPlayer`.

So `ctx.getActor().hasPermission(node)` and CommandLib's own permission check give different answers for the same
source and node:

- console / RCON / function: the command check says `true`, the actor says `false`;
- command block running an `ALL`-default node: the command check says `true`, the actor says `false`.

`docs/permissions.md:130` says the console "pass[es] every check". Spigot/Paper (`sender.hasPermission`) and
Velocity (`VelocityPermissions`, where the console gets TRUE) agree with their own command check. Only modded
disagrees.

**Failure scenario.** A mod calls `if (ctx.getActor().hasPermission("mymod.command.admin")) ...` inside a command to
gate a sub-feature. It works for players but is always denied from the server console. `Cooldown` with a bypass
permission (`common/.../Cooldown.java:185`) uses `actor.hasPermission`, so an entity source (`/execute as @e`, which
has a UUID) never bypasses, although the same node passes the command check.

**Fix.** Make `PlatformAdapterImpl.hasPermission(CommandSourceStack, String)` a static helper, or move its body into
one, and have `ForgeCommandActor.hasPermission` delegate to it. This also removes the duplicate
`>=1.18` / `<1.18` permission branches in the two classes.

---

## C-2 — bug — medium

**File:** `modded/src/main/java/dev/marulabs/commandlib/ForgeCommandActor.java:198-217`, used by
`PlatformAdapterImpl.java:340-343`

**Problem.** `getType()` returns `CONSOLE` for every source that has no entity and is not a `BaseCommandBlock` or an
`RconConsoleSource`. That includes `CommandSource.NULL`, other mods' custom `CommandSource`s, and a `null` raw source
when the reflective field lookup fails on Fabric, Forge 1.16.5 or Forge 1.18.2. The new `PlatformAdapterImpl` then
grants such sources every CommandLib permission (`isConsole() -> return true`) and ignores their permission level.
The fallback is open where it should be closed.

**Failure scenario.** A chat-bridge or scripting mod runs commands for remote users. It uses
`new CommandSourceStack(CommandSource.NULL or its own source, ..., permissionLevel 0, ...)`. That source passes every
`OP`- and `NONE`-default CommandLib node and can run admin commands. Vanilla commands would refuse it on permission
level. The case was not reproduced. It follows directly from the code.

**Fix.** Treat only `rawSource instanceof MinecraftServer` (console, functions) as `CONSOLE`, and `RconConsoleSource`
as `REMOTE_CONSOLE`. Return `CommandActorType.UNKNOWN` for anything else, including a failed lookup, so
`hasDefaultPermission` decides by the source's level. `instanceof` survives remapping, as the existing
`BaseCommandBlock` check already relies on.

---

## C-3 — doc — low

**File:** `docs/agents/modded.md:131-132`

**Problem.** The doc says "no target installs fabric-permissions-api or runs a command block". The shared modded test
mod runs a command block on every modded target, Fabric included: `ActorTest.commandBlockActorType()` and
`PermissionTest` (`ActorTest.commandBlock(..., 5)` with an armor stand probe). `docs/compatibility.md:41` correctly
says Fabric checks the command block and `/execute as` armor stand. `docs/roadmap.md:35` and
`docs/roadmap-history.md:120` also still list "command block" as unverified or out of scope.

**Fix.** Change the line to "no target installs fabric-permissions-api", and remove "command block" from the Fabric
row of roadmap/roadmap-history.

---

## C-4 — backcompat / improvement — low

**File:** `modded/src/main/java/dev/marulabs/commandlib/CommandContext.java:89-91, 140-142, 151-153, 160-179`

**Problem.** In this range, `sendSuccess(String, boolean)` moved to common under the name `broadcastToOps`, and the
modded `sendSuccess(Component, boolean)` was renamed with it. The modded-only overloads `sendMessage(String, boolean
allowLogging)`, `sendWarn(String|Component, boolean allowLogging)` and `sendFailure(String|Component, boolean
allowLogging)` still use the old `allowLogging` name. Spigot, Paper and Velocity have no equivalents. Nothing in the
repo calls them with `true`: no samples, examples, tests or docs, apart from the design table in
`docs/design/rich-text-messages.md:182`.

`sendFailure(..., true)` also changes the meaning silently. It routes through `sendSuccess`, so the message is
suppressed by the `sendCommandFeedback` gamerule and is no longer a vanilla failure.

**Fix.** Delete the boolean overloads of `sendMessage(String, …)`, `sendWarn` and `sendFailure`. Keep
`sendMessage(Component, boolean)` only as the internal bridge, or rename its parameter to `broadcastToOps`. Update the
design table.

---

## C-5 — improvement — low

**File:** `modded/src/main/java/dev/marulabs/commandlib/argument/LocationArgument.java:38-52` vs
`modded/src/main/java/dev/marulabs/commandlib/CommandContext.java:45-51`

**Problem.** In this range, `CommandContext.getLocation()` changed to read the rotation from
`CommandSourceStack.getRotation()`, so it follows `/execute rotated` and the console gets the source's rotation.
`LocationArgument` still copies the entity's `getYRot()`/`getXRot()`. Its Javadoc says "with the sender's rotation".
Under `/execute rotated ...` or `/execute as <entity> rotated as <other>`, the two APIs return different yaw/pitch for
the same execution.

**Fix.** In `parseImpl`, use `Vec2 r = ctx.getSender().getRotation(); new Location(world, x, y, z, r.y, r.x)`. That
also removes the `>=1.17` field/getter Stonecutter branch.

---

## C-6 — spec-question — low

**File:** `velocity/src/main/java/dev/marulabs/commandlib/VelocityPermissions.java:128-130`,
`velocity/src/main/java/dev/marulabs/commandlib/CommandLib.java:209-213`

**Problem.** Within one copy of CommandLib, Velocity stores defaults with `DEFAULTS.put`, so the last registration
that declares a node decides its default. Modded uses `putIfAbsent` ("The first command that declares a node decides
it"). `unregister()` also never removes the defaults it added, so a node of an unregistered command keeps granting
`ALL` to anyone who asks for it.

**Fix.** Use `putIfAbsent` to match modded. Optionally, drop the registration's defaults in `unregister()` when no
other live registration declares them.

---

## Checks with no finding

- **Backward-compatibility leftovers.** No `@Deprecated`, aliases, or "kept for compatibility" shims in the area.
  `BlockPosArgument` was renamed to `BlockPositionArgument` with no alias. The old
  `PlatformAdapter.hasPermission(Command, CommandSourceStack, String)` was removed. The reflective
  `findLanguage` was removed. The Forge 1.16.5 `permission(DefaultPermissionLevel …)` overloads are documented
  platform overloads (parallel to Spigot's `permission(PermissionDefault)`) for a supported version, so they are
  legitimate.
- **Stonecutter branches.** Every import and code branch that was read puts the loader first (`fabric` /
  `neoforge`, then `forge && >=x` or `elif !fabric`), as `docs/agents/modded.md` requires: `CommandLib`, `Command`,
  `ForgeCommandActor`, `CurrentServer`, `CommandContext.getLanguage`, `RawWordArgumentType`, `AnnotatedCommands`,
  `BuiltInCommandBuildContext`, `Advancement/Recipe/LootTable/Attribute/EntityType/ScoreboardDisplaySlot`. No branch
  references a class that only exists on the active `forge-1.20.1` node. `BuiltInCommandBuildContext` has no class on
  `<1.19`, and every user of it is gated `>=1.19` / `>=1.19.3`.
- **Fabric.** `Permissions.check` is only resolved lazily behind `isModLoaded`, and the integration targets run
  without fabric-permissions-api, so the missing class is exercised. `fabric.mod.json` is excluded on Forge and
  NeoForge.
- **Velocity.** Aliases each become a `BrigadierCommand`, built from copied nodes, not redirects. The console gets
  TRUE from `getPermissionValue`. The scheduler runs sync tasks inline, which is fine on the proxy.
- **Kotlin DSL parity.** `argument` (1–7 typed and vararg), `require`, `subcommand` on commands, branches and
  required branches, `executeSuspend` for all three executors, `addMiddlewareSuspend`, suggestions, options, flags
  and `provide`/`get`/`put` cover the Java builder surface. There is no modded Kotlin artifact, and that is
  documented (`docs/kotlin.md:78`, `docs/compatibility.md:91`).
