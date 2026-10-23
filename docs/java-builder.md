# Java Builder

`Command.builder(name)` configures a command in one chain, without a subclass or an initializer block, and `build()`
returns the same `Command` the fluent API makes. The tree, the help, the permission nodes and the behavior are the same,
so a command can move between the styles without changing what players see or what its tests assert.

Every platform's `Command` (`spigot`, `paper`, `velocity`, `modded`) has `builder(name)`; it returns a
`CommandBuilder<CommandContext, Command>` from `common`.

```java
CommandOption<Boolean, CommandContext> silent = Options.<CommandContext>flag("silent", 's')
                                                       .description("Do not tell the target");

Command heal = Command.builder("heal")
                      .description("Heal a player")
                      .permission(DefaultPermission.OP, "Heal players")
                      .addAliases("h")
                      .option(silent)
                      .require(Command.playerRequirer(),
                               branch -> branch.execute((player, ctx) -> player.setHealth(20)))
                      .argument(new PlayerArgument("target"), new IntegerArgument("amount"),
                                branch -> branch.description("Heal by an amount")
                                                .execute((target, amount, ctx) -> {
                                                    target.setHealth(Math.min(20, target.getHealth() + amount));
                                                    if (!ctx.getOption(silent)) {
                                                        target.sendMessage("You were healed");
                                                    }
                                                }))
                      .addChild(Command.builder("all")
                                       .permission(DefaultPermission.ALL)
                                       .execute(ctx -> Bukkit.getOnlinePlayers().forEach(p -> p.setHealth(20))))
                      .build();

CommandLib.register(this, heal);
```

is the same command as

```java
Command heal = new Command("heal") {{
    description("Heal a player");
    permission(DefaultPermission.OP, "Heal players");
    addAliases("h");
    CommandOption<Boolean, CommandContext> silent = option(Options.<CommandContext>flag("silent", 's')
                                                                  .description("Do not tell the target"));
    require(Command.playerRequirer()).execute((player, ctx) -> player.setHealth(20));
    argument(new PlayerArgument("target"), new IntegerArgument("amount")).description("Heal by an amount")
                                                                         .execute((target, amount, ctx) -> { ... });
    addChildren(new Command("all") {{ ... }});
}};
```

## Methods

The builder has the configuration methods of `Command` under the same names, each returning the builder:
`description`, `serverHelp`, `permission`, `permissionDescription`, `addAliases`, `option`, `mutuallyExclusiveOptions`,
`addPrerequisite`, `addPreprocess`, `addMiddleware`, `provide`, `confirm`, `cooldown`, `execute`, `executeAsync`,
`addUncaughtExceptionHandler`, `addChildren`, `disableParentPrerequisite` and `disableParentMiddleware`. They behave as
described for the fluent API ([permissions](permissions.md), [options](options.md), [execution](execution.md),
[context values](context-values.md)).

The methods that return a branch on `Command` take a callback for it instead:

| `Command` | `CommandBuilder` |
|---|---|
| `argument(a, b).execute((a, b, ctx) -> ...)` | `.argument(a, b, branch -> branch.execute((a, b, ctx) -> ...))` |
| `argument(a).child(ref -> ...)` | `.argument(a, branch -> branch.child(ref -> ...))` |
| `require(extractor).execute((value, ctx) -> ...)` | `.require(extractor, branch -> branch.execute((value, ctx) -> ...))` |
| `option(option)` returns the option typed for the context | `.option(option)` returns the builder; keep the option you passed |
| `addChildren(new Command("x") {{ ... }})` | `.addChild(Command.builder("x")...)`, or `.addChildren(command)` |

The callback receives the same typed branch as the fluent API, so the executor gets one typed parameter per argument
for up to seven arguments, and a child command factory gets an `ArgRef<T>` per argument:

```java
Command config = Command.builder("config")
                        .argument(new StringArgument("key"),
                                  branch -> branch.child(key -> Command.builder("get")
                                                                       .execute(ctx -> ctx.sendMessage(key.get()))
                                                                       .build()))
                        .build();
```

Platform methods that are not on the common command, such as `requirePlayer()`, `requireConsole()` or
`permission(PermissionDefault)` on Spigot and Paper, go through `configure`:

```java
Command.builder("fly")
       .configure(Command::requirePlayer)
       .configure(c -> c.permission(PermissionDefault.TRUE))
       .execute(ctx -> ...)
       .build();
```

The builder configures its command as it goes: `build()` returns that command, and the builder is not used after it.
`CommandBuilder.of(command)` wraps a command made another way, such as an instance of a subclass.

## Eight or more arguments

The typed callbacks take one to seven arguments. A branch of a list of arguments reads each value typed from its
argument instance with `ctx.getArgument(argument)`, without a name lookup or a cast:

```java
DoubleArgument x1 = new DoubleArgument("x1"), y1 = new DoubleArgument("y1"), z1 = new DoubleArgument("z1");
DoubleArgument x2 = new DoubleArgument("x2"), y2 = new DoubleArgument("y2"), z2 = new DoubleArgument("z2");
WorldArgument world = new WorldArgument("world");
StringArgument name = new StringArgument("name");

// Fluent
argument(List.of(x1, y1, z1, x2, y2, z2, world, name)).execute(ctx -> {
    double width = Math.abs(ctx.getArgument(x2) - ctx.getArgument(x1));
    regions.create(ctx.getArgument(name), ctx.getArgument(world), width);
});

// Builder
Command.builder("region")
       .argument(List.of(x1, y1, z1, x2, y2, z2, world, name), branch -> branch.execute(ctx -> { ... }))
       .build();
```

`argument(List)` also takes the arguments of a branch built at runtime. A child command of such a branch reads
the parent's values with `ArgRef.of(argument).get()` or `ctx.getArgument(argument)`.

## Testing

A builder command is a `Command`, so the testers take it as any other: `new CommandTester(() -> Command.builder("heal")
... .build(), "myplugin.command")`, and `assertCommandTreeMatchesSnapshot` gives the same snapshot as the fluent
definition. See [testing](testing.md).
