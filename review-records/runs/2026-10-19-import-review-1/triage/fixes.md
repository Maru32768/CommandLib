# Fix status of the GA review findings

The fixes start from the reviewed HEAD `1b8f17dc` and are on `master`, `444fddcf..6b898637` (52 commits, not pushed). The
user's two uncommitted wording edits (`ArgRef.java`, `OptionGraph.java`) were kept out of every commit.

Verification legend:

- **real server**: an integration target in Docker failed before the fix and passed after it.
- **test**: a new or changed test failed before the fix and passes after it.
- **mutation**: the reviewers' surviving mutation is now killed.
- **trace**: the fix is checked by reading the code and compiling; no runtime reproduction was possible.

## P1

| ID | Commits | Verification |
|---|---|---|
| GA-01 Forge 1.16.5 `/reload` drops commands | `444fddcf` (probe), `441e0b43` (fix) | real server: forge-1.16.5 62/1 → 63/0; the probe passes on forge-1.18.2, forge-1.20.1, neoforge-1.21.1 and neoforge-1.21.4 |
| GA-02 Branch permission under `optionsAnywhere()` untested | `2ce72644` | mutation: M3, M4, M2 and M8 are killed |
| GA-03 Modded permission denial untested | `bd241436` | real server: a player-permission bypass fails forge-1.20.1 |
| GA-04 No release notes | `cedb378f` (later commits add to `../../../CHANGELOG.md`) | review of the claims against the code |
| GA-05 Velocity, Fabric and Folia had no real-server evidence | `782b527f` (Folia), `69559659` (Fabric 1.20.1 and 1.21.1), `57cfba05` (Velocity 3.4.0) | real server: each target passes, and a deliberately broken check fails it |
| GA-06 CI, japicmp and Maven Central | `bcdaa369` | maintainer decision: deferred until after 0.18.0, and the roadmap's GA criteria say so |

## P2

| ID | Commits | Verification |
|---|---|---|
| GA-07 `optionsAnywhere()` child of a branch of two or more arguments | `c4c1b71c` | test |
| GA-08 Kotlin dispatcher runs a dispatch in place | `3a5748eb` | test (`yield()` loop and tick hand-back) |
| GA-09 Cooldown race with an async middleware | `1aa854f4` | test |
| GA-10 The help literal bypasses the runner | `31aa1487` | test |
| GA-11 Spigot `unregister()` in `onDisable` throws | `a7582051` | real server: spigot-1.21.8 logged `IllegalPluginAccessException` before; passes on spigot-1.21.8, spigot-1.16.5, mohist-1.20.1 and paper-1.21.8 |
| GA-12 Fabric command blocks counted as the console | `41bc1954` | real server: fabric-1.21.1 reported `CONSOLE,true` before |
| GA-13 Modded non-player sources passed every check | `41bc1954` | real server: an armor stand ran a `NONE` probe before; maintainer decision: use the permission level |
| GA-14 Shared argument node took the longest branch's permission | `d565e717` | test |
| GA-15 Same-named arguments of different types | `d565e717` | test |
| GA-16 Harness assertions that cannot fail | `c0a9a7ec`, `0ba0a28d`, `ab4cc5c2` | test; mutation (M2, M3 and M4 are killed); real server (mohist-1.20.1) |
| GA-17 paper-testing mocks differ from Paper | `bb543cff`, `e91f87f6` | test; mutation |
| GA-18 Lifecycle tests | covered by `a7582051` and `743befb1` (real-server lifecycle cases) | real server |
| GA-19 Docs that overclaim | `2052821f`, `92b6b528`, `121bdbc8` | review against the code; plus a Velocity annotation test |
| GA-20 The real-server campaign could not be reproduced | `5ca69084`, `793445a4`, `89b9503f` (harness); reports in `campaign/89b9503f` | real server: all 78 targets pass at `89b9503f` (2026-10-17, one at a time, 93 minutes). The earlier failures were in the harness. The images carried a local world and Paper config (`keep-spawn-loaded-range`), Paper 1.20.5 does not tick its spawn chunks, and Spigot 1.20.3 stores 0 even for vanilla commands. `793445a4` skipped Paper 1.16.5, which its message still describes; `89b9503f` removed that skip |
| GA-21 Annotation type mapping duplicated four times | `2bf822de` | tests and unchanged snapshots |
| GA-22 Benchmark gate | `992e2377` | `jmhCompare` against the stripped baseline; `ExecuteBenchmark` input check |

## P3

| ID | Commits | Verification |
|---|---|---|
| GA-23 Option-tree growth | `9730cc20` | test |
| GA-24 Nested help usage | `249e1d27` | test |
| GA-25 Help path under `/execute run` | `a4bf5ce2` | test |
| GA-26 Forked confirmations | `d9293e50` | test |
| GA-27 Paper `Mode.Direct` fallback | `8a254f95` | removed; test asserts the clear error |
| GA-28 Greedy argument not last | `c78b4411` | test |
| GA-29 Fabric shaded copy started late | `3a155d27` | trace; Fabric targets pass; documented limit for the client |
| GA-30 Spigot label ownership | `743befb1` | real server: "Unknown command" before; passes on spigot-1.21.8, paper-1.21.1 and spigot-1.16.5 |
| GA-31 Thread contract of register / unregister | `ba86162e` | test; real server (folia-26.3, paper-1.21.4, spigot-1.16.5 and mohist-1.16.5 still register) |
| GA-32 Paper `MessageArgument` silent fallback | `1bd847f2` | test; real server (paper-1.21.4 and paper-26.3 resolve `@p`) |
| GA-33 Final-field writes (JEP 500) | `1bd847f2` | standalone JDK 26 run; the error names the flag; documented |
| GA-34 `withTimeout` reported nothing | `273537ae` | test |
| GA-35 Modded `List<Holder<X>>` | `559d3703` | test |
| GA-36 Validation gaps | `ca1bd61a` | test |
| GA-37 Execution edge cases | `d0d48dcf` | test (A2-08, printed twice, by trace) |
| GA-38 Velocity quoted suggestions | `a761dffa` | test; real server (velocity-3.4.0) |
| GA-39 Weak tests | `81baf565` | mutation: M9 and M10 are killed |
| GA-40 Duplication | `b8ddd233`, `06ee2734`, `0bb45912` | tests and modded nodes |
| GA-41 Misc gaps | `bb7bd9d5` (optional/variadic on real servers), `121bdbc8` (store success) | real server |

## Not done, and why

- **GA-40 C-05 / C-06 / C-07 / C-08** (identical Spigot/Paper sources, testing-module plumbing, test files and fixtures):
  sharing them needs a new shared module or test source set, an architecture change rather than a GA fix. The
  integration-test README calls the separate fixtures intentional.
- **GA-40 C-10** (no check that the committed NMS modules match the generator): the generator needs BuildTools and
  paperweight outputs, which belong to the deferred CI.
- **GA-39 B1-07** (two "some redirect exists" tests): harmless, and the behaviour is covered elsewhere.
- **GA-39 B1-08 / B2-09** (a FakeSender mode that applies permission defaults; a non-operator bot): this is a new
  testing feature, and the limit is documented in docs/testing.md.
- **GA-39 B2-08 / B2-12**: Folia detection is now proven by folia-26.3, and the Spigot yaw/pitch mapping by the
  integration fixture.
- **GA-41 D-11** (modded relocation in a real server, samples building): this needs the published 0.18.0 artifacts.
- **GA-41 D-15** (unit tests for fixes covered only by integration targets): those fixes stay guarded by the targets.
