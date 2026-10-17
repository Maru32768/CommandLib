# Vanilla Command Integration

CommandLib registers its commands in the server's Brigadier dispatcher, the one vanilla commands live in. They can
therefore be run by everything that runs vanilla commands: `/execute ... run`, command blocks, data pack functions and
function tags. This page describes what a command sees in each case and where the platforms differ.

The integration fixtures check these cases on real servers (`VanillaExecutionTest` and `DataPackFunctionTest` in
`integration-test/shared/spigot` and `integration-test/shared/paper`, `ActorTest` in `integration-test/shared/modded`).
[Compatibility](compatibility.md) lists the targets. On 2026-10-17 (commit `89b9503f`) every case passed on all 78
integration targets, which cover each minor version from 1.16 to 26.3.

On every platform a command's executor runs during the dispatch when it is called on the server thread, so a command
that `/execute`, a function or another command runs has finished before the next one, also while the server handles a
player's command packet. Only the work an `executeAsync` command hands to the async executor finishes later.

## `/execute`

`/execute` changes the source of the command it runs. CommandLib reads the changed source, so the subcommands of
`/execute` reach the command context:

| Subcommand | Changes | Read with |
|---|---|---|
| `as <entity>` | The executing entity | `ctx.getEntity()` |
| `at <entity>`, `positioned <pos>`, `in <dimension>` | The position and world | `ctx.getLocation()`, `ctx.getWorld()` |
| `rotated <yaw> <pitch>`, `facing ...`, `at <entity>` | The rotation | the yaw and pitch of `ctx.getLocation()` |
| `store result` / `store success` | What is stored | `ctx.setResult(int)`; a failure with a message stores 0, one without a message only returns 0 (see [errors](errors.md#results-and-failures)) |

`execute positioned 10.25 70.5 -20.75 rotated 90 45 run mycmd` gives a location of `10.25, 70.5, -20.75` with yaw 90
and pitch 45 on every platform. Note that `positioned` centers whole numbers on the block, as vanilla does:
`positioned 10 70 -20` is `10.5, 70, -19.5`.

Selector arguments (`EntityArgument`, `PlayerArgument`, and so on) use the vanilla selector parser, so they resolve from
the changed source: `execute as Steve run mycmd @s` targets Steve, and `@e[distance=..5]` is measured from the position
that `at` or `positioned` set. The parser also keeps vanilla's rule that a single-entity argument such as
`EntityArgument` rejects a selector that may match several entities, so `@e` needs `limit=1` there.

### Sender and actor

The platforms disagree on who the sender is after `execute as`:

| Platform | `ctx.getSender()` | `ctx.getActor()` | `ctx.getEntity()` |
|---|---|---|---|
| Spigot | The original sender, such as the console | The original sender (`CONSOLE`) | The entity of `as` |
| Paper | The original sender (`CommandSourceStack#getSender`) | The original sender (`CONSOLE`) | The entity of `as` (`CommandSourceStack#getExecutor`) |
| Forge / NeoForge | The `CommandSourceStack` itself | The entity of `as` (`PLAYER`), named after it | The entity of `as` |

On Bukkit the sender is the `CommandSender` that started the command, so `ctx.getSender()`, `ctx.getPlayer()` and
permission checks keep using the console when the console runs `execute as Steve run mycmd`. Use `ctx.getEntity()` for
the entity the command runs as. On Forge and NeoForge the actor is built from the source, whose entity and name
`execute as` replaces, so the actor's type, name and permission checks follow the executing entity.

## Command blocks

A command block runs its command with a source at the center of the block:

- On Spigot and Paper the sender is a `BlockCommandSender`, `ctx.getActor().getType()` is `COMMAND_BLOCK`, and
  `ctx.getLocation()` is in the block (`getBlockX()` and so on are the block's coordinates).
- The block's `SuccessCount` counts the successful runs of its command, not the result the command sets: a command that
  calls `ctx.setResult(42)` leaves `SuccessCount` at 1, and one that fails leaves 0. Run the command through
  `/execute store result ...` to keep the result. This is vanilla behavior: the block adds one for each successful
  result callback.
- Spigot 1.20.3 stores 0 from `/execute store result` and leaves `SuccessCount` at 0 for every command, vanilla commands
  included. CommandLib commands behave the same there.
- Paper 1.20.5 does not tick the spawn chunks (its `spawnChunkRadius` is 0), so a command block there runs only in a
  chunk a player keeps loaded or one added with `/forceload`.
- A failure's messages become the block's `LastOutput`, like a vanilla command's error.
- Up to 1.21.8 the server runs command blocks only with `enable-command-block=true` in `server.properties`. Later
  servers (checked on 1.21.11 and 26.3) read a game rule instead, `command_blocks_work` on 26.x, which is true by
  default.

The Forge, NeoForge and Fabric fixture checks that a command block's actor type is `COMMAND_BLOCK` and that a command
block cannot run a command it lacks the permission for. It does not check `SuccessCount` or `LastOutput`. The actor
type is detected from the source there (`ForgeCommandActor`).

## Data pack functions and tags

A function is compiled when the data packs load: at startup, on `/minecraft:reload`, `/datapack enable|disable` and
`Bukkit#reloadData`. Each line is parsed against the dispatcher then, and a function with a line that does not parse is
dropped.

- **Forge / NeoForge** add the commands on `RegisterCommandsEvent`, which the server fires before it compiles the
  functions, so functions see them without help from CommandLib.
- **Spigot** compiles the functions before plugins can add commands, and has no event for a data reload. CommandLib
  checks every tick whether the data pack resources were replaced or its commands changed, and then compiles the
  functions and function tags again with its commands.
- **Paper** fires the `COMMANDS` lifecycle event after the functions were compiled, at startup and on every reload.
  CommandLib compiles them again on the next tick.

The server's own compilation reports every function that uses a CommandLib command as `Failed to load function ...`,
and every function tag that lists one as `Couldn't load tag ... as it is missing following references: ...`. Both
errors are dropped while CommandLib is going to compile again, and the errors of CommandLib's own compilation are still
logged, so a function that is really broken is reported once.

Function tags, such as `function #mypack:setup`, run the commands like functions do, since the recompilation rebuilds
the tags. `#minecraft:load` runs once after a reload, with the commands: the server runs it on the tick after the
reload, after the Bukkit tasks of that tick, which compile the functions first. A reload that a plugin task runs, such
as `Bukkit.dispatchCommand(console, "minecraft:reload")`, is applied after that tick's compilation, and the server runs
`#minecraft:load` in the same tick with its own compilation, which dropped the tag. On Spigot the next tick's
compilation then runs the tag once, when the server's compilation lacked it. A later recompilation, such as the one
after a plugin registers a command at runtime, does not run `#minecraft:load` again. `#minecraft:tick` uses the
recompiled functions from the same tick on.

A command run by a function is silent for `sendSuccess`, like vanilla commands, and its failure messages go to the
function's source, not to a player. A function runs with the source that started it, at permission level 2:
`/function` run by the console has the console's source, and `#minecraft:load` and `#minecraft:tick` have the server's.

Commands registered after a data load are usable in functions from the next compilation: on Spigot from the next tick,
on Paper from the next tick for a plugin registered at runtime, and from the server's own compilation for a
registration in a `bootstrap`.

## Platform differences

| Behavior | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|
| Sender after `execute as` | Original sender | Original sender | The source, whose entity changed |
| Actor after `execute as` | Original sender | Original sender | The entity |
| Executing entity | `getEntity()` | `getEntity()` | `getEntity()` |
| Location and rotation | `getLocation()`, nullable when the source has no world | `getLocation()` | `getLocation()` |
| Functions using the commands | Compiled again by CommandLib every tick the resources change | Compiled again by CommandLib on the next tick | Native |
| Command block sender | `BlockCommandSender` | `BlockCommandSender` | `CommandSourceStack` of the block |
