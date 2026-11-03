# Triage

Assign stable IDs and link each finding to its original review. Record decisions independently of the original text.

Source: `reviews/agents/claude-opus-5.5-pbt-1.md`. Evidence `reproduced` refers to
`evidence/reproduction/scratch-test-output.txt` or `paper-probe-output.txt`; `traced` refers to the file:line citations in
the report.

| ID | Source | Decision | Evidence | Rationale / duplicate of | Follow-up |
|---|---|---|---|---|---|
| F1 | report §3 F1 (h1, h7) | needs-decision | reproduced | The generated `help` literal shadows the value `help` of a first argument; behavior depends on preceding options. Choose between dropping the literal where a first argument exists, keeping it and documenting the lost value, or another spelling | roadmap P0 要判断 |
| F2 | report §3 F2 (h2) | accepted | reproduced | `OptionGraph.isVariadic` mistakes a node shared with a variadic branch for a variadic node | roadmap P0 仕様判断不要 |
| F3 | report §3 F3 (h3) | accepted | reproduced | `findRegion` / `prefix` strip a namespace from names that contain `:`; reject such names or strip only a platform namespace | roadmap P0 仕様判断不要 |
| F4 | report §3 F4 (h4, P1) | duplicate | reproduced | New cases of 20-F05 (index-based inputs) | roadmap P0 20-F05 item |
| F5 | report §3 F5 (p5) | accepted | reproduced | `QuotableSuggestions` neither quotes keys that are option tokens nor unescapes the typed prefix | roadmap P0 仕様判断不要 |
| F6 | report §3 F6 (h6) | duplicate | reproduced (model of Paper's registration) | New symptom of 20-F05: Paper's `getInputs()` depends on option spelling | roadmap P0 20-F05 item |
| F7 | report §3 F7 (h7) | accepted | reproduced | Enum constants outside the unquoted character set are suggested but cannot be typed | roadmap P0 仕様判断不要 |
| F8 | report §3 F8 (p4) | accepted | reproduced | Comment of `CommonDurationArgument.parse` claims a Duration overflow limit; the limit is a long amount | roadmap P0 仕様判断不要 |
| F9 | report §3 F9 (Paper h5) | needs-decision | reproduced (Paper), traced (Spigot), unverified (vanilla 1.20.5+) | No contract on selector permission for the self-implemented `@a` / `@r` | roadmap P0 要判断 |
| U1-U4 | report §5 | untriaged | traced (U2, U3), unverified (U1, U4) | Not reproduced with the testers | none yet |

Decisions: untriaged, accepted, rejected, duplicate, needs-decision, deferred.
Evidence: historical-report, traced, reproduced, regression-tested, or unverified. Acceptance alone is not proof.
