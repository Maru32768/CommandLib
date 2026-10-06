# CommandLib Repository Notes

## Scope

This document is for agents editing the CommandLib repository itself. The
Claude Code skill under `.claude/skills/commandlib` is for downstream library
users and should not be used as the main source of repository maintenance
instructions.

## Project Structure

- `common`: shared command model, arguments, options, parsing, and utilities.
- `spigot`: Spigot-facing API, registration, Bukkit argument types, and NMS
  bridges.
- `paper`: Paper-facing API using Paper official command/lifecycle API and
  Adventure components. Targets Paper 1.20.6+.
- `spigot-testing`: test utilities for downstream plugins and this repository.
- `modded-testing`: test utilities for downstream Forge / NeoForge mods, built
  per loader and Minecraft version like `modded`.
- `integration-test`: Minecraft integration tests. Current targets are
  Bukkit-compatible plugin fixtures; Forge fixtures may be added separately.
- `modded`: Forge- and NeoForge-facing integration. One shared source tree in
  `modded/src` builds every supported loader and Minecraft version
  (`:modded:forge-1.16.5`, `:modded:forge-1.20.1`, `:modded:neoforge-1.21.1`)
  through Stonecutter conditional comments and Architectury Loom. Read
  `docs/agents/modded.md` before editing it.
- `sample`: sample usage.

Prefer the existing module boundaries. Keep shared behavior in `common` when it
does not depend on Bukkit APIs. Keep Bukkit and NMS-specific behavior in
`spigot`.

## Implementation Guidance

- Follow the public API style already used in the surrounding code.
- Keep changes scoped to the behavior being fixed or added.
- Avoid broad formatting-only edits.
- Preserve binary/source compatibility unless the task explicitly calls for an
  API break.
- Prefer typed APIs over stringly-typed lookups when adding new public examples
  or tests.
- When adding or changing public argument classes, follow
  `docs/agents/argument-addition.md`.
- For Bukkit/NMS behavior, check both the public wrapper and the mock/test
  support paths before changing constructor or registry behavior.

## Public Usage Patterns

When writing examples or tests that represent downstream user code, prefer the
same patterns documented in `.claude/skills/commandlib/references/patterns.md`.
Those patterns are user-facing API guidance, not repository architecture rules.
