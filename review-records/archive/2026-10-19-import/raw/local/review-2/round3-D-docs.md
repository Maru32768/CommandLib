# Round 3 D: docs / README / CHANGELOG / examples / samples / skill

Scope: docs/** (except docs/agents, roadmap*), README.md, CHANGELOG.md, examples/**, samples/**, plugins/** skill,
checked against code at master 3db5ca99, with focus on round-2 commits (0ccd8a27..HEAD).

Verified accurate (no finding): getOption throwing for undeclared options and repeatable() after registration
(docs/options.md); annotation inheritance - superclass/interface methods, @Inherited class annotations, generic
override rule (docs/annotations.md vs AnnotationParser#methods/hierarchy/overrideOf); @Range default validation
including @Optional 0 and @Option without @Default (AnnotationParser#checkDefault, Options.integer); wildcard List
params (no doc claims the opposite); cooldown-bypass node (CommandTree, kind `cooldown-bypass`, children not listed,
README/permissions/diagnostics/skill); Forge 1.18+ unregistered-node limit (CommandLib#hasPermission falls back to
level 4); function-compilation source on modded (ForgeCommandActor#isFunctionCompilation); FakeSender grant/deny
semantics per artifact, common-testing returning copies, velocity-testing UNDEFINED/ALL rule; CommandLibExtension
refusals (@BeforeAll/constructor in all 5 artifacts, duplicate player names in all but common-testing); selector
behaviour of spigot/paper/modded testers (arguments.md table); modded mock overworld; Folia rules in
docs/execution.md; AssertionError / silent ErrorFormatter (docs/errors.md, help.md, testing.md vs
CommandFailure.replayed). No example/sample uses an out-of-range default, repeatable after option(), or a
case-sensitive/@a-excluding expectation.

## D3-1 doc-wrong, medium - CHANGELOG.md:84-91 (modded Permissions bullets) misstate the change from 0.17.2

What is wrong: both bullets describe round-1 changes to unreleased code, not changes from 0.17.2.
- "a command block or a non-player entity ... no longer passes every check ... The console, RCON and data pack
  functions still pass every check": in 0.17.2 Forge, `PlatformAdapterImpl#hasPermission` was
  `commandSource.hasPermissionLevel(command.permissionLevel())` (default level 4) for every source. Command blocks never
  passed every check, and data pack functions did not either: they were judged by the function permission level, so a
  default (level 4) command in a function failed to load. Fabric and NeoForge did not exist in 0.17.2.
- "`ctx.getActor().getType()` is `UNKNOWN`, not `CONSOLE` ... `hasPermission(...)` is now `true` ... it was `false`":
  `getActor()` does not exist in 0.17.2 (`git grep getActor 0.17.2` finds nothing). The Added section already lists it
  as new.

What really changes for a 0.17.2 Forge user: levels 1-3 are gone. `OP` needs level 4 and `ALL` lets everyone in, so a
command that 0.17.2 opened to command blocks with `permissionLevel(2)` can no longer be limited to command blocks and
operators. Data pack functions now pass every node check (the function permission level still applies to vanilla's
own checks).

Evidence: `git show 0.17.2:forge/src/main/java/net/kunmc/lab/commandlib/PlatformAdapterImpl.java` lines 22-31;
`git grep -n getActor 0.17.2` is empty.

Suggested fix: replace the two bullets with one Forge bullet: "`permissionLevel(int)` levels 1-3 are gone: `OP` needs
level 4, so a command block (level 2) can run only `ALL` commands, including those 0.17.2 opened with
`permissionLevel(2)`. Data pack functions now pass every CommandLib check." Remove the `getActor()` bullet, or move its
behaviour to docs/permissions.md. It is not a breaking change from 0.17.2.

## D3-2 doc-wrong, low - CHANGELOG.md:212 lists a new rejection under Fixed

What is wrong: "Adding a child or alias whose name is already taken fails at once instead of when the tree is built."
In 0.17.2 nothing rejected a duplicate child name or alias. The only duplicate check was `AbstractArgumentBuilder` for
argument names. Brigadier silently merged same-named literals. Now `addChildren` / `addAliases` throw
`IllegalArgumentException` ("Duplicate command name or alias ...", CommandNameValidator:90). For a 0.17.2 upgrader
this is a tree that used to be accepted and now throws.

Evidence: `git grep -n -i "duplicate\|already" 0.17.2 -- common/src/main` finds only the argument-name check.

Suggested fix: move it to Breaking changes > Registration > "Registration rejects trees it used to accept": "a child
command or alias whose name a sibling already has (`IllegalArgumentException`)". Drop it from Fixed.

## D3-3 doc-wrong, low - CHANGELOG.md:98-99 Folia register/unregister thread rule is still too narrow (E2-1 fix incomplete)

What is wrong: "(on Folia, off the global region thread; region threads are refused too)" says that only the global
region thread is accepted. `paper` `CommandLib#requireGlobalThread` accepts any primary thread that is not ticking a
region, so it also accepts the server thread that runs `onEnable` / `onDisable`. That thread is not the global region
thread, which is why B-4 used this check. docs/execution.md:173-174 gets it right ("register during initialization ...
a later register or unregister must run on the global region thread"). Read literally, the CHANGELOG says that
registering in `onEnable` on Folia throws.

Evidence: paper/src/main/java/dev/marulabs/commandlib/CommandLib.java:399-405. Triage B-4 note: isGlobalTickThread is
false in onEnable.

Suggested fix: "(on Folia, from `onEnable` / `onDisable` or the global region thread; region threads are refused)".

## Out of area (code), found while checking selector docs

### D3-4 bug (message), low - spigot-testing/src/main/java/dev/marulabs/commandlib/nms/argument/MockEntitySelector.java:92

The exception text reads "No fake entity entities with name: ...". A rename in 1ef103f7 (registered -> entities) also
changed this string. paper-testing's CommandTester:526 still says "No fake entity registered with name". Fix: restore
"registered".
