# ponytail review 1-B: common runtime / messages / docs

Range: `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD` (HEAD = master 16926819).
Paths: common/src/main execution/runtime/messages/docs files (CommonCommand, CommonCommandContext,
ChainedCommandContext, CurrentCommandContext, CommandRunner, CommandScheduler, Cooldown, Confirmation, ContextKey,
CommandDefaults, CommandActor*, CommandError, CommandFailure, ErrorFormatter, Caption, Captions, CaptionSender,
CommandHelp, HelpFormatter, DefaultHelpFormatter, HelpMessageAction, UsageFormatter, CommandDocs,
CommandLibDiagnostics, Json, Snapshots, PermissionConfig, DefaultPermission, PlatformAdapter*,
FunctionCompilationMarker, FunctionLoadErrorFilter), command/, util/, common/src/main/resources, common-kotlin/src/main.

Load assumed: a Minecraft server, sync-thread command dispatch, tens to hundreds of players. Per-sender maps
(Cooldown, Confirmation) are pruned on use, so nothing grows without bound.

What this change does: it adds the execution runtime: middlewares, async executors, cooldown, confirmation, context
values (provide/put/get), captions and localization, a typed help/usage/error formatter model, Markdown/JSON docs and
diagnostics, plus a Kotlin DSL with coroutine support. CommandRunner drives each execution: checks, parse, middleware
chain on the sync thread, then failure capture and reporting.

Items already decided in triage.md (A2-3/A-1 confirm in functions, A2-7 getInput index, A2-8 handler inheritance,
A-18 parent options before child literal, D-27 cooldown message during reservation, A-21 addPreprocess) were seen
again and are not reported.

## Must fix

None found.

## Should fix

### 1. delete | low | common/src/main/java/dev/marulabs/commandlib/PlatformAdapter.java:38 (+ 6 impls)

- What this is: `PlatformAdapter.hasPermission(U command, C ctx, String permissionPrefix)`.
- Problem: it has one caller, `HelpMessageAction.isUsable` (HelpMessageAction.java:122). Every implementation
  (spigot, paper, velocity, modded, common-testing, common test) only does
  `hasPermission(<sender>, command.permissionName(permissionPrefix))`, which the existing default
  `hasPermission(C ctx, String node)` already does. Spigot's copy even differs from its 2-arg method: it calls
  `ctx.getSender().hasPermission(...)` directly and skips the null-sender handling of `hasPermission(Object, String)`.
  Nothing hits that difference today, but two paths for one check is how such gaps get in.
- Evidence: grep for `hasPermission(\w+, \w+, permissionPrefix)` finds only HelpMessageAction.java:122; read all
  six implementations (confirmed by reading, not by a build).
- Fix: in HelpMessageAction.isUsable use `platformAdapter.hasPermission(ctx, child.permissionName(permissionPrefix))`.
  Delete the interface method and the six overrides.
- Savings: about -25 lines over 7 files.

## Nice to have

### 2. simplify | low | common/src/main/java/dev/marulabs/commandlib/CommandRunner.java:111-115, 124-128, 149-153

- What this is: the three permission-denied exits in `runInContext`.
- Problem: the same 5-line call
  `fail(ctx, CommandError.Kind.PERMISSION_DENIED, null, null, () -> ctx.sendFailure(Caption.PERMISSION_DENIED))`
  appears three times.
- Fix: add `private int denied(C ctx) throws CommandFailure` and call `return denied(ctx);` at each site.
- Savings: about -8 lines.

### 3. simplify | low | common/src/main/java/dev/marulabs/commandlib/Captions.java:105-116 and Caption.java:159-169

- What this is: two copies of "open stream, read UTF-8 properties, wrap IOException".
- Problem: `Captions.bundle` repeats `Caption.load` except for where the stream comes from.
- Fix: give `Caption.load` an `InputStream`-supplying overload (or a `read(InputStream, String resource)` helper) and
  call it from both. Keep `Caption.class.getResourceAsStream` with the relative path for the built-in bundle, so shading
  relocation still works.
- Savings: about -8 lines. Skip it if nobody is touching these files anyway.

Verdict: Ship. Fix 1 when convenient.
Lean: -40 lines possible.

Checked: CommandRunner flow (sync/async completion, late failure reporting, CancellationException/TimeoutException,
formatter fallback, the replayed AssertionError path), captureFailure/deliver, Cooldown reserve/start/release ordering
and pruning, Confirmation pruning and forked siblings, provider recursion guard, Captions lookup order and the
translatable fallback (en/ja keys match Caption.builtIns), help/usage building and coloring, CommandDocs/Json output,
FunctionLoadErrorFilter warm-up, and the Kotlin dispatcher queue (covered by KotlinDslTest, including 200k yields).

Not checked: no build or test run (review only). Real-server behavior of the Kotlin dispatchers was not checked:
`ctx.syncDispatcher`/`asyncDispatcher` return a new instance on each access, so `withContext(ctx.syncDispatcher)`
from code already on the sync dispatcher never takes kotlinx's same-dispatcher fast path and waits a tick. It is
correct, only slower, so it is not a finding.
