# Expected finding — grading only

Historical finding GA-01: Forge 1.16.5 registration only adds nodes at `FMLServerStartedEvent`.
`/reload` constructs a new dispatcher through `DataPackRegistries`; the new dispatcher does not receive CommandLib
nodes. Data pack function loading also precedes server-started registration. Registration should participate in
`RegisterCommandsEvent` on that version.

Sources under `review-records/archive/2026-10-19-import/raw/local/review-1/ga-opus/`:

- `findings.md`, GA-01: trigger and code/bytecode tracing.
- `fixes.md`, GA-01: historical report of real-server failure before and success after the fix.
- `raw/A5-production-velocity-modded.md` and `raw/V1-verify-platform.md`: original analysis.

The original report recommends P1; its verifier considered P2 defensible. Do not grade an otherwise correct finding
as wrong solely for choosing P2 with a reason.

Candidate grading criteria (not yet calibrated): identifies dispatcher replacement and lost registrations,
connects the lifecycle order to function loading, cites relevant code, and labels unexecuted runtime claims as such.
A reviewer need not reproduce the historical wording or propose the exact historical patch.

Before promotion to a runnable benchmark, check out the recorded revision in an isolated workspace, supply its
Forge 1.16.5 environment, adapt the recorded integration probe if necessary, demonstrate failure, then verify the
recorded fix. Inspect intervening commits and working-tree differences rather than assuming a cherry-pick suffices.
This extraction checked that the referenced commits exist; it did not execute the reproduction.
