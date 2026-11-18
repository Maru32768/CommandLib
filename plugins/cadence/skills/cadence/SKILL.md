---
name: cadence
description: Generate, test, or explain code that uses the Cadence library in a Spigot/Paper/Velocity plugin or Forge/NeoForge/Fabric mod. Usage: /cadence:cadence <request>
allowed-tools: Bash Read Glob Grep
---

## Purpose

Use this skill when the user is writing a plugin or library that depends on
Cadence.

Do not use this skill for maintaining the Cadence repository itself unless
the user explicitly asks for downstream usage examples.

## Step 1 - Read downstream usage references

Read the relevant references before generating code:

- `${CLAUDE_SKILL_DIR}/references/user-guide.md`
- `${CLAUDE_SKILL_DIR}/references/patterns.md`
- `${CLAUDE_SKILL_DIR}/references/testing.md`

## Step 2 - Extract Cadence sources when needed

Run the extraction script, passing the Cadence artifact the user's project
depends on (`spigot`, `paper`, `velocity`, `forge-1.20.1`, ...; defaults to
`spigot`) and, when the build file names one, its Cadence version (defaults
to the newest in the Gradle cache):

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/extract-sources.sh" spigot 0.18.0
```

The script prints one of:

- A JAR path: sources were extracted to `/tmp/cadence-sources/`
- `NOT_FOUND`: no Cadence sources JAR is in the Gradle cache

## Step 3 - Read key source files if API detail is needed

If extracted JAR, use `/tmp/cadence-sources/` as the base path. If
`NOT_FOUND`, use the project root only when it is a Cadence checkout
(`common/src/main/java/dev/marulabs/cadence` exists there); otherwise rely on
the references above and ask the user to download the sources (see the
Cadence README, "Claude Code Skill").

Read the files relevant to the user's request. Common public API files include:

- `README.md`
- `spigot/src/main/java/dev/marulabs/cadence/Cadence.java`
- `spigot/src/main/java/dev/marulabs/cadence/Command.java`
- `spigot/src/main/java/dev/marulabs/cadence/CommandContext.java`
- `common/src/main/java/dev/marulabs/cadence/CommonCommand.java`
- `common/src/main/java/dev/marulabs/cadence/DefaultPermission.java`
- `common/src/main/java/dev/marulabs/cadence/CommandOption.java`
- `common/src/main/java/dev/marulabs/cadence/Options.java`
- `common/src/main/java/dev/marulabs/cadence/branch/ArgumentBranch.java`
- `common/src/main/java/dev/marulabs/cadence/CommandBuilder.java`
- `common/src/main/java/dev/marulabs/cadence/CommonArgument.java`
- `spigot/src/main/java/dev/marulabs/cadence/argument/`

Paper, Velocity and Forge/NeoForge/Fabric projects use `paper/`, `velocity/`
or `modded/` in place of `spigot/`. For extracted JAR sources, map those paths to the extracted package layout.

## Step 4 - Respond to the request

Respond in the same language as the user's request. If asked to generate code,
output a complete working implementation that follows the public Cadence API.
If asked how to do something, explain with a code example.

$ARGUMENTS
