# Instructions for fixer agents (CommandLib review round)

You are fixing review findings in the CommandLib repository (Minecraft command library). You run in your own git worktree on a branch created from master HEAD. Read `CLAUDE.md` and the `docs/agents/*.md` files it lists for the modules you touch.

Rules:
- The project is pre-GA. Breaking API changes are fine; delete unneeded APIs instead of deprecating; no compatibility shims.
- For each assigned finding: read its full entry in the report file named below (absolute path, read-only for you), VERIFY it against the code first (reproduce with a test when cheap). If it is not real, skip it and say why. If it needs a product/spec decision, do not guess: mark it "pending" with the question.
- Fix the root cause in shared code once. Add a focused regression test for non-trivial logic. Update every caller, test, fixture, sample (`samples/`, `examples/`), doc (`docs/`, `README.md`) and the downstream skill (`plugins/commandlib/skills/commandlib/**`) that references an API you change or delete.
- Keep the diff minimal; match surrounding style; comment only the non-obvious why. No unrelated refactors.
- If you find another clear bug while working in your area, fix it too (separate commit) and report it.
- Commit on your worktree branch (never push, never touch master). One commit per finding or tight group. Message style: conventional prefix like `fix(common): ...`, `fix(paper): ...`, `test(velocity-testing): ...`, `refactor(spigot): ...`; a short body explaining why. End every message with the line:
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
- Format with `./gradlew spotlessApply` (scoped: `:<module>:spotlessApply`) before committing; Spotless must pass.
- Verify with focused Gradle tasks for touched modules. Windows: run `./gradlew` from Git Bash. Non-modded builds: add `--configure-on-demand -Pcommandlib.typedNms=false`. Other agents build in parallel worktrees: for ANY modded / modded-testing build you MUST use your own short Gradle home `-g C:/Users/maru3/AppData/Local/Temp/gh-<yourtag>` (short path, never under the scratchpad) and delete that directory when finished. Do not run Docker integration targets unless your task says so.
- Report test results faithfully (paste the failing output if anything fails).

Final message format:
1. Branch name and `git log --oneline master..HEAD`.
2. A table: finding id | status (fixed / skipped / pending) | commit | one-line note (for skipped: why; for pending: the exact question).
3. Extra bugs you found and fixed.
4. Tests/tasks run and their result.
