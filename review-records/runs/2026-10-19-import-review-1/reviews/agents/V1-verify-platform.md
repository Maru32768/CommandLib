# V1 — Adversarial verification: platform findings (A4-01..03, A5-01..04)

HEAD `1b8f17dc`, baseline `origin/master` `2c8ffd7a`. Static review. I checked platform behavior with `javap -c -p` on
cached jars: spigot-api 1.16.5 and 1.21.8, paper-api 1.21.8, the Paper dev-bundle server jar
(`applyDevBundlePatches_d14c…/output.jar`), the Loom Forge 1.16.5-36.2.42 merged Mojang and SRG jars and sources, the
Forge 1.20.1 SRG jar, and Fabric `intermediary-v2.tiny` 1.21.8. I ran no Gradle builds.

## A4-01 — CONFIRMED (P2, predates the range)

The premise holds: `PluginDisableEvent` fires before `onDisable()`.
- spigot-api 1.21.8 `JavaPluginLoader.disablePlugin`: offset 69 `callEvent(new PluginDisableEvent)`, then offset 87
  `JavaPlugin.setEnabled(false)`. spigot-api 1.16.5 has the same order (offsets 61 and 87).
- Paper server `PaperPluginInstanceManager.disablePlugin`: offset 80 `new PluginDisableEvent`, then offset 95
  `setEnabled(false)`. `cancelTasks` and `HandlerList.unregisterAll` run afterwards.
- `JavaPlugin.setEnabled` runs `putfield isEnabled` before it calls `onDisable()`, so `isEnabled()` is false inside
  `onDisable()`.
- `CraftScheduler.validate` checks `plugin.isEnabled()` and throws `IllegalPluginAccessException("Plugin attempted to
  register task while disabled")`.

So `onPluginDisable` (`spigot/.../CommandLib.java:650`) unregisters the instance first. A later public `unregister()`
(`:715`, then `:784-802`) reaches `updatePlayerCommandsLater()` → `runTask(plugin)` in its `finally` block, which throws.
Nothing guards against this. `origin/master` has the same `unregister(boolean)` (lines 644-662). The docs never
recommend calling `unregister()` in `onDisable()` (`docs/permissions.md:140` says disabling removes the commands), but
calling it there is natural, and the paper artifact returns early instead of throwing. No test covers it.

## A4-02 — CONFIRMED-WITH-CORRECTIONS (P3, predates the range)

The mechanism is right:
- `serverDispatcher` is written only by `register()`'s own COMMANDS handler (`paper/.../CommandLib.java:581`).
- Paper refuses late handlers: `PaperLifecycleEventManager.registerEventHandler` runs
  `checkState(registrationCheck, "Cannot register lifecycle event handlers")`, and `JavaPlugin` sets
  `allowsLifecycleRegistration=false` right after `onEnable()`.
- With a relocated copy that serves one plugin, the `Direct` branch (`:337-348`) and `reregisterDirectStates` can only
  take effect for a second plugin that shares an unrelocated copy.

Correction: late first registration is documented as unsupported. The class Javadoc (`:30`) says "Call register during
plugin initialization". The exception message says "Ensure at least one CommandLib.register() call is made during
plugin initialization". So this is dead code and misleading diagnostics text (`docs/diagnostics.md:154`), not a broken
contract. The call fails fast, so P3. The code is identical at `origin/master` (`:215`, `:218`). The only coverage is
the unrelocated mock test.

## A4-03 — CONFIRMED (P3, predates the range)

- Enable (`spigot/.../CommandLib.java:381-396`) runs `knownCommands.remove`, then `CommandNodeChildren.replace` on both
  roots. Nothing is saved.
- `removeRegisteredCommand` and `removeCommand` (`:968-976`, called from `:870-877`) remove entries by name only.
- Only `removeMinecraftNamespacedCommand` (`:942-953`) checks ownership (`wrappedVanillaCommand(command) != node`).

No doc describes a policy for label conflicts. `removeRegisteredCommand` is the same at `origin/master` (line 818).
There is no test for collisions.

## A5-01 — CONFIRMED (P1; P2 is defensible because Forge is "Best effort" in compatibility.md; predates the range)

From the Forge 1.16.5-36.2.42 merged jar:
- `MinecraftServer.getCommands()` returns `getfield resources` → `ServerResources.getCommands()`.
- `lambda$reloadResources$11` runs `resources.close()`, then `putfield resources` with the new `ServerResources`.
- `ServerResources.<init>` runs `new Commands` at offset 82.
- `Commands.<init>` calls `ForgeEventFactory.onCommandRegister` at offset 542. That method posts
  `RegisterCommandsEvent(dispatcher, environment)`; the class exists in Forge 36 with `getDispatcher()`.

The 1.16.5 branch (`modded/.../CommandLib.java:325-356`) adds the commands only on `FMLServerStartedEvent`, or right
away when a server already exists, and it then never subscribes. Startup functions load in
`ServerResources.loadResources`, before the server starts, so they cannot use the commands. `origin/master:150` uses the
same `FMLServerStartedEvent`. No modded integration test runs `/reload`. The Paper and Spigot reload tests do not cover
modded.

## A5-02 — CONFIRMED-WITH-CORRECTIONS (P2; the Fabric part is new in the range, the RCON part predates it)

- Fabric production: every intermediary class name in the 1.21.8 tiny file has the form `class_N`. The only exceptions
  are `Main` and `net/minecraft/server/MinecraftServer`, and no class name contains `CommandBlock`. Command-block sources
  therefore fall through to `CONSOLE`, `isConsole()` returns true, and `consoleRequirer()` / `requireConsole()` accept
  them. Confirmed.
- The RCON check: the 1.16.5 SRG runtime name is `net/minecraft/network/rcon/RConConsoleSource`, so `contains("RCon")`
  is correct there. The Forge 1.20.1 SRG jar and the Mojang jars name the class `net/minecraft/server/rcon/RconConsoleSource`,
  which the check misses.
- Correction: `isConsole()` treats `REMOTE_CONSOLE` like `CONSOLE`, so the RCON mismatch only changes what
  `getType()` reports. It does not affect the console checks.
- No test uses `COMMAND_BLOCK` or `REMOTE_CONSOLE`.

## A5-03 — CONFIRMED (P3 suggested; new in the range, because Fabric and CurrentServer are new)

On Fabric, `CurrentServer` (`modded/.../util/CurrentServer.java:24-27`) learns of the server only from a
`SERVER_STARTING` listener that its static initializer registers. A listener registered while that event is being
dispatched misses that occurrence. The commands are built when `Commands` is constructed, which happens before
`SERVER_STARTING`. So a first `register` at `SERVER_STARTING` or later gets `server == null`: only the callback is added
(`CommandLib.java:222-234`), and the commands appear after the next `/reload`. This breaks the diagnostics promise
("or the running server's dispatcher when registered later"). It is P3 rather than P2 because the README example
registers from `onInitialize`, where everything works. The tests mock `CurrentServer`.

## A5-04 — CONFIRMED, documented design (P2/P3 design concern; predates the range, Fabric copies it)

`PlatformAdapterImpl.java:27-37` returns `true` for any source whose entity is not a player. `resolveDefault` gives
`OP` players level 4 (`Command.java:96-104`). So a level-2 source (`/execute as <pig>`, or a command block) can run
commands whose default is `OP`, and even commands whose default is `NONE`. That is a real step up from level 2 to what
CommandLib treats as level 4.

The docs describe this behavior:
- `docs/errors.md:196`: "Allowed unless the source entity is a player".
- `docs/permissions.md:130-131`: "Non-player senders on Forge / NeoForge / Fabric … always pass the checks".

The `../../../../docs/errors.md` table has no Fabric row, a minor docs gap. No test pins the behavior.
