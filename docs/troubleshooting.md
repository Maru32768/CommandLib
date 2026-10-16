# Troubleshooting

## The command is "Unknown or incomplete" for some players

The sender lacks the permission node of the command or of the branch they typed. Nodes the sender may not use are
removed from their command tree and parsed as if they did not exist. Check the node names in
[permissions](permissions.md): every step of the route needs its node, for example `/config <key> get` needs
`<prefix>.config`, `<prefix>.config.key`, and `<prefix>.config.get`. Root commands default to operators only; use
`permission(DefaultPermission.ALL)` for commands everyone may use.

## A LuckPerms grant has no effect on Forge / NeoForge

On Forge 1.18+ and NeoForge, CommandLib's nodes live in the `commandlib` namespace. Grant
`commandlib.<prefix>.<path>`, for example `commandlib.mymod.command.spawn`, not `mymod.command.spawn`.

On Fabric the node has no namespace (`mymod.command.spawn`), and LuckPerms answers only when a mod ships
fabric-permissions-api. Without it, the node's default decides.

## Operators cannot use an `OP` command on Forge / NeoForge / Fabric

`DefaultPermission.OP` resolves to permission level 4 there. An operator with a lower `op-permission-level` does not get
it by default. Grant the node, or use `DefaultPermission.ALL` with your own check.

## A granted command does not show up until the player rejoins

CommandLib does not resend the command tree when permissions change. Call `player.updateCommands()` (Bukkit) after
changing permissions in code. LuckPerms on Bukkit does this itself unless its `update-client-command-list` option is
off.

## Typing `/command` shows a usage list instead of running

The command has argument branches or children but no executor of its own, so the bare input sends the generated help.
Add `execute(ctx -> ...)` to the command for the no-argument form. The same happens for a partial branch such as
`/msg Steve` when the branch is `<target> <text>`.

## Options are rejected

Options go right after the command or subcommand name and before regular arguments: `/scan -f Steve`, not
`/scan Steve -f`. Value options use a space: `--limit 20`, not `--limit=20`. Options of a subcommand go after the
subcommand: `/game start -f arena`. See [options](options.md).

## An argument after `UnparsedArgument` gets no tab completion

`UnparsedArgument` is advertised to the client as a greedy string so that the client accepts any characters, which
makes the client stop completing after it. The server still parses the following arguments. Put it last.

## A `Location` from `LocationArgument` is off by 0.5

Integer x and z coordinates are centered, like vanilla commands (`10 64 10` → `10.5 64 10.5`). Use `getBlockX()` /
`getBlockZ()` for block coordinates, `new LocationArgument(name, false)` to keep integers, or
`BlockPositionArgument` for a block position.

## The executor of `PlayersArgument` gets an empty list on Paper

Paper's selector resolver can return no players where Spigot reports `No player was found`. Check `isEmpty()` when
the plugin runs on both. See [arguments](arguments.md#entities-and-players).

## "An unexpected error occurred trying to execute that command"

The executor threw an exception. The stack trace is in the server console. To report it elsewhere, add an
`addUncaughtExceptionHandler((e, ctx) -> ...)` to the command. See [errors](errors.md#unexpected-exception).

## Commands from data pack functions fail to load

Functions are compiled before plugins enable on Spigot and Paper. CommandLib compiles the functions that use its
commands again after it registers them, and on later `/minecraft:reload`s, so the earlier "Unknown or incomplete
command" load error of such a function can be ignored. On Paper, registering from a `PluginBootstrap` with
`CommandLib.bootstrap(...)` makes the commands exist before the first compilation.

## Java 26 warns that a final field "has been mutated reflectively"

On Spigot and Paper, CommandLib swaps the children maps of Brigadier command nodes when it registers or unregisters
commands, because Paper reads those maps on another thread. The maps are in `final` fields. From Java 26 the JVM warns
on the first such write, and a JVM that denies it (`--illegal-final-field-mutation=deny`, planned as the default of a
later release) makes registration fail. Start the server with `--enable-final-field-mutation=ALL-UNNAMED` to allow
the write and silence the warning.

## Two plugins shading CommandLib conflict

Relocate `dev.marulabs.commandlib` in the shadow jar (see the README installation snippets). Each plugin then has its
own copy.

## Tests fail with `UnsupportedOperationException` or `IllegalStateException: No active CommandTester`

Build the command inside the tester with the `Supplier` constructor: `new CommandTester(MyCommand::new, prefix)`.
Platform argument types are only available while the tester builds the commands. For arguments that call `Bukkit`
statics, mock them in the same try-with-resources. See [testing](testing.md).

## Tests see `command.unknown.argument` instead of the English text

`paper-testing` returns translation keys for translatable messages. Assert on the key there, or on the English text
with `spigot-testing` and `modded-testing`.
