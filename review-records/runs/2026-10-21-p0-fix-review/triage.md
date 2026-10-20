# Triage

Assign stable IDs and link each finding to its original review. Record decisions independently of the original text.

`CR-n` is finding n of `reviews/agents/code-review-1.md`, `PT-n` finding n of `reviews/agents/ponytail-review-1.md`,
`PT-NC-n` an item that review listed under "Not checked".

| ID | Source | Decision | Evidence | Rationale / duplicate of | Follow-up |
|---|---|---|---|---|---|
| CR-1 | code-review-1 #1 | accepted | traced | Unregistering the newer of two live registrations sharing a node removes the node the older one still uses (Spigot, Paper). | fix round 1 |
| PT-1 | ponytail-review-1 #1 | duplicate | traced | duplicate of CR-1 | — |
| CR-2 | code-review-1 #2 | accepted | traced | `getInput(name)` of an argument returns a later literal's text when a literal on the route has the same name; validators and `additionalParser` see it. Distinct from the index API question (20-F05). | fix round 1 |
| CR-3 | code-review-1 #3 | rejected | traced | Server-side (ask_server) completion is the platform's dispatcher; `docs/suggestions.md:132` already states that it may list a literal the sender cannot use. No platform hook exists. | — |
| CR-4 | code-review-1 #4 | accepted | traced | Spigot compiles functions synchronously inside `unregister()`; move it back to the scheduled task and guard the `finally` block. | fix round 1 |
| CR-5 | code-review-1 #5 | rejected | traced | `CommonCommand.defaults(...)` replaces a single field (`CommonCommand.java:797-798`); re-registering after a failed `register` does not apply middlewares twice. | — |
| CR-6 | code-review-1 #6 | rejected | traced | The second tree build happens once per registration, at start-up only; one validation path through the real builder is kept on purpose. | — |
| CR-7 | code-review-1 #7 | accepted | traced | An alias equal to the command name or repeated is filtered only when nodes are built; reject or drop it in `addAliases`. | fix round 1 |
| PT-2 | ponytail-review-1 #2 | duplicate | traced | duplicate of CR-7 | — |
| CR-8 | code-review-1 #8 | accepted | traced | Required option that requires an option it excludes (or that another required option excludes) is still accepted. | fix round 1 |
| CR-9 | code-review-1 #9 | rejected | traced | Two ~10-line copies in mocks of two different platforms; sharing them would need a new shared main source set for the testing artifacts. | — |
| PT-3 | ponytail-review-1 #3 | accepted | traced | Static cache on a cold path (`HelpTopics.indexTopics`); revert to the plain lookup. | fix round 1 |
| PT-4 | ponytail-review-1 #4 | accepted | traced | `.jev-lint.yaml` misses `common/src/bukkit/java` and `common/src/adventure/java`. | fix round 1 |
| PT-NC-1 | ponytail-review-1 "Not checked" | accepted | traced | Built-in captions are now read through `ClassLoader#getResourceAsStream`, which a named module does not open; read them class-relative as before (`Class#getResourceAsStream` finds resources in the class's own module). | fix round 1 |


## Round 2 (`6f116a35..0f40c1a5`)

`CR2-n` is finding n of `reviews/agents/code-review-2.md` (in the order of its JSON list), `PT2-n` finding n of
`reviews/agents/ponytail-review-2.md`.

| ID | Source | Decision | Evidence | Rationale / duplicate of | Follow-up |
|---|---|---|---|---|---|
| CR2-1 | code-review-2 #1 | accepted | traced | The always-given closure of CR-8 follows `requires(option, value)` / predicate edges, which a default value satisfies without the option being given; only plain `requires(option)` edges force an option. | fix round 2 |
| CR2-2 | code-review-2 #2 | accepted | traced | Last `unregister()` plus a plugin disable in the same tick leaves functions compiled with the removed commands (regression of CR-4). | fix round 2 |
| PT2-1 | ponytail-review-2 #1 | duplicate | traced | duplicate of CR2-2 | — |
| CR2-3 | code-review-2 #3 | accepted | traced | CR-2 changed what `getInputs()` / `getInput(int)` show while a variadic element or option value is parsed, against its own claim; swap both maps as before. | fix round 2 |
| PT2-3 | ponytail-review-2 #3 | duplicate | traced | duplicate of CR2-3 (resolved in code rather than by a docs note) | — |
| CR2-4 | code-review-2 #4 | accepted | traced | Other cleanup steps in Spigot's `unregister()` `finally` can still hide the root failure and skip the remaining cleanup. | fix round 2 |
| CR2-5 | code-review-2 #5 | accepted | traced | The guard test of CR-4 never reaches the guard (timer mock not cancelled). | fix round 2 |
| PT2-2 | ponytail-review-2 #2 | duplicate | traced | duplicate of CR2-5 | — |
| CR2-6 | code-review-2 #6 | accepted | traced | `VariadicArgument.parseValues` restores a non-null input and can mark the argument as given; restore "not given" as `CommonArgument` does. | fix round 2 |
| CR2-7 | code-review-2 #7 | accepted | traced | `restorePermissions` duplicated between Spigot and Paper right after it was written; share it in `common/src/bukkit/java`. | fix round 2 |
| CR2-8 | code-review-2 #8 | deferred | traced | Recording node ownership at registration is a redesign of the cross-registration permission rule, which is a pending maintainer decision (roadmap P0 要判断, 18-C03 / R2-R3B-1). | roadmap P0 要判断 |
| PT2-4 | ponytail-review-2 #4 | accepted | traced | `docs/design/module-structure.md` says every public internal class is annotated; generated typed NMS classes and the Kotlin facade rely on the package (or have none). Correct the sentence. | fix round 2 |

Decisions: untriaged, accepted, rejected, duplicate, needs-decision, deferred.
Evidence: historical-report, traced, reproduced, regression-tested, or unverified. Acceptance alone is not proof.
