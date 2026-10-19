# ponytail review 2-B: common runtime / messages / docs

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (HEAD = master 02fbf5f9), extra attention to `16926819..HEAD`.
Paths: common/src/main execution/runtime/messages/docs files (CommonCommand, CommonCommandContext, CommandRunner,
Cooldown, Confirmation, CommandDefaults, CommandFailure, Caption*, Help*, CommandDocs, CommandLibDiagnostics, Json,
PlatformAdapter*), command/, util/, resources, common/src/adventure, common-kotlin/src/main.

Load assumed: a Minecraft server or proxy, sync-thread dispatch, tens to hundreds of players.

What this change does: since the first pass, three commits touched these paths. 934cc3a1 folds CommandRunner's three
permission-denied exits into `denied(ctx)` (P1B-2). 68657e57 deletes `PlatformAdapter.hasPermission(command, ctx,
prefix)`; the help action now calls the two-argument check (P1B-1). 02fbf5f9 moves the Paper/Velocity `RichTexts`
Adventure converter and its test into `common/src/adventure` (+ `adventureTest`), added as a source dir by paper,
velocity and the paper integration-test plugins (P1D-2).

Checked for the new commits:
- 68657e57: every removed override did `hasPermission(<sender of ctx>, command.permissionName(prefix))`. For paper,
  velocity, modded and the test adapters the new path is the same call. For spigot it now goes through
  `hasPermission(Object, String)` with the NMS wrapper and the null-sender handling, which returns true only for
  function-compile sources (no help is shown there). Paper `getSender()` is `getSource().getSender()`, the same object
  the 2-arg path uses, so `/execute as` does not change the result. No remaining 3-arg implementation (grep).
- 934cc3a1: the helper keeps the same kind, exception, usage and default message. Behavior unchanged.
- 02fbf5f9: the shared file is byte-identical to both old copies except the comment. `RichText` has a
  package-private constructor and only two subclasses, so the cast in `toAdventure` stays safe. Spotless covers
  `common/src/**`. The velocity integration target uses `project(":velocity")`, so it gets the file. The paper
  integration plugins (including folia, which uses the paper module) add the dir.

Earlier triage items (A2-3/A-1, A2-7, A2-8, A-18, A-21, D-27, P1B-3) not re-reported.

## Must fix

None found.

## Should fix

### 1. delete | low | common/src/main/java/dev/marulabs/commandlib/PlatformAdapter.java:11, HelpMessageAction.java:15-24,121,210-213

- What this is: the type parameter `U extends CommonCommand<C, U>` on `PlatformAdapter`, and the `U` that
  `HelpMessageAction` carries to match it.
- Problem: 68657e57 deleted the only method that used `U`. Now no `PlatformAdapter` method mentions `U`, but every
  implementation (spigot, paper, velocity, modded, common-testing, common test) and every use site still has to name it
  (`PlatformAdapter<S, ?, C, ?>`, `PlatformAdapter<?, ?, ?, ?>`). In `HelpMessageAction`, `U` now exists only so
  `isUsable(U, ...)` can be called, which forces the unchecked `castCommand` helper. With
  `isUsable(CommonCommand<C, ?> child, C ctx)` the cast and `U` go away; `command` can be `CommonCommand<C, ?>`.
- Evidence: read `PlatformAdapter` in full after 68657e57 (methods: `createCommandContext`, `createSourceContext`,
  `hasPermission(S, String)`, `hasPermission(C, String)`, `convertCommandSyntaxException`), none uses `U`. A probe
  compile was not possible (unconfirmed by build).
- Fix: drop `U` from `PlatformAdapter` and `get()`; change `<?, ?, ?, ?>`/`<S, ?, C, ?>`/`<S, T, C, ?>`/`<S, T, C, U>`
  to three arguments in PlatformAdapterHolder, CommandRunner, OptionGraph, ArgumentCommandNodeCreator,
  CommandNodeCreator, the 6 implementations and `integration-test/shared/bukkit/.../CommandSyntaxExceptionTest.java:95`.
  In HelpMessageAction drop `U`, type `command` and `isUsable` with `CommonCommand<C, ?>`, delete `castCommand`.
  (`.jev-lint` fixture CommandRunner.java also names `PlatformAdapter<S, ?, C, ?>`; it is a lint fixture and need not
  compile.)
- Savings: about -8 lines, plus one unchecked cast and one generic parameter on a public interface removed. Mechanical,
  ~12 files.

## Nice to have

### 2. merge | low | common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:304-312 and HelpMessageAction.java:36-43

- What this is: two copies of "the options of a command this sender may use": `CommandRunner.hasUsableOption` (for the
  `[options]` tag in an argument-error usage) and the filter loop at the top of `HelpMessageAction.help` (for the help
  `[options]` tag and option list).
- Problem: the two must agree, or the error usage and the help show different `[options]` tags. They already drifted
  once (A2-9, then the A-2 regression about which command's options count). Keeping one copy prevents a third time.
- Evidence: read both; same loop over `owner.options()`, same `option.permissionName(owner, prefix)`, same
  `permission == null || hasPermission(ctx, permission)` test.
- Fix: make it one static in HelpMessageAction, e.g.
  `static <C ...> List<CommandOption<?, C>> usableOptions(PlatformAdapter<?, ?, C, ?> adapter, CommonCommand<C, ?> owner, String prefix, C ctx)`;
  `help` uses the list, CommandRunner passes `!usableOptions(...).isEmpty()` to `branchUsage` and deletes
  `hasUsableOption`.
- Savings: about -6 lines.

Verdict: Ship. Fix 1 when convenient; it is the tail end of P1B-1.
Lean: -14 lines possible.

Not checked: no build or test run. A throwaway probe compile of finding 1 was blocked by the sandbox, so finding 1 is
confirmed by reading only. Rest of the runtime (Cooldown, Confirmation, Json, Kotlin DSL) re-skimmed; the first pass
covered them in depth and nothing in the range changed them since.
