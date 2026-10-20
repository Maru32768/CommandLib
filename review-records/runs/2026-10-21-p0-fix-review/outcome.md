# Outcome

## Round 1 (`3279d890..50286c2d`)

All accepted findings of `triage.md` were fixed by one subagent in a worktree and merged on master after the
internal-package reorganization (`f19737a7..446bbec5`):

| ID | Commit | Regression test |
|---|---|---|
| PT-3 | `c27ed4c5` refactor(paper): look up IndexHelpTopic.allTopics on each call again | none (no behaviour change) |
| PT-4 | `01783b9f` chore: lint the shared Bukkit and Adventure source trees with jev-lint | none (lint config) |
| PT-NC-1 | `ae638910` fix(common): read the built-in captions relative to Caption again | none: needs an explicit JPMS module layer |
| CR-2 | `4eac32cc` fix(common): keep an argument's input when a later literal has its name | `CommonCommandContextTest`; failed before the fix with `["validated x","x"]` |
| CR-7 | `5bc2f85b` fix(common): reject an alias equal to the command name or repeated | `CommandStructureTest` (replaces the test of 5061aefd) |
| CR-8 | `ef234ebf` fix(common): reject exclusive options that a required option needs | option tests; failed before the fix |
| CR-1 | `e7eadba6` fix(spigot,paper): give a shared node back to the oldest registration left | Spigot `CommandLibRegistrationTest` (real `SimplePluginManager`), paper-testing `CommandLibLifecycleTest`; both got no permission before the fix |
| CR-4 | `0f40c1a5` fix(spigot): compile functions on the task, not inside unregister() | Spigot `CommandLibRegistrationTest`; failed before the fix |

`PermissionReplacementTest.removing_the_newer_registrations_permission_first_removes_it` and
`CommandStructureTest.repeated_names_within_one_child_are_accepted` locked in the old behaviour and were removed.

### Verification (executed, 2026-10-21, on the rebased branch equal to `0f40c1a5`)

```
./gradlew :common:test :spigot:test :paper:test :velocity:test :common-testing:test :spigot-testing:test \
  :paper-testing:test :velocity-testing:test :spigot-kotlin:test :paper-kotlin:test :velocity-kotlin:test \
  :examples:test :benchmark:compileJava :integration-test:test :integration-test:shared:spigot-fixture:compileJava \
  :integration-test:shared:paper-fixture:compileJava :spotlessCheck -Pcommandlib.typedNms=false --configure-on-demand --offline --continue
./gradlew :modded:<node>:compileJava :modded-testing:<node>:test (all 9 nodes) -Pcommandlib.typedNms=false --offline --continue
```

Both exited 0.

Integration targets on master `0f40c1a5` (executed, run one at a time, `--offline`, the spigot target with
`-Pcommandlib.typedNms=spigot-1.20.4`):

| Target | Result |
|---|---|
| `paper-26.3` | SUCCEEDED 98, FAILED 0 |
| `spigot-1.20.4` | SUCCEEDED 99, FAILED 0 |
| `fabric-1.21.1` | SUCCEEDED 72, FAILED 0 |
| `forge-1.20.1` | SUCCEEDED 72, FAILED 0 |

They cover the reflective `PlatformAdapterImpl` lookup, typed NMS lookup under the new package, the Fabric entrypoint
string and the moved `CurrentServer` on real servers.

## Round 2

See `reviews/agents/code-review-2.md` and `reviews/agents/ponytail-review-2.md` (range `6f116a35..0f40c1a5`).

Accepted findings fixed in `8144fa25..366de2b1` (CR2-1 `8144fa25`, CR2-2 `3a56d74b` and Paper `366de2b1`, CR2-5
`06d7c227`, CR2-4 `a0f07dac`, CR2-7 `5c959f88`, CR2-3 / CR2-6 `8febc53c`, PT2-4 `eb5bf956`). The fix agent ran the
non-modded suite (all modules, `spotlessCheck`) green. CR2-2 is only partly fixed: a runtime `unregister()` followed by
the plugin's disable in the same tick still skips the recompilation; this predates the session and is on the roadmap as a
decision. CR2-8 is on the roadmap as a decision. No third review round and no integration run were done on these commits.
