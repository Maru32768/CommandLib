# Claude Code Instructions

This repository contains the CommandLib library itself.

For repository maintenance, bug fixes, tests, and implementation work, read:

- `docs/agents/repository.md`
- `docs/agents/testing.md`
- `docs/agents/spigot-testing.md` when touching `spigot-testing`
- `docs/agents/modded.md` when touching `modded`
- `docs/agents/nms-build.md` when touching `nms`, `buildSrc`, or typed NMS registrations
- `docs/agents/argument-addition.md` when adding or changing public argument classes

Do not treat `.claude/skills/commandlib` as repository-maintenance instructions.
That skill is for downstream library users who want to generate or understand
CommandLib usage code.

Prefer focused Gradle verification for touched modules. Do not rewrite unrelated
code, generated files, or user changes.
