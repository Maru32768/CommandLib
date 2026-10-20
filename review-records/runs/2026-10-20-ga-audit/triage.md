# Triage

Assign stable IDs and link each finding to its original review. Record decisions independently of the original text.

Source: `reviews/agents/ga-architecture-api-1.md` (section numbers in parentheses). Evidence `reproduced` refers to
`evidence/reproduction/scratch-test-output.txt`; `traced` refers to the file:line citations in the report.

| ID | Source | Decision | Evidence | Rationale / duplicate of | Follow-up |
|---|---|---|---|---|---|
| F01 | report §2 F01 (h1, h1b) | untriaged | reproduced | Argument names collide across parent branch and argument child; no registration-time check | |
| F02 | report §2 F02 (h3, h10) | untriaged | reproduced (tester), traced (Paper/modded rebuild) | Mutable model after registration; snapshot point differs per platform | |
| F03 | report §2 F03 (h7) | untriaged | reproduced | Duplicate permission node with conflicting defaults, last registration wins | |
| F04 | report §2 F04 (h8) | untriaged | reproduced | Argument-instance executor creates an undocumented executable point | |
| F05 | report §2 F05 (h2) | untriaged | reproduced | Index-based getters misaligned and platform dependent | |
| F06 | report §2 F06 (h6) | untriaged | reproduced | `getOption` of another command's option returns its default | |
| F07 | report §2 F07 | untriaged | traced | Testing docs promise more than renderer/Velocity tester keep | |
| F08 | report §2 F08 | untriaged | traced | Registration failure timing, unregister, label conflicts differ per platform | |
| F09 | report §2 F09 | needs-decision | traced | `/execute as` actor semantics differ (roadmap `[?]`) | |
| F10 | report §3 F10 (h5, h9) | untriaged | reproduced | Unsatisfiable option constraints and duplicate child accepted | |
| F11 | report §3 F11 (h4) | needs-decision | reproduced | Failing executor consumes the cooldown | |
| F12 | report §3 F12 | untriaged | traced | No API boundary markers or stability policy; japicmp baseline unusable | |
| F13 | report §3 F13 | untriaged | traced | Contract details: exception types, thread rules, reloadData fallback, last-executor-wins | |
| F14 | report §3 F14 | untriaged | traced | Annotation API: @Range saturation, branch config, error attribution, stale design doc | |
| F15 | report §3 F15 | untriaged | traced | Kotlin docs vs Velocity threads, `require {}` semantics | |
| R1–R10 | report §4 | untriaged | traced | Roadmap inconsistencies | |

Decisions: untriaged, accepted, rejected, duplicate, needs-decision, deferred.
Evidence: historical-report, traced, reproduced, regression-tested, or unverified. Acceptance alone is not proof.
