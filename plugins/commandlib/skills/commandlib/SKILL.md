---
name: commandlib
description: Generate, test, or explain code that uses the CommandLib library in a Spigot/Paper/Velocity plugin or Forge/NeoForge/Fabric mod. Usage: /commandlib:commandlib <request>
allowed-tools: Bash Read Glob Grep
---

## Purpose

Use this skill when the user is writing a plugin or library that depends on
CommandLib.

Do not use this skill for maintaining the CommandLib repository itself unless
the user explicitly asks for downstream usage examples.

## Step 1 - Read downstream usage references

Read the relevant references before generating code:

- `${CLAUDE_SKILL_DIR}/references/user-guide.md`
- `${CLAUDE_SKILL_DIR}/references/patterns.md`
- `${CLAUDE_SKILL_DIR}/references/testing.md`

## Step 2 - Extract CommandLib sources when needed

Run the extraction script, passing the CommandLib artifact the user's project
depends on (`spigot`, `paper`, `velocity`, `forge-1.20.1`, ...; defaults to
`spigot`) and, when the build file names one, its CommandLib version (defaults
to the newest in the Gradle cache):

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/extract-sources.sh" spigot 0.18.0
```

The script prints one of:

- A JAR path: sources were extracted to `/tmp/commandlib-sources/`
- `NOT_FOUND`: no CommandLib sources JAR is in the Gradle cache

## Step 3 - Read key source files if API detail is needed

If extracted JAR, use `/tmp/commandlib-sources/` as the base path. If
`NOT_FOUND`, use the project root only when it is a CommandLib checkout
(`common/src/main/java/dev/marulabs/commandlib` exists there); otherwise rely on
the references above and ask the user to download the sources (see the
CommandLib README, "Claude Code Skill").

Read the files relevant to the user's request. Common public API files include:

- `README.md`
- `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java`
- `spigot/src/main/java/dev/marulabs/commandlib/Command.java`
- `spigot/src/main/java/dev/marulabs/commandlib/CommandContext.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `common/src/main/java/dev/marulabs/commandlib/DefaultPermission.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandOption.java`
- `common/src/main/java/dev/marulabs/commandlib/Options.java`
- `common/src/main/java/dev/marulabs/commandlib/branch/ArgumentBranch.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandBuilder.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonArgument.java`
- `spigot/src/main/java/dev/marulabs/commandlib/argument/`

Paper, Velocity and Forge/NeoForge/Fabric projects use `paper/`, `velocity/`
or `modded/` in place of `spigot/`. For extracted JAR sources, map those paths to the extracted package layout.

## Step 4 - Respond to the request

Respond in the same language as the user's request. If asked to generate code,
output a complete working implementation that follows the public CommandLib API.
If asked how to do something, explain with a code example.

$ARGUMENTS
