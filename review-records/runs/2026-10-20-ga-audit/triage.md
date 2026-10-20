# Triage

Assign stable IDs and link each finding to its original review. Record decisions independently of the original text.

Source: `reviews/agents/ga-architecture-api-1.md` (section numbers in parentheses). Evidence `reproduced` refers to
`evidence/reproduction/scratch-test-output.txt`; `traced` refers to the file:line citations in the report.

| ID | Source | Decision | Evidence | Rationale / duplicate of | Follow-up |
|---|---|---|---|---|---|
| F01 | report §2 F01 (h1, h1b) | accepted | reproduced | Argument names collide across parent branch and argument child; no registration-time check | roadmap P0 仕様判断不要 |
| F02 | report §2 F02 (h3, h10) | needs-decision | reproduced (tester), traced (Paper/modded rebuild) | Mutable model after registration; snapshot point differs per platform | roadmap P0 要判断 |
| F03 | report §2 F03 (h7) | accepted | reproduced | Duplicate permission node with conflicting defaults, last registration wins | roadmap P0 仕様判断不要 |
| F04 | report §2 F04 (h8) | needs-decision | reproduced | Argument-instance executor creates an undocumented executable point | roadmap P0 要判断 |
| F05 | report §2 F05 (h2) | needs-decision | reproduced | Index-based getters misaligned and platform dependent | roadmap P0 要判断 |
| F06 | report §2 F06 (h6) | accepted | reproduced | `getOption` of another command's option returns its default | roadmap P0 仕様判断不要 |
| F07 | report §2 F07 | accepted | traced | Testing docs promise more than renderer/Velocity tester keep | roadmap P0 仕様判断不要 |
| F08 | report §2 F08 | accepted | traced | Registration failure timing, unregister, label conflicts differ per platform | roadmap P0; cross-registration rule is P0 要判断 |
| F09 | report §2 F09 | needs-decision | traced | `/execute as` actor semantics differ (roadmap `[?]`) | roadmap P0 要判断 / P3 `/execute as` |
| F10 | report §3 F10 (h5, h9) | accepted | reproduced | Unsatisfiable option constraints and duplicate child accepted | roadmap P0 仕様判断不要 |
| F11 | report §3 F11 (h4) | needs-decision | reproduced | Failing executor consumes the cooldown | roadmap P0 要判断 |
| F12 | report §3 F12 | needs-decision | traced | No API boundary markers or stability policy; japicmp baseline unusable | roadmap P0 要判断 (GA definition); P6 internal package item |
| F13 | report §3 F13 | accepted | traced | Contract details: exception types, thread rules, reloadData fallback, last-executor-wins | roadmap P0 仕様判断不要 |
| F14 | report §3 F14 | accepted | traced | Annotation API: @Range saturation, branch config, error attribution, stale design doc | roadmap P0; @Subcommand branch config is P0 要判断 |
| F15 | report §3 F15 | accepted | traced | Kotlin docs vs Velocity threads, `require {}` semantics | roadmap P0 仕様判断不要 |
| R1–R10 | report §4 | accepted | traced | Roadmap inconsistencies | R3–R6 fixed in roadmap 2026-10-21; R1/R2 in P0 要判断; R8 in docs item |

Decisions: untriaged, accepted, rejected, duplicate, needs-decision, deferred.
Evidence: historical-report, traced, reproduced, regression-tested, or unverified. Acceptance alone is not proof.
