# Instructions for ponytail-review agents (CommandLib)

You review the CommandLib repository (Minecraft command library: common core + Spigot/Paper/NMS + Forge/NeoForge/Fabric "modded" + Velocity). You run in your own git worktree.

FIRST STEP: run `git reset --hard master` in your worktree and confirm `git log -1` matches `git log -1 master`.

Review only; do not commit. Throwaway probes are fine (delete them).

1. Invoke the `ponytail:ponytail-review` skill via the Skill tool and follow it. The change to review is `git diff 2c8ffd7aa17181325ba6f4759586a2bfb94e86ed..HEAD -- <your paths>` (the skill may assume the last commit or a PR; apply it to this range and these paths instead). Read the connected code, not only the diff.
2. The project is pre-GA: breaking changes are fine, unneeded API should be deleted, not deprecated. Bias toward deletion and simplification where it clearly pays off, but do not propose churn for its own sake.
3. Several review rounds already ran. Their decisions are in `C:/Users/maru3/IdeaProjects/CommandLib_2/.local/review-2/triage.md` (fixed / skipped / Pending). Don't re-report those items unless you have new evidence.
4. Build notes: Windows, `./gradlew` from Git Bash; non-modded builds add `--configure-on-demand -Pcommandlib.typedNms=false`. For a modded build use your own short Gradle home `-g C:/Users/maru3/AppData/Local/Temp/gh-<tag>` and delete it afterwards; avoid unless essential. No Docker.

Output: write the full report to `.local/review-2/<file named in your task>` inside your worktree, then `cp` it to `C:/Users/maru3/IdeaProjects/CommandLib_2/.local/review-2/`. For each finding: id, category (bug | safety | load | test-gap | perf | delete | simplify | spec-question), severity, file:line, the problem in plain English, evidence (mark "unconfirmed" if only read), suggested fix, and for delete/simplify the rough line savings.
Final message: compact list (id, category, severity, file:line, one-line summary, one-line fix). If nothing is worth fixing, say so plainly.
