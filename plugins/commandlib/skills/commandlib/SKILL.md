---
name: commandlib
description: Generate, test, or explain code that uses the CommandLib library in a Bukkit plugin. Usage: /commandlib:commandlib <request>
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

Run the extraction script:

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/extract-sources.sh"
```

The script prints one of:

- A JAR path: sources were extracted to `/tmp/commandlib-sources/`
- `LOCAL_BUILD`: CommandLib source tree is available locally at the project root

## Step 3 - Read key source files if API detail is needed

If extracted JAR, use `/tmp/commandlib-sources/` as the base path. If
`LOCAL_BUILD`, use the project root as the base path.

Read the files relevant to the user's request. Common public API files include:

- `README.md`
- `bukkit/src/main/java/net/kunmc/lab/commandlib/CommandLib.java`
- `bukkit/src/main/java/net/kunmc/lab/commandlib/Command.java`
- `bukkit/src/main/java/net/kunmc/lab/commandlib/CommandContext.java`
- `common/src/main/java/net/kunmc/lab/commandlib/CommonCommand.java`
- `common/src/main/java/net/kunmc/lab/commandlib/DefaultPermission.java`
- `common/src/main/java/net/kunmc/lab/commandlib/CommandOption.java`
- `common/src/main/java/net/kunmc/lab/commandlib/Options.java`
- `common/src/main/java/net/kunmc/lab/commandlib/AbstractArgumentBuilder.java`
- `common/src/main/java/net/kunmc/lab/commandlib/branch/ArgumentBranch.java`
- `bukkit/src/main/java/net/kunmc/lab/commandlib/ArgumentBuilder.java`
- `common/src/main/java/net/kunmc/lab/commandlib/CommonArgument.java`
- `bukkit/src/main/java/net/kunmc/lab/commandlib/argument/`

For extracted JAR sources, map those paths to the extracted package layout.

## Step 4 - Respond to the request

Respond in the same language as the user's request. If asked to generate code,
output a complete working implementation that follows the public CommandLib API.
If asked how to do something, explain with a code example.

$ARGUMENTS
