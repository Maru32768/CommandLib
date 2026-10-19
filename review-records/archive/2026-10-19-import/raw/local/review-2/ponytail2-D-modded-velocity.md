# ponytail2-D: modded / velocity / velocity-kotlin / gradle modded node scripts

Range: `2c8ffd7a..HEAD` (HEAD = master 02fbf5f9), extra attention on `16926819..HEAD`.
New commits in scope: 9366e526, da8f4660 (modded), 2df9241a (velocity-kotlin), 02fbf5f9 (shared RichTexts),
68657e57 (PlatformAdapter.hasPermission(command, ctx, prefix) removed from modded and velocity).
Assumed load: a mod or proxy plugin registers a handful of commands at startup. No hot paths here.

What this change does: Forge/NeoForge now build permission nodes from all of a registration's commands in one walk,
so a cooldown bypass node that names another command's node keeps that command's default. Modded
`CommandLib.register` now rejects an empty prefix and keeps a copy of the command list. The per-branch
`register(root)` / `onRegisterCommands` copies are merged into one. Paper and Velocity share one `RichTexts` source in
`common/src/adventure`.

## Findings

### P2D-1 (bug, low) Modded register does not validate command names until the dispatcher is built
- File: `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:85-100` (compare
  `paper/src/main/java/dev/marulabs/commandlib/CommandSet.java:26` and
  `velocity/src/main/java/dev/marulabs/commandlib/CommandLib.java:144`)
- Problem: da8f4660 says modded `register` now validates "as Spigot, Paper and Velocity do", but it only checks the
  prefix and nulls. Velocity builds the whole node tree inside `register`. Paper runs
  `CommandNameValidator.validateTopLevel` there. Modded builds nodes only in `register(root)`. That runs at
  `RegisterCommandsEvent` / `CommandRegistrationCallback`, or right away only when a server is already running. The
  normal case is to register from the mod constructor, before any server exists. Concrete case:
  `CommandLib.register("mymod.command", new Command("a"), new Command("a"))` or `new Command("-x")` (rejected since
  334695ec) returns normally. It writes `DEFAULTS`, adds itself to `REGISTRATIONS` and to the event bus. The
  `IllegalArgumentException` comes later, from the event handler during server start, and its stack trace does not
  point at the caller.
- Evidence: from reading the code only; not run on a server. `register` calls no `CommandNodeCreator` or validator
  before line 97, and the constructor builds nodes only when `server != null`.
- Fix: add one line before `defaults.applyTo(list)`: `CommandNameValidator.validateTopLevel(list);`. This matches
  Paper. The validator is package-private and in the same package. Building the whole tree eagerly, as Velocity does,
  would also catch child and argument errors. But it may need bootstrapped registries when the mod is constructed, so
  do not do that without a real-server check. Optional test in modded-testing: duplicate top-level names throw from
  `register`.

### P2D-2 (delete, low) velocity-testing still has the extra velocity-api compileOnly line that P1D-5 removed from velocity-kotlin
- File: `velocity-testing/build.gradle.kts:15` (outside my paths; same fix as 2df9241a)
- Problem: `api(project(":velocity"))` already exports velocity-api through velocity's `compileOnlyApi`. 2df9241a
  removed the same line from velocity-kotlin for exactly this reason.
- Evidence: from reading the build file. Line 14 is `api(project(":velocity"))`, line 15 is
  `compileOnly("com.velocitypowered:velocity-api:3.4.0")`. Keep the `testImplementation` of velocity-api on line 20:
  the test runtime still needs it.
- Fix: delete line 15. Saves 1 line.

## Checked, nothing to report
- 9366e526: `Command.permissionEntries(commands, prefix)` now takes the whole registration, on both the 1.18+
  Forge/NeoForge branch and the 1.16.5 branch. All three call sites use it: the constructor,
  `onGatherPermissionNodes` and `registerPermissionNodes`. The new test covers the 1.18+ path.
- da8f4660: The Stonecutter block boundaries are correct after the merge. `*///?}` now closes the 1.16.5 block before
  the shared `//? if !fabric` `onRegisterCommands` and the unconditional `register(root)`. Fabric's callback uses the
  shared `register(root)`. `List.copyOf` works on 1.16.5: its toolchain is Java 11 (`gradle/modded-node.gradle.kts:39-43`).
- 68657e57: The modded and velocity `PlatformAdapterImpl` keep only `hasPermission(source, node)`. No override of the
  removed method is left.
- 02fbf5f9: `common/src/adventure` is added to the paper and velocity main and test source sets, and to the paper
  integration plugin. The velocity integration target uses `project(":velocity")`, so it gets the file too. Paper
  (Java 21) and Velocity (Java 17) compile the same file, and nothing in it needs more than Java 17.
- 2df9241a: Correct. velocity's `compileOnlyApi` puts velocity-api on velocity-kotlin's compile classpath.
- Renamed vanilla classes: modded links `net.minecraft.commands.arguments.ColorArgument` (in ChatColorArgument) and
  other vanilla argument classes with no version condition. The `ColorArgument` -> `TeamColorArgument` rename came in
  26.2 (P1C-1). The newest modded node in `settings.gradle.kts` is 1.21.4, so every supported node still has these
  classes. Triage records that all 9 modded nodes compiled after the P1D fixes. A future 26.x node will need a
  Stonecutter branch here.

Verdict: fix P2D-1 (one line) if register should fail fast like Paper and Velocity. P2D-2 is a one-line cleanup.
Lean: -1 line possible.
Not checked: no Gradle build was run (the claim that all 9 modded nodes compile comes from the triage record), and
P2D-1's late failure was not reproduced on a real Forge server.
