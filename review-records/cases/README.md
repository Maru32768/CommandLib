# Harness evaluation cases

Store extracted candidates in `candidates/<case-id>/` with these parts:

- `case.json`: reviewed revision, provenance, fix and test commits, and validation status.
- `input/task.md`: the request given to the agent, without findings, answers, or fix commits.
- `oracle/expected.md`: grading criteria, evidence, and verification instructions for the evaluator only.

Give the agent only the specified revision and inputs in an isolated workspace. Exclude grading evidence, original
reports, review records, and subsequent fix history. Public historical cases can still be affected by prior model
exposure.

The `historical-evidence-only` status means that a candidate has historical evidence but has not been reproduced
as an extracted evaluation case. Before using it as a runnable benchmark, pin the revision, demonstrate failure
before the fix and success after it, and record dependencies, environment requirements, working-tree differences,
and grading criteria. Document how required external artifacts can be obtained.

Cases can evaluate valid findings, false positives, unresolved specification questions, duplicate findings,
severity judgments, regressions caused by fixes, and unsupported claims.
