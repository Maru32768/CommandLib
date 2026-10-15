# Command Options

Options are named modifiers such as `-f`, `--force` or `--limit 10`, given between the command name and its
positional arguments. Each option is a typed `CommandOption<T, C>`: register it with `option(...)` and read it with
`ctx.getOption(option)`, which returns `T` without casts.

```java
public final class ScanCommand extends Command {
    public ScanCommand() {
        super("scan");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                     .aliases("yes")
                                                                     .description("Run even if safety checks fail."));
        CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3)
                                                                       .description("More progress messages."));
        CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                     .description("Maximum number of targets."));
        CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                         .repeatable(3)
                                                                         .description("Tag to match."));
        CommandOption<Boolean, CommandContext> json = option(Options.flag("json", 'j'));
        CommandOption<Boolean, CommandContext> yaml = option(Options.flag("yaml", 'y'));
        mutuallyExclusive(json, yaml);

        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            boolean isForce = ctx.getOption(force);
            int verbosity = ctx.getOption(verbose);
            int maxCount = ctx.getOption(limit);
            List<String> tagList = ctx.getOption(tags);
            boolean limitWasGiven = ctx.hasOption(limit);
        });
    }
}
```

```text
/scan Steve
/scan -f -n 20 Steve
/scan --yes -vv -t red --tag blue Steve
/scan -fvj Steve
```

## Kinds

| Factory or modifier                    | `getOption` type | Not given      | Input                              |
|----------------------------------------|------------------|----------------|------------------------------------|
| `Options.flag(name[, short])`          | `Boolean`        | `false`        | `--force`, `-f`                    |
| `Options.count(name[, short], max)`    | `Integer`        | `0`            | `-vvv`, `-v --verbose`             |
| `Options.bool/integer/longValue/...`   | the value type   | the default    | `--limit 20`, `-n 20`              |
| `Options.of(name[, short], argument)`  | the argument's   | see below      | `--target Steve`, `-t @p`          |
| `valueOption.repeatable(max)`          | `List<T>`        | empty list     | `--tag a -t b`                     |

The value factories are `bool`, `integer` (optionally with a range), `longValue`, `floatValue`, `doubleValue` and
`string`. Each takes the default value returned when the option is not given, and has an overload with a `char`
short name.

`Options.of` takes any CommandLib argument as the value, with the argument's type, suggestions, validator and
transformer:

```java
public final class HealCommand extends Command {
    public HealCommand() {
        super("heal");

        CommandOption<Player, CommandContext> target = option(Options.of("target", 't',
                                                                         new PlayerArgument("target").optional(playerRequirer())));
        CommandOption<World, CommandContext> world = option(Options.of("world", new WorldArgument("world")));
        execute(ctx -> {
            Player player = ctx.getOption(target);    // the sender when -t is not given
            World in = ctx.getOption(world);          // required: the argument is not optional
        });
    }
}
```

When the option is not given, the default of an `optional(...)` argument applies (computed for each execution, so
`playerRequirer()` works); the option of an argument that is not optional is `required()`. A value the argument rejects
fails like a positional argument. A variadic argument is rejected; use `repeatable(max)`.

- A value option takes one value token after its name: `--limit 20` or `-n 20`. `--limit=20` and `-n20` are not
  supported.
- Flags and counts with short names can be combined in one token: `-fv`, `-vf`. A count can repeat its short name:
  `-vvv`. Value options always stand alone.
- `repeatable(max)` turns a value option into one that can be given up to `max` times. The values come in input
  order. Call it before `requires(...)`, since the returned option only keeps the aliases and the description.
- `Options.count` and `repeatable` take a maximum because the command tree has a branch for each further occurrence
  (see [Limits](#limits)).

## Aliases

`aliases(String...)` adds long names. Every alias works like `--name`, is suggested, and is listed in help:

```java
public final class ExampleCommand extends Command {
    public ExampleCommand() {
        super("example");

        option(Options.flag("force", 'f')
                      .aliases("yes"));    // --force, --yes, -f
    }
}
```

An option has at most one short name. Names and aliases must be unquoted Brigadier words (`0-9`, `A-Z`, `a-z`, `_`,
`-`, `.`, `+`), and no two options of a command may share a long name, an alias or a short name. Set the aliases before
registering the option with `option(...)`.

## Required Options

`required()` makes the command fail when the option is not given:

```java
public final class DeployCommand extends Command {
    public DeployCommand() {
        super("deploy");

        CommandOption<String, CommandContext> env = option(Options.string("env", 'e', "")
                                                                  .required());
        argument(new StringArgument("app")).execute((app, ctx) -> {
            String environment = ctx.getOption(env);    // always given
        });
    }
}
```

```text
/deploy -e prod api      ok
/deploy api              Missing required option --env.
```

The default value of a required option is never returned. Input that ends where the command has no executor, such as
`/deploy` above, shows help instead of asking for the option.

## Permissions

`permission(DefaultPermission)` gives an option its own node, `<command node>.option.<name>`; `permission(node)` and
`permission(node, DefaultPermission)` use an exact node instead, with the command's default unless one is given. A
sender without the node does not see the option in the command tree, in suggestions or in help, cannot give it (also
not inside a combined token such as `-fs`), and the runner checks it again before the command runs. The node is
registered and exported with the command's other nodes; see [permissions](permissions.md#options).

```java
public final class BanCommand extends Command {
    public BanCommand() {
        super("ban");

        option(Options.flag("silent", 's')
                      .permission(DefaultPermission.OP));    // myplugin.command.ban.option.silent
    }
}
```

## Mutually Exclusive Options

`mutuallyExclusive(a, b, ...)` on the command allows at most one of the options in one input. Once one is given, the
command tree stops offering the others, so they are neither suggested nor accepted, also inside combined short flags
such as `-jy`. All options must be registered on the command first. A command can declare several groups.

## Dependencies

An option can depend on another option when it is given:

```java
public final class ExportCommand extends Command {
    public ExportCommand() {
        super("export");

        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
        CommandOption<String, CommandContext> reason = option(Options.string("reason", 'r', "")
                                                                     .requires(force));
        CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
        CommandOption<Integer, CommandContext> threads = option(Options.integer("threads", 't', 1, 1, 16)
                                                                       .requires(mode, "parallel"));
        CommandOption<Integer, CommandContext> depth = option(Options.integer("depth", 'd', 1));
        CommandOption<Boolean, CommandContext> deep = option(Options.flag("deep", 'D')
                                                                    .requires(depth, x -> x >= 3, "at least 3"));
    }
}
```

`requires(other)` needs `other` to be given. `requires(other, value)` and `requires(other, predicate, description)`
check `ctx.getOption(other)`, so the default value of `other` can satisfy them. The checks run only when the option
itself is given.

## Errors

Option errors are separate from argument errors:

- Brigadier rejects input the command tree does not accept before the command runs: a malformed or out-of-range
  value (`--limit abc`), a value option without its value, an option given more often than allowed, an option of a
  mutually exclusive group after another one, or an option after a positional argument. The client marks these
  inputs as it marks any incorrect command. An unknown option such as `--bogus` is rejected the same way unless the
  first positional argument accepts it as a value, as a word string argument does.
- A missing required option and an unmet `requires(...)` throw `OptionParseException`, whose messages are sent to the
  sender as failures. The command body does not run. Positional arguments fail with `ArgumentParseException` instead.

## Help

The generated help puts `[options]` in each usage line and lists every option in its own `Options:` section, with the
short name, long name and aliases, the value tag of value options, `(required)` and `(up to N times)` notes, and the
description:

```text
Usage:
  /tag [options] <target>

Options:
  -f, --force, --yes: Skip confirmation
  -v, --verbose (up to 3 times): More output
  -t <tag>, --tag <tag> (required, up to 5 times): Tag to add
```

## Subcommands

Options belong to the command that declares them and are placed after that command's name. A child command does not
inherit the options of its parent:

```text
/game start -f arena1      ok, -f belongs to start
/game -f start arena1      rejected
```

## Limits

Options are literal nodes in the Brigadier command tree, so the client can suggest and check them. The tree has a
branch for each set of options already given; the tokens that reach the same set redirect to one branch, so neither the
server nor the players get a copy for every order of the options. Each set still offers every token of the options left,
and combined short flags are a token for each order of the flags (`-fv`, `-vf`), so keep the number of options per
command and the maximums of counts and repeatable options small: a few options with maximums of 3 to 5 are fine, and
six short flags already make about 5,500 nodes.

An argument child (a command under an argument) keeps a tree without redirects, which players receive with a copy for
every order of the options. Keep its options fewer still.

Options cannot follow positional arguments, and one occurrence of an option takes exactly one value.
