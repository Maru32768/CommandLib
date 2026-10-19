# CommandLib GA review — scope

## Baseline and reviewed revision

| Item | Value |
|---|---|
| Baseline | `origin/master` = `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed` (2026-10-15 00:30 +0900) |
| Reviewed HEAD | `master` = `1b8f17dc2b10b8ab7bd8707e6aa0eb5a522b8ec5` (2026-10-16 14:55 +0900) |
| Commits in range | 192 (`git rev-list --count origin/master..HEAD`); `HEAD..origin/master` is empty, so `origin/master` is the merge base |
| Diff size | 1666 files, +65,536 / −3,167 |
| Uncommitted working tree | `ArgRef.java:42` and `OptionGraph.java:54`: comment-only wording edits ("outside of" → "outside", "re-parse" → "reparse"). Not reviewed; no behaviour impact. |

**Why this baseline.** The task statement says the work is the diff between `master` and `origin/master`. Repository
evidence agrees: `origin/master` is an ancestor of HEAD, the merge base equals `origin/master`, every commit in the range
is authored on 2026-10-15/16 in the roadmap-campaign style (`feat(common): …`, `docs: record the real-server …`), and
`../../../docs/roadmap.md` / `docs/roadmap-history.md` change in the same range to mark roadmap items done. No other branch or
tag sits between them.

Reproduce: `git diff origin/master..HEAD`, `git log --oneline origin/master..HEAD`.

## Commit mix

74 `docs`, ~44 `feat`/`feat!` (common 18, kotlin 4, modded 4, paper 4, samples 4, velocity 2, spigot 2, …), ~25 `fix`,
~13 `test`, 4 `build`, 3 `perf`, a few `style`/`refactor`. Four commits are marked breaking (`feat!`, `fix(paper)!`).

## Subsystem inventory (added lines per module)

| Subsystem | Added lines / files | New in range? | Main content |
|---|---:|---|---|
| `common` main | ~8.5k / ~100 | partly | Annotation API (`AnnotationParser` 1.4k), options (`OptionGraph`, `CommandOption`, `Options`, `OptionNode`), Java `CommandBuilder`, `CommandTree`, optional/variadic args, middlewares, async, cooldown/confirm, context values, captions/localization, help/usage/error formatters, `CommandDocs`/`Json`, diagnostics |
| `common` tests | ~5.3k / ~50 | — | Unit tests and snapshots |
| `common-testing` | 314 / 9 | — | Fake platform harness, JUnit extension |
| `common-kotlin`, `*-kotlin` | ~950 / 11 | yes | Kotlin DSL, coroutines |
| `spigot` main | ~1.6k / 57 | — | Registration, new vanilla arguments, NMS registry, diagnostics command, Folia refusal |
| `paper` main | ~1.3k / 40 | — | Same argument set, Folia schedulers, selector errors |
| `nms/*` (29 typed modules) | ~400 each, ~12k total | modules existed; regenerated | Generated per-version NMS bridges (`../../../tools/nms-codegen`) |
| `velocity`, `velocity-testing`, `velocity-kotlin` | ~2.4k / 40 | yes | New platform |
| `modded` (Forge/NeoForge/Fabric via Stonecutter) | ~2.0k / 51 | Fabric + 4 versions new | Arguments, annotation bridge, scheduler, server tracking |
| `spigot-testing`, `paper-testing`, `modded-testing` | ~8.5k / 148 | — | Harness + tests |
| `integration-test` | ~2.4k / 32 | — | Real-server fixtures (Docker), 29 targets |
| `examples`, `samples/*` | ~3.9k / 75 | partly | Example code + style-parity tests; standalone sample builds |
| `benchmark` | 7.9k / 7 (7.5k = `baseline.json`) | yes | JMH benchmarks |
| `buildSrc`, `../../../tools/nms-codegen`, build scripts | ~0.7k | — | NMS module plugin, generator |
| `docs`, `../../../README.md`, `mkdocs.yml` | ~8.4k | — | User docs, roadmap |

## Review assignment

All primary reviewers ran on Opus in independent fresh contexts, in parallel, and each wrote its own raw report
(`raw`) before seeing any other report.

| Agent | Area | Raw report |
|---|---|---|
| A1 | common: options, tree building, arguments, optional/variadic, suggestions | `raw/A1-production-tree-options.md` |
| A2 | common: execution pipeline, middleware/async/cooldown/confirm, context values, captions, help/docs, diagnostics | `raw/A2-production-execution.md` |
| A3 | common annotation API, platform `AnnotatedCommands`, Kotlin DSL/coroutines | `raw/A3-production-annotations-kotlin.md` |
| A4 | Spigot/Paper/Folia integration, NMS registry, typed NMS modules + generator | `raw/A4-production-bukkit-paper-nms.md` |
| A5 | Velocity, modded (Forge/NeoForge/Fabric) | `raw/A5-production-velocity-modded.md` |
| B1 | Tests: common, common-testing, common-kotlin, examples, benchmark artefact | `raw/B1-tests-common.md` |
| B2 | Tests: spigot/paper/velocity/modded testing modules, platform unit tests, integration-test, samples | `raw/B2-tests-platform.md` |
| C | Complexity / maintainability across the diff | `raw/C-complexity.md` |
| D | Roadmap requirement traceability | `raw/D-roadmap.md` |

Coverage actually achieved is recorded in the matrix below, from each agent's own coverage section.

## Review coverage matrix

Depth: **Deep**: read end to end, with paths traced and repro or mutation attempts. **Traced**: the relevant paths
were read, with no runtime check. **Skim**: spot-checked. **None**: not examined.

| Subsystem | Production review | Test audit | Verified at runtime? | Not examined / gaps |
|---|---|---|---|---|
| common: options, `OptionGraph`, tree building, args | Deep (A1) | Deep, with mutations (B1) | Yes: scratch JUnit (A1, V2), 15 mutations (B1) | Typed branches beyond `Unary`. Some `argument/*` classes. Paper argument types inside the option re-parse. |
| common: runner, middleware, async, cooldown/confirm, context values | Deep (A2) | Deep (B1) | Yes (A2, V2, B1) | Platform schedulers and executor shutdown, from the common side |
| common: help, usage, errors, captions, docs/JSON, diagnostics | Deep (A2); complexity (C) | Deep (B1), snapshots read | Yes (A2, V2) | Platform message rendering |
| common annotation API | Deep, full read of `AnnotationParser` (A3) | Skim (B1) | Yes (A3) | Core validation of argument order and alias collisions (partly covered by A1) |
| Platform `AnnotatedCommands` | Traced (A3) | Traced | No | Modded runtime. Velocity has no tests (D-07). |
| Kotlin DSL / coroutines | Deep (A3) | Traced (B1) | Yes (A3: StackOverflow repro) | Folia dispatcher when a task is retired. `SuggestionBuilder.await` errors. paper-kotlin build. |
| Spigot main | Deep (A4) | Deep, with mutations (B2) | Unit suites pass (B2). Bytecode of Bukkit's disable order (V1). | Mohist-specific paths. `FunctionLoadErrorFilter`. |
| Paper / Folia main | Deep (A4) | Deep, with mutations (B2) | Unit suites pass (B2) | `HelpTopics`, `RichTexts`. Paper's real resolver class (only the paperclip patch is on disk). No real Folia run. |
| typed NMS modules + `../../../tools/nms-codegen` | Traced: 4 of 29 modules plus the generator (A4); complexity (C) | None | No | About 25 modules. The `spigot_legacy.py` diff. buildSrc beyond the release setting. |
| Velocity | Deep (A5) | Deep (B2) | Unit suites pass (B2) | Alias node sharing as the proxy sends it to clients. No real proxy run. |
| modded (Forge / NeoForge / Fabric) | Traced, with bytecode of cached loader jars (A5, V1) | Traced (B2) | **No.** Not built; Loom cache risk. | `AnnotatedCommands`. UUID / Location / GameProfile / Enchantment / Effect / Component / Message / Particle / Predicate arguments. `RichTexts`. modded-testing at runtime. |
| testing harnesses (common, spigot, paper, velocity, modded) | — | Deep (B1, B2) | Yes: probes and mutations (B1, B2; not modded) | modded-testing not executed |
| integration-test fixtures + runner | — | Traced (B2) | **No.** Docker targets not run. | No local evidence for the 10-16 campaign (GA-20) |
| examples, samples | — | Skim (B1, B2) | examples run within the suites | Samples are not built (they need 0.18.0) |
| benchmark | — | Traced (B1); complexity (C) | No | Benchmark classes other than `ExecuteBenchmark` |
| build scripts, buildSrc | Partial (A4, C) | — | No | Publishing/relocation configuration, beyond what A4 and C read |
| docs, README, roadmap | Spot-checked against code (D, A3) | — | — | Not every doc page was cross-checked |

**Explicitly incomplete:**

- Nothing in this review was run on a real server or proxy.
- Modded code was never compiled or executed by the reviewers.
- About 25 of the 29 generated NMS modules were not read; the review relies on the generator being uniform.
- The uncommitted working-tree edits (comment-only) were not reviewed.
