# CommandLib GA review — executive summary

Reviewed `origin/master` `2c8ffd7a` → `master` `1b8f17dc`: 192 commits, 1,666 files, +65.5k lines. Nine independent
Opus reviewers (production ×5, tests ×2, complexity, roadmap) worked in parallel. Two fresh-context verifiers then tried
to refute the significant findings. Details: [`findings.md`](findings.md). Coverage: [`scope.md`](scope.md).

## Verdict

**Not ready for GA as-is, but close on the code.**

- There is no P0 code defect.
- The common core held up well under deep review and mutation testing. Option re-parse, cooldown/confirm basics, help
  filtering and the annotation parser's reflection paths are sound.
- What blocks GA is:
  1. one platform bug that silently drops all commands (Forge 1.16.5 `/reload`);
  2. authorization paths that no test protects;
  3. release process and evidence (release notes, CI/compat gate, real-server proof for the newly claimed platforms).

## Counts (after verification and deduplication)

| Severity | Count | Of which |
|---|---:|---|
| P0 | 0 | — |
| P1 | 6 | 1 code bug (GA-01); 2 test deficiencies on authorization (GA-02, GA-03); 3 release-process/evidence items (GA-04, GA-05, GA-06) |
| P2 | 16 | 9 code bugs or behaviour issues (GA-07–GA-15); 4 test-quality groups (GA-16–GA-18, GA-22); docs/evidence (GA-19, GA-20); 1 maintainability item (GA-21) |
| P3 | 19 rows (≈45 items) | Minor bugs, validation gaps, duplication |

Verification:

- Every P1/P2 code finding was reproduced by two agents, or confirmed against platform bytecode.
- No finding was refuted.
- Eight severities were lowered: A1-01, A1-06, C-01, A2-01, A2-02, A3-02, A4-02 and A5-03.
- Two claims were corrected in substance: the RCON class name and the A4-02 documentation premise.

## What is broken (verified)

1. **GA-01 (P1):** on Forge 1.16.5, `/reload` drops every CommandLib command, and data pack functions that call them
   fail to load. This predates the range.
2. **GA-07:** `optionsAnywhere()` with an argument child under a branch of two or more arguments errors when an option
   comes early.
3. **GA-08:** Kotlin dispatchers run blocks in place. `yield()` loops overflow the stack, and Spigot/Paper never get the
   main thread back.
4. **GA-09:** a cooldown can be bypassed while an async middleware is waiting.
5. **GA-10:** `/<cmd> help` skips prerequisites and error handlers.
6. **GA-11:** Spigot `unregister()` in `onDisable()` throws, so the rest of `onDisable` (for example data saving) is
   skipped.
7. **GA-12:** on Fabric production, command blocks pass `requireConsole()`.
8. **GA-14:** merged same-name branches use the longest branch's permission.
9. **GA-15:** same-name arguments with different types fail at runtime.

## What remains unverified

- No real-server or proxy run was done by this review.
- The 10-16 campaign's results are not in the repo (GA-20).
- Velocity, Fabric and Folia have no integration target at all (GA-05).
- Modded code was not compiled; findings there are bytecode-traced.
- About 25 of 29 generated NMS modules were not read individually.

## What is unnecessarily complex

The core abstractions are justified. C measured `OptionGraph`'s redirect machinery cutting a tree from 491,607 to
5,490 nodes. The cost is duplication:

- 4 copies of the annotation type mapping (GA-21);
- identical Spigot/Paper source, tests and IT fixtures (one copy has already drifted);
- 5 copies of the testing plumbing;
- 45 hand-written `CommandBuilder` forwarders;
- 1,452 generated NMS files with no up-to-date check;
- a 7.6k-line benchmark baseline that gates nothing (GA-22).

## Recommended fix and review order

1. **GA-01:** use `RegisterCommandsEvent` on Forge 1.16.5. Add an IT step that runs `/reload`.
2. **GA-02, GA-03:** add the missing authorization tests: branch permission under `optionsAnywhere()`, and modded
   permission denial with a real `deniedProbe`. Cheap, and they protect the riskiest regressions.
3. **GA-11, GA-12, GA-13:**
   - Make Spigot `unregister` idempotent.
   - Replace class-name sniffing with `instanceof`.
   - Decide and document the modded non-player permission policy.
4. **GA-08, GA-09, GA-10:** the dispatcher must always schedule; reserve the cooldown at check time; route help through
   `CommandRunner`.
5. **GA-07, GA-14, GA-15:** fix or reject the bad tree shapes at registration.
6. **GA-16, GA-17:**
   - The tester should rethrow `AssertionError`.
   - Narrow the Mohist allowance.
   - Align the paper-testing mocks with vanilla.
7. **Release decisions (maintainer):**
   - Write release notes (GA-04).
   - Decide whether GA waits for CI and japicmp, or whether the roadmap GA criteria change (GA-06; CI was deliberately
     deferred).
   - Either add smoke targets for Velocity, Fabric and Folia or label them preview (GA-05).
   - Rerun and archive the real-server campaign at the GA commit (GA-20).
   - Fix the docs that overclaim (GA-19).
8. **Afterwards:** the P3 table, then the duplication clean-ups (GA-21, GA-40), which are best done together with the
   Spigot/Paper parity work.

## Artefacts

| File | Content |
|---|---|
| `scope.md` | Baseline/HEAD SHAs, subsystem inventory, assignment, coverage matrix |
| `production.md` | A1–A5 production findings with repros |
| `tests.md` | B1–B2 test audit with mutation tables |
| `complexity.md` | C maintainability review |
| `roadmap.md` | D requirement traceability (43 items) |
| `findings.md` | Consolidated, deduplicated, verified findings with final severities |
| `raw` | Unedited per-agent reports, including the verifier reports V1 and V2 |
