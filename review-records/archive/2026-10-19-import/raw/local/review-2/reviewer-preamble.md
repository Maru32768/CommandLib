# Instructions for reviewer agents (CommandLib review, round 2+)

You review the CommandLib repository (Minecraft command library: common core + Spigot/Paper/NMS + Forge/NeoForge/Fabric "modded" + Velocity). You run in your own git worktree.

FIRST STEP: your worktree may have been created at an old commit. Run `git reset --hard master` in your worktree before anything else and confirm `git log -1` matches `git log -1 master`.

Review only; do not commit. You may write throwaway tests / run focused Gradle tasks to confirm a suspicion, then delete them.

Context:
- Review range: everything after base `2c8ffd7aa17181325ba6f4759586a2bfb94e86ed` (`git diff 2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD -- <your paths>`).
- A previous round already found and fixed many issues. Its triage notes are in `C:/Users/maru3/IdeaProjects/CommandLib_2/.local/review-2/triage.md` and the reports in `round1-*.md` next to it. Do NOT re-report items listed there (fixed, skipped, or pending) unless the fix itself is wrong or incomplete.
- Give special attention to the round-1 fix commits: `git log --oneline 6b898637..HEAD` (they were made by parallel agents and merged with some conflict resolution, so look for regressions, half-done changes, inconsistent behaviour across platforms, and stale docs/tests).
- Pre-GA: breaking changes are fine; unneeded APIs should be deleted rather than deprecated. Flag backward-compat leftovers.

How: invoke the `code-review` skill via the Skill tool (args e.g. `high`). It cannot take a commit range, so apply its recipe manually to the range/paths. Do not post comments, do not use --fix.

Build notes: Windows, `./gradlew` from Git Bash; non-modded builds add `--configure-on-demand -Pcommandlib.typedNms=false`. Other agents build in parallel: for ANY modded build use your own short Gradle home `-g C:/Users/maru3/AppData/Local/Temp/gh-<yourtag>` and delete it afterwards; avoid modded builds unless essential. No Docker.

Output: worktree isolation blocks writing outside your worktree, so write the full report to `.local/review-2/<file named in your task>` INSIDE your worktree and then copy it with Bash `cp` to `C:/Users/maru3/IdeaProjects/CommandLib_2/.local/review-2/`. For each finding: id, category (bug | regression | backcompat | improvement | test-vacuous | test-gap | test-util-bug | doc-wrong | spec-question), severity, file:line, what is wrong, evidence/how confirmed (mark "unconfirmed" otherwise), suggested fix. Skip nitpicks and pure style.
Final message: compact list (id, category, severity, file:line, one-line summary, one-line fix). If you found nothing worth fixing, say so plainly.
