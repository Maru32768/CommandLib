# ponytail review 1-D: modded / velocity / velocity-kotlin / gradle modded node script

Range: `2c8ffd7a..16926819` (master at review time), paths `modded/**`, `velocity/**`, `velocity-kotlin/**`,
`gradle/modded-node.gradle.kts`.

Load assumed: one game server or proxy per JVM, a few dozen commands, tree sends to every online player on join and
refresh. Nothing here is hot except permission checks during command-tree sends.

What this change does: Velocity is a brand-new platform module (registration through `BrigadierCommand`, permission
defaults kept by CommandLib because the proxy has none, Adventure text, player/server arguments). The modded tree
gains Fabric as a third loader, a `CurrentServer` wrapper, an async scheduler, ~20 new vanilla-backed arguments,
diagnostics, annotation support, and a reworked actor/permission model.

Already decided in triage and not re-reported: R2C-3 (first-vs-last default, Velocity unregister keeps defaults),
R2C-4 (console/NONE), E-13 (command block level 4), R2C-2 (undeclared node on Forge 1.18+).

## Should fix

### D-1 bug, medium — Forge/NeoForge skip the A4-2 fix: a cooldown bypass node can still steal another command's node
- File: `modded/src/main/java/dev/marulabs/commandlib/Command.java:77-93` (`permissionEntries`, also the `<1.18`
  copy at :128-134), used by `CommandLib.java:290`, `:310-316` and `:374-379`.
- What this is: on Forge/NeoForge the permission nodes the server registers come from `Command.permissionEntries`,
  which calls `CommandTree.permissionConfigs(List.of(this), prefix)`, one command at a time.
- Problem: A4-2 (commit 20d89461) made `permissionConfigs` drop a cooldown bypass entry when another entry uses the
  same node, and made Spigot/Paper walk all commands at once so this holds across commands. Modded still walks per
  command, so the cross-command case is not caught. Case: command `kit` has `Cooldown.bypassPermission("p.vip")`,
  command `vip` (registered after it, `permission(ALL)`) has node `p.vip`. Forge 1.18+: `kit`'s bypass entry (OP,
  "Skips the cooldown of /kit") is first, `onGatherPermissionNodes` keeps the first name, so `p.vip` resolves to OP and
  guests cannot run `/vip`. Late registration (`PERMISSIONS.putIfAbsent`, :290) does the same. Forge <1.18
  `PermissionAPI.registerNode` keeps the last one, so the result there depends on the command order. Fabric and
  `docs()` use the whole-collection walk (`DEFAULTS`, :89), so docs say ALL while Forge enforces OP.
- Evidence: from reading only (unconfirmed by a run). `git show 20d89461 --stat` touches only common/spigot/paper.
- Fix: do what Spigot does (`spigot/.../Command.java:78`). Make `permissionEntries` static and give it the collection,
  `static List<PermissionNode<Boolean>> permissionEntries(Collection<? extends Command> commands, String prefix)`,
  and in `CommandLib` call it once with `commands`. Do the same in the `<1.18` branch. About 0 net lines. Add one case to
  `modded-testing/.../CommandLibRegistrationTest` in the same shape as
  `a_cooldown_bypass_node_is_registered_with_the_command`: register `kit` (bypass `t.b`) and then `b`
  (`permission(ALL)`), and assert `CommandLib.hasPermission(guestSource, guest, "t.b")` is true.
- If we skip it: the bug A4-2 fixed on Spigot/Paper stays on Forge/NeoForge. A command can quietly need OP because
  another command uses its node as a bypass node.

### D-2 simplify (merge), low — Paper and Velocity carry byte-identical `RichTexts` and `RichTextsTest`
- Files: `velocity/src/main/java/dev/marulabs/commandlib/util/text/RichTexts.java` (77 lines) and
  `velocity/src/test/.../RichTextsTest.java` (85 lines). They match `paper/...` except for one comment line each.
- Problem: they are two copies that must change together. The Adventure 5 click-event workaround was already copied
  into both by hand. The file itself says "A copy of paper's converter, since the modules share no source."
- Fix: use the trick the repo already uses for `common-kotlin/src/platform/kotlin`. Move the file and its test to one
  shared directory (for example `adventure/src/main/java` and `adventure/src/test/java`), and add `java.srcDir(...)`
  to the `sourceSets` of `paper` and `velocity`. This touches `paper/` too.
- Lean: about -160 lines.
- If we skip it: the next Adventure change has to be made twice.

## Nice to have

### D-3 safety (parity), low — modded `CommandLib.register` takes an empty prefix and keeps the caller's list
- File: `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:85-94`.
- Problem: Velocity (:134-142), Spigot and Paper null-check their inputs, reject `permissionPrefix.isEmpty()` and copy
  `commands`. Modded does none of this. If a caller passes `""`, registration works, but `docs()` and the whole
  `diagnostics()` report then throw from `CommandDocs.of`. The caller's mutable collection is also kept. Every
  `RegisterCommandsEvent` (each `/reload`) rebuilds from it, while `DEFAULTS` was fixed at register time.
- Fix: copy Velocity's four lines (`requireNonNull`, an empty check, `List<Command> list = List.copyOf(commands)`) and
  use `list` from then on.

### D-4 simplify, low — three identical `register(RootCommandNode)` bodies and two identical `onRegisterCommands`
- File: `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java:258-261, 326-329, 381-384` (and `:298-302`,
  `:362-366`).
- Fix: move `register(root)` out of the Stonecutter branches, where it compiles the same on every node. Keep one
  `onRegisterCommands` under `//? if !fabric`. Lean: about -12 lines.

### D-5 delete, low — `velocity-kotlin` repeats a compile dependency it already gets
- File: `velocity-kotlin/build.gradle.kts:24` `compileOnly("com.velocitypowered:velocity-api:3.4.0")`.
- Problem: `velocity` declares velocity-api as `compileOnlyApi`, so `api(project(":velocity"))` already puts it on
  velocity-kotlin's compile classpath. paper-kotlin needs its line because `paper` uses plain `compileOnly`, so this
  line was probably copied from there. Unconfirmed by a build.
- Fix: delete the line. Lean: -1 line.

Verdict: fix D-1 first. The rest are optional.
Lean: about -173 lines possible (D-2, D-4, D-5).
Not checked: nothing was compiled or run (no modded Gradle build). D-1 comes from reading the code. Velocity
`CommandManager` behavior (unregister by meta identity, overwriting on register) is from memory of Velocity 3.x.
