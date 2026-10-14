# Suggestions

Suggestions are the completions a player sees while typing an argument. Every CommandLib argument keeps an ordered list
of suggestion providers; when the client asks for completions, CommandLib runs them all and sends the combined result.

## Sources of suggestions

An argument starts with one provider: the **default suggestions of its argument type**. For arguments backed by a
vanilla or Paper type (players, entities, items, blocks, enchantments, worlds, Paper registry arguments, ...) these are
the native suggestions. Brigadier's primitive and string types have none, except `true` / `false` for booleans.

Many built-in arguments add their own provider on top, for example `EnumArgument` (constant names in lower case),
`LiteralArgument`, `ObjectArgument`, `NameableObjectArgument`, `OfflinePlayerArgument`, `UUIDArgument`,
`ObjectiveArgument`, Paper's `TeamArgument`, and the Spigot registry-backed arguments (registry keys).

Change the list with these methods on the argument:

| Method | Effect |
|---|---|
| `addSuggestionProvider(provider)` | Appends a provider; existing suggestions stay. |
| `setSuggestionProvider(provider)` | Replaces every provider, including the default suggestions, with this one. |
| `clearSuggestionProviders()` | Removes every provider; the argument suggests nothing. |
| `useDefaultSuggestions()` | Adds the argument type's default suggestions back if they are not in the list. |

```java
class WarpCommand extends Command {
    WarpCommand(WarpStore warps) {
        super("warp");

        argument(new StringArgument("name", StringArgument.Type.WORD).setSuggestionProvider(sb -> {
            String typed = sb.getLatestInput();
            warps.names()
                 .stream()
                 .filter(name -> name.startsWith(typed))
                 .forEach(sb::suggest);
        })).execute((name, ctx) -> ctx.sendMessage("Warping to " + name));
    }
}
```

`WarpStore` stands for your own code.

## The suggestion builder

A provider is a `SuggestionProvider<C>`, a `Consumer<SuggestionBuilder<C>>`.

| `SuggestionBuilder` method | Purpose |
|---|---|
| `getLatestInput()` | The text typed so far for the argument being completed. Empty right after a space. |
| `getInput()` | The whole input line. |
| `getArgument(name)`, `getArgument(name, Class)`, `getInput(name)` | Earlier arguments of the same command path, already parsed. |
| `getContext()` | The command context: sender, platform helpers. |
| `suggest(text)` | Adds a completion. |
| `suggest(text, tooltip)` | Adds a completion with a plain-text tooltip. |
| `suggest(text, message)` | Adds a completion with a Brigadier `Message` tooltip. |
| `await(stage)` | Delays the response until the `CompletionStage` completes; see [Asynchronous suggestions](#asynchronous-suggestions). |

CommandLib does not filter what a provider adds: suggestions reach the client as given. Filter by `getLatestInput()`
yourself. The built-in providers use case-insensitive "contains" matching, while vanilla suggestions use prefix
matching.

### Reading earlier arguments

Before the providers run, CommandLib parses the arguments that come before the one being completed: earlier arguments
of the same branch and the arguments of parent branches for [argument child commands](../README.md). This is
best-effort: an argument that fails to parse is left out, and `getArgument(name)` then throws
`IllegalArgumentException`. Catch it when an earlier value may be invalid (`getInput(name)` returns `""` instead).

```java
argument(new WorldArgument("world"), new StringArgument("region", StringArgument.Type.WORD).setSuggestionProvider(sb -> {
    World world;
    try {
        world = sb.getArgument("world", World.class);
    } catch (IllegalArgumentException e) {
        return; // the world did not parse
    }
    regions.namesIn(world)
           .forEach(sb::suggest);
})).execute((world, region, ctx) -> ctx.sendMessage(world.getName() + "/" + region));
```

`regions` stands for your own code.

In a child command built with `child(ref -> ...)`, read the parent value with `ref.get()` inside the provider.

### Validators filter suggestions

For arguments whose suggestions CommandLib builds from candidates (`EnumArgument`, `ObjectArgument`,
`NameableObjectArgument`, `OfflinePlayerArgument`, `OfflinePlayersArgument`, `UUIDArgument`, `UUIDsArgument`, Paper's
`TeamArgument`, and Spigot's `TeamArgument` with a custom scoreboard), a candidate is suggested only if the argument's
validator accepts it:

```java
argument(new EnumArgument<>("block", Material.class).validator(Material::isBlock))
        .execute((material, ctx) -> ctx.sendMessage(material.name()));
```

Only block materials are suggested, and other materials are rejected when the command runs.

## Tooltips

`suggest(text, tooltip)` sends the tooltip as plain text on every platform.

For a styled tooltip, pass a Brigadier `Message` that the platform can send:

| Platform | Styled tooltip |
|---|---|
| Paper | `PaperMessages.toMessage(Component)` converts an Adventure component. |
| Forge / NeoForge | A vanilla `Component` is a `Message`; pass it directly. |
| Spigot | No helper; use the plain-text overload. |

```java
// Paper
argument(new StringArgument("key", StringArgument.Type.WORD).addSuggestionProvider(sb -> sb.suggest("difficulty",
        PaperMessages.toMessage(Component.text("The world difficulty", NamedTextColor.GRAY)))))
        .execute((key, ctx) -> ctx.sendMessage(key));
```

How each client version renders tooltips has not been checked per platform.

## Permissions

Suggestions follow [permissions](permissions.md):

- A command, subcommand, or argument branch the sender may not use is not in the sender's command tree, so it is
  neither shown nor completed.
- The argument's suggestion callback also checks the branch permission and returns no suggestions without it.
- Generated help lists only the routes the sender may use.

## Asynchronous suggestions

Providers run on the thread where the platform asks for suggestions (normally the server thread). Do not block it.
For slow lookups, start the work, add the suggestions when it finishes, and pass the stage to `await(...)`:

```java
argument(new StringArgument("player", StringArgument.Type.WORD).setSuggestionProvider(sb -> {
    sb.await(database.findNamesAsync(sb.getLatestInput())
                     .thenAccept(names -> names.forEach(sb::suggest)));
})).execute((player, ctx) -> ctx.sendMessage(player));
```

`database.findNamesAsync` stands for your own code returning a `CompletionStage<List<String>>`. `SuggestionBuilder`
accepts suggestions from any thread. The response is sent once every awaited stage and the default suggestions have
completed. If a stage never completes, the client gets no suggestions for that request; there is no timeout or cache
yet.

Do not use Bukkit or Minecraft APIs that require the server thread inside the asynchronous part.

## Exceptions

An exception thrown by a provider is printed to the console and the client gets no suggestions for that request.

## Testing suggestions

Every testing artifact has `CommandTester#suggestions(input, sender)`, which returns Brigadier's
`CompletableFuture<Suggestions>`:

```java
List<String> texts = tester.suggestions("warp sp", sender)
                           .join()
                           .getList()
                           .stream()
                           .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                           .collect(Collectors.toList());
```

See [testing](testing.md).
