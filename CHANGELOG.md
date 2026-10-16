# Changelog

This file lists the changes in each CommandLib release that affect plugin and mod authors. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). CommandLib is 0.x, so a minor release can break the API.

## [0.18.0] - Unreleased

0.18.0 breaks the API in many places. Read **Breaking changes** before you upgrade from 0.17.2.

### Breaking changes

#### Artifacts, packages and Java

- The packages move from `net.kunmc.lab.commandlib` to `dev.marulabs.commandlib`. Change your imports and your Shadow
  `relocate(...)` source package.
- The JitPack coordinates move from `com.github.TeamKun.CommandLib` to `com.github.Maru32768.CommandLib`. See
  [Packaging](docs/packaging.md#artifacts) for every artifact.
- The `bukkit` artifact is removed. Use `spigot` for Spigot, Mohist, and Paper up to 1.21.1, or `paper` for Paper 1.21+.
- On Paper newer than 1.21.1, the `spigot` artifact fails at registration with an `NMSClassNotFoundException` that names
  the `paper` artifact. Switch to `paper` there.
- The `forge` artifact is now `forge-1.16.5`. Each modded artifact is named `<loader>-<minecraft version>`.
- CommandLib needs Java 11 or later. 0.17.2 targeted Java 8. The `paper` artifact needs Java 21 and `velocity` needs
  Java 17.
- The `spigot` artifact reaches the server only through typed NMS modules. A server version without one fails at
  registration with `NMSClassNotFoundException`. The modules cover Spigot 1.16.4 to 26.3
  ([compatibility](docs/compatibility.md)).

#### Commands and arguments

- `argument(arg, (value, ctx) -> ...)` and the other overloads that took an executor are removed. Call
  `argument(arg).execute((value, ctx) -> ...)`. The returned branch also takes `permission`, `description` and `child`.
- `argument(Consumer<ArgumentBuilder>)` is removed together with `ArgumentBuilder` and `AbstractArgumentBuilder`. For
  eight or more arguments, call `argument(a, b, ..., h)` or `argument(List)` and read each value with
  `ctx.getArgument(argument)` ([Java builder](docs/java-builder.md#eight-or-more-arguments)).
- The argument constructors that took a `Consumer<CommonArgument.Option>` are removed, and so is `CommonArgument.Option`.
  Chain the methods on the argument instead, for example
  `new IntegerArgument("n").setSuggestionProvider(...).validator(...)`. `displayDefaultSuggestions` is now
  `useDefaultSuggestions`, and the argument's `contextAction` is now `execute`.
- Custom arguments take a self type: `Argument<T>` becomes `Argument<T, SELF>`, and `CommonArgument<T, C>` becomes
  `CommonArgument<T, C, SELF>`.
- `SuggestionAction` is now `SuggestionProvider` in `dev.marulabs.commandlib.suggestion`. The setters are
  `setSuggestionProvider`, `addSuggestionProvider` and `clearSuggestionProviders`, and they reject `null`.
- `ContextAction` is now `CommandExecutor` in `dev.marulabs.commandlib.command`.
- `AbstractCommandContext` is now `CommonCommandContext`.
- Some types move to subpackages: `Prerequisite` to `command`, `Suggestion` and `SuggestionBuilder` to `suggestion`,
  and `ArgumentValidator` and `Nameable` to `argument`.
- `UncaughtExceptionHandler<S, C>` is now `UncaughtExceptionHandler<C>`.
- The context getters are renamed. `getArg` and `getArgs` are now `getInput` and `getInputs`, and `getParsedArg` and
  `getParsedArgs` are now `getArgument` and `getArguments`. `getArgument` throws `IllegalArgumentException` for an
  unknown argument instead of returning `null`.
- `setInheritParentPrerequisite(false)` is now `disableParentPrerequisite()`, and `setInheritParentPreprocess(false)` is
  now `disableParentMiddleware()`.
- Each execution runs through a middleware chain. `addPreprocess` still works and runs as a middleware
  ([execution](docs/execution.md#middleware)).
- Forge 1.16.5: `BlockPosArgument` is now `BlockPositionArgument`.

#### Messages

- `TextComponentBuilder`, `TranslatableComponentBuilder`, `ComponentBuilder`, `sendMessageWithOption` and
  `MessageOption` are removed. Build a `RichText` (`TextRichText` or `TranslatableRichText`) and pass it to
  `sendMessage`, `sendSuccess`, `sendWarn` or `sendFailure`.
- CommandLib's own messages are now captions in the sender's locale, with English and Japanese built in. Several
  English texts are reworded. For example, the prerequisite failure says "You do not have permission". Where vanilla
  has a message with the same meaning, such as "No player was found", CommandLib sends vanilla's translatable message.
  Update tests that compare message text ([localization](docs/localization.md#built-in-captions)).
- On Spigot, Paper and the modded artifacts, `sendSuccess` goes through vanilla's `CommandSourceStack#sendSuccess`.
  The `sendCommandFeedback` game rule and silent sources now suppress it.
- A failing command reports its error as a `CommandSyntaxException`, like a vanilla command, so
  `/execute store success` stores 0 ([vanilla integration](docs/vanilla-integration.md)).

#### Permissions

- Spigot: permission nodes change from `minecraft.command.<name>` to `<plugin name>.command.<path>`, and every child
  command and argument branch gets its own node. Update your permission plugin configuration
  ([permissions](docs/permissions.md#node-names)).
- Spigot: `permission(PermissionDefault.NOT_OP)` throws `IllegalArgumentException`.
- Forge: `CommandLib.register(commands...)` is now `CommandLib.register(permissionPrefix, commands...)`, and
  `permissionLevel(int)` is replaced by `permission(DefaultPermission)` (or `permission(DefaultPermissionLevel)` on
  1.16.5).

### Added

#### Platforms

- The `paper` artifact for Paper 1.21+. It registers through Paper's command API and lifecycle events, and can also
  register from a plugin bootstrapper with `CommandLib.bootstrap(...)`.
- Folia support in the `paper` artifact. Async commands return to the entity, region or global region thread
  ([Folia](docs/execution.md#folia)). The `spigot` artifact refuses Folia with an `UnsupportedOperationException`.
- The `velocity` artifact for Velocity 3.4+, with proxy arguments such as `PlayerArgument` and `ServerArgument`
  ([Velocity arguments](docs/arguments.md#velocity)).
- Fabric artifacts `fabric-1.20.1`, `fabric-1.21.1` and `fabric-1.21.4`. They need Fabric API.
- Forge artifacts `forge-1.18.2`, `forge-1.19.2` and `forge-1.20.1`, and NeoForge artifacts `neoforge-1.21.1` and
  `neoforge-1.21.4`.
- The `spigot` artifact runs on Spigot up to 26.3.

Velocity, Fabric and Folia are new in this release and are not yet verified on a real server.

#### Ways to define commands

- An annotation API: `@Command`, `@Subcommand`, `@Flag`, `@Option`, `@Condition` and more, registered through each
  platform's `AnnotatedCommands` ([annotated commands](docs/annotations.md)).
- A Java builder: `Command.builder(name)` configures a command in one chain ([Java builder](docs/java-builder.md)).
- A Kotlin DSL for Spigot, Paper and Velocity (`spigot-kotlin`, `paper-kotlin`, `velocity-kotlin`), with delegated
  options, `require(...)` blocks and `suspend` executors ([Kotlin DSL](docs/kotlin.md)).

#### Commands and arguments

- Typed options: flags, value options, `Options.count`, `repeatable`, `aliases`, `required`, option permissions,
  `requires` dependencies, `mutuallyExclusive`, and `optionsAnywhere()` for options after the arguments
  ([options](docs/options.md)).
- Optional and variadic trailing arguments: `optional(...)` and `variadic(...)`.
- Child commands on argument branches. `child(ref -> ...)` passes an `ArgRef` whose `get()` reads the parent's value.
- `require(Extractor)` with `playerRequirer()` and `consoleRequirer()`, and `requirePlayer()` / `requireConsole()`.
- `executeAsync`, `ctx.supplyAsync` and `ctx.runAsync`, which return to the server thread
  ([async executors](docs/execution.md#async-executors)).
- Middlewares (`addMiddleware`), and middlewares and providers for a whole registration.
- `confirm()` and `cooldown(Duration)`. `Cooldown` can report the remaining time and reset it.
- Typed context values: `provide(...)` on commands, values that middlewares put, sender mapping, and dependencies
  ([context values](docs/context-values.md)).
- `ctx.fail(...)` ends a command with an error from any executor.
- `visibleWhen(predicate)` hides a command or branch by plugin state.
- `description(Function<C, String>)` for a description that depends on the sender, and `serverHelp(...)` for the text
  of the server's `/help`.
- Permission nodes for every command, child and argument branch, with a configurable prefix and per-node overrides
  ([permissions](docs/permissions.md)).
- `CommandLib.refresh(player)` and `CommandLib.refreshAll()` resend the command tree.
- `getLocale()`, `getLanguage()` and the platform-independent `getActor()` on the context.
- `ctx.setResult(...)` sets what `/execute store result` stores, and `sendSuccess(message, true)` broadcasts to
  operators like vanilla.
- Spigot: commands also get the `<plugin>:<command>` label.

#### New arguments

- On Spigot, Paper and the modded artifacts: `AdvancementArgument`, `AttributeArgument`, `BiomeArgument`,
  `BlockPositionArgument` (Bukkit side), `BlockPredicateArgument`, `ItemPredicateArgument`, `ChatColorArgument`,
  `ComponentArgument`, `MessageArgument`, `NBTCompoundArgument`, `DurationArgument`, `IntegerRangeArgument`,
  `DoubleRangeArgument`, `EntityTypeArgument`, `GameModeArgument`, `LongArgument`, `LootTableArgument`,
  `ObjectiveArgument`, `RecipeArgument`, `RegistryArgument`, `ScoreboardDisplaySlotArgument`, `SoundArgument`,
  `TimeArgument` and `WorldArgument` ([argument catalog](docs/arguments.md)).
- `NamespacedKeyArgument` on Spigot and Paper, and `ResourceLocationArgument` on the modded artifacts.
- Registry-backed arguments (`AttributeArgument`, `BiomeArgument`, `SoundArgument`, `EntityTypeArgument`) take a
  namespaced key such as `minecraft:zombie`. `ScoreboardDisplaySlotArgument` and `ChatColorArgument` take vanilla's
  names, such as `below_name`.
- `TeamArgument` can look teams up in a supplied scoreboard.
- Suggestions with tooltips, and asynchronous suggestions through `SuggestionBuilder#await`
  ([suggestions](docs/suggestions.md)).

#### Help, messages and errors

- A generated `help` subcommand and usage lines, `examples(...)`, and `helpFormatter`, `usageFormatter` and
  `errorFormatter` ([help](docs/help.md)).
- `RichText` with colors, styles, hover and click actions on every platform.
- Captions: per-key and per-locale texts, `ResourceBundle` and properties bundles, a `CaptionSender`, and
  `captions(...)` on a command ([localization](docs/localization.md)).
- A resolve error whose exception carries its input shows vanilla's context line under it
  ([errors](docs/errors.md)).
- `sendSuccessFor(targets, single, multiple)` sends the singular text for one target and the counted text otherwise.
- Paper sends resolve errors and empty selectors as vanilla's translatable errors.

#### Documentation and diagnostics

- `CommandDocs` exports the commands, their examples and every permission node as Markdown or JSON.
- `CommandLib.diagnostics()` reports the platform and the registrations, and an opt-in diagnostics command shows it
  in game ([diagnostics](docs/diagnostics.md)).

#### Testing

- Server-free test artifacts: `common-testing`, `spigot-testing`, `paper-testing`, `velocity-testing` and
  `<loader>-testing-<version>` ([testing](docs/testing.md)).
- `CommandTester` runs commands, suggestions and command tree snapshots. `FakeSender` has player and console builders.
- A JUnit 5 extension, `CommandLibExtension`, creates a tester per test and injects `CommandTester` and `@Sender`
  `FakeSender` parameters.

### Fixed

- Suggestions match case-insensitively.
- Vanilla argument types such as item, block and entity selector arguments keep their own suggestions.
- An argument named like the literal before it keeps its own input.
- `UncaughtExceptionHandler` receives exceptions thrown by executors.
- Adding a child or alias whose name is already taken fails at once instead of when the tree is built.
- Spigot: commands work after Bukkit `/reload`, because CommandLib adds them to the vanilla dispatcher.
- Spigot: commands appear under the plugin's `/help` index with their permission and description, instead of as "A
  Mojang provided command", and lose the stray `minecraft:` label.
- Spigot: data pack functions that use CommandLib commands keep working after `/minecraft:reload`, `/datapack` and
  `Bukkit#reloadData`, and on Mohist. Registration no longer reruns `#minecraft:load`, and the server no longer logs
  "Failed to load function" for those functions.
- Spigot: `ParticleArgument` accepts particles with options, such as `dust`, from 1.20.4.
- Spigot: potion effect lookups on 1.17 to 1.19.2, and commands after `/minecraft:reload` on 1.20.6+.
- The command source location carries the sender's rotation.
- Forge 1.16.5: commands survive `/reload`, failure messages go through `sendFailure`, `Location#getChunk` returns the
  right chunk, and `BlockPositionArgument` rejects fractional coordinates like vanilla.
- Forge: argument resolve errors are sent as failures, and cloning a `Location` without a world no longer throws.
