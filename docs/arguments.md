# Arguments

This catalog lists CommandLib's built-in arguments, compares each one by behavior with
[CommandAPI](https://docs.commandapi.dev/create-commands/arguments/arguments) and Paper's
[`ArgumentTypes`](https://jd.papermc.io/paper/1.21.11/io/papermc/paper/command/brigadier/argument/ArgumentTypes.html),
and lists the gaps. The comparison looks at what the argument does, not at its name: the input it accepts, its
suggestions, its errors, and how it looks values up in a registry.

All argument classes live in `dev.marulabs.commandlib.argument`. The class names are the same on every platform where
an argument exists, so code written against the `spigot` artifact usually compiles against `paper` unchanged.

For how arguments fail, see [errors](errors.md). For custom suggestions, see [suggestions](suggestions.md).

## Status legend

| Status | Meaning |
|---|---|
| **equivalent** | Same input syntax, same kind of result, comparable suggestions and errors. |
| **partial** | Exists, but the input syntax, result type, suggestions, or registry semantics differ. The note says how. |
| **missing** | No built-in argument. A workaround is given where one exists. |
| **unverified** | CommandLib has it, but the comparison point has not been checked (by code reading or a test). |
| — | The platform has no such concept, or the argument is CommandLib-specific. |

The status was checked by reading CommandLib's source and tests, CommandAPI's documentation, and the Paper Javadoc.
Every built-in Spigot, Paper, Forge, and NeoForge argument also runs on a real server in the integration tests
(`integration-test/shared/*/ArgumentTest.java`), which check that a valid input parses; they do not compare error texts
or suggestions with the references.

## Primitive and string arguments

| CommandLib | Result | Input and suggestions | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `BooleanArgument` | `Boolean` | `true` / `false`, suggested by Brigadier | `BooleanArgument` | Brigadier `bool()` | equivalent | equivalent | equivalent |
| `IntegerArgument(name[, min, max])` | `Integer` | Brigadier integer; out-of-range input is a Brigadier error | `IntegerArgument` | Brigadier `integer()` | equivalent | equivalent | equivalent |
| `LongArgument`, `FloatArgument`, `DoubleArgument` | `Long`, `Float`, `Double` | as above | same names | Brigadier | equivalent | equivalent | equivalent |
| `StringArgument(name, Type.WORD)` | `String` | one unquoted word (`a-z A-Z 0-9 _ - . +`) | `StringArgument` | Brigadier `word()` | equivalent | equivalent | equivalent |
| `StringArgument(name)` / `Type.PHRASE_QUOTED` | `String` | a word or a `"quoted phrase"`; **the default** | `TextArgument` | Brigadier `string()` | equivalent | equivalent | equivalent |
| `StringArgument(name, Type.PHRASE)` | `String` | the rest of the input | `GreedyStringArgument` | Brigadier `greedyString()` | equivalent | equivalent | equivalent |
| `UnparsedArgument` | `String` | one raw token, `@` and `/` allowed; advertised to the client as a greedy string | — | — | — | — | — |
| `LiteralArgument(name, values)` | `String` | one of a fixed or `Supplier` list, exact match; suggests values containing the input | `MultiLiteralArgument` | literal nodes | partial | partial | partial |
| `EnumArgument<T>(name, Class<T>)` | `T` | enum constant name, case-insensitive; suggests lower-case names | — (custom) | — | — | — | — |
| `ObjectArgument<T>(name, Map)` | `T` | a key of the map (quoted string, exact match); suggests keys | `CustomArgument` | custom | — | — | — |
| `NameableObjectArgument<T>(name, Collection)` | `T extends Nameable` | `tabCompleteName()` of a candidate; suggests names | `CustomArgument` | custom | — | — | — |

Notes:

- `LiteralArgument` is a string argument limited to a set of values, not a set of literal nodes. The client sees a
  string, so it does not validate the value or show it as a literal; the server rejects other values with
  "Incorrect argument for command". The value list can change at runtime. For a single fixed literal use a
  subcommand: `addChildren(new Command("start"))`.
- `UnparsedArgument` is advertised as a greedy string so that the client accepts any characters. Arguments after it
  are parsed by the server but get no client-side completion, so put it last.
- `EnumArgument`, `ObjectArgument`, `NameableObjectArgument`, `LiteralArgument` filter suggestions with
  case-insensitive "contains" matching, not prefix matching.

## Entities and players

| CommandLib | Result (Spigot, Paper / Forge, NeoForge) | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `PlayerArgument` | `Player` / `ServerPlayer` | vanilla single-player selector: name, UUID, `@p`, `@s`, `@r`, `@a[limit=1]` | `EntitySelectorArgument.OnePlayer` | `player()` | equivalent | equivalent | equivalent |
| `PlayersArgument` | `List<Player>` / `List<ServerPlayer>` | vanilla player selector | `EntitySelectorArgument.ManyPlayers` | `players()` | equivalent | unverified | equivalent |
| `EntityArgument` | `Entity` | vanilla single-entity selector | `EntitySelectorArgument.OneEntity` | `entity()` | equivalent | equivalent | equivalent |
| `EntitiesArgument` | `List<Entity>` | vanilla entity selector | `EntitySelectorArgument.ManyEntities` | `entities()` | equivalent | unverified | equivalent |
| `OfflinePlayerArgument` (Bukkit) | `OfflinePlayer` | a name of a player who has played before; suggests known names | — | — | — | — | — |
| `OfflinePlayersArgument` (Bukkit) | `List<OfflinePlayer>` | a known name, `@a` (all known players), `@r` (one random known player) | `PlayerProfileArgument` | `playerProfiles()` | partial | partial | — |
| `GameProfileArgument` (Forge / NeoForge) | `GameProfile` | the name of an online player, case-insensitive; suggests online players | `PlayerProfileArgument` | `playerProfiles()` | — | — | partial |
| `UUIDArgument` | `UUID` | a UUID, or a known player name resolved to its UUID; suggests names with the UUID as tooltip | `UUIDArgument` | `uuid()` | partial | partial | partial |
| `UUIDsArgument` | `List<UUID>` | a UUID, a known name, `@a`, `@r` | — | — | — | — | — |
| score holder | — | — | `ScoreHolderArgument.Single` / `.Multiple` | — | missing | missing | missing |

Notes:

- Selectors are parsed and resolved by Minecraft on Spigot and Forge / NeoForge, and by Paper's resolvers on Paper.
  Selector permissions (operator level 2 for `@e` etc.) are vanilla's.
- When a selector or name matches nothing, Spigot and Forge / NeoForge report vanilla's `No player was found` /
  `No entity was found` for all four selector arguments. On Paper, `PlayerArgument` / `EntityArgument` check the
  resolved list themselves and send CommandLib's `no player found` / `no entity found`. `PlayersArgument` /
  `EntitiesArgument` on Paper do not check it: whether Paper's resolver rejects an empty result or passes an empty
  list to the executor has not been checked, hence `unverified`. (CommandAPI's `ManyPlayers` / `ManyEntities` allow
  empty results by default.)
- `UUIDArgument` is `partial` because it also accepts player names, which vanilla's `uuid` argument and CommandAPI's
  `UUIDArgument` do not. A UUID string always works.
- `OfflinePlayersArgument` and `UUIDsArgument` implement `@a` / `@r` themselves over `Bukkit.getOfflinePlayers()`;
  other selectors are rejected with `<input> is invalid selector`.

## Positions and worlds

| CommandLib | Result | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `LocationArgument` | Bukkit `Location` / CommandLib `util.Location` (Forge) | vanilla `vec3`: `x y z`, `~`, `^`; integer x and z are centered (`1 64 1` → `1.5 64 1.5`); world is the sender's | `LocationArgument` (`PRECISE_POSITION`) | `finePosition(true)` | equivalent | equivalent | equivalent |
| `BlockPosArgument` (Forge / NeoForge) | `BlockPos` | vanilla block position | `LocationArgument` (`BLOCK_POSITION`) | `blockPosition()` | missing | missing | equivalent |
| `WorldArgument` (Bukkit) | `World` | dimension key such as `minecraft:overworld`, suggested by the server | `WorldArgument` | `world()` | equivalent | equivalent | missing |
| 2D location | — | — | `Location2DArgument` | `columnBlockPosition()`, `columnFinePosition()` | missing | missing | missing |
| rotation | — | — | `RotationArgument` | `rotation()` | missing | missing | missing |
| angle | — | — | `AngleArgument` | `angle()` | missing | missing | missing |
| axes | — | — | `AxisArgument` | `axes()` | missing | missing | missing |
| entity anchor | — | — | — | `entityAnchor()` | missing | missing | missing |
| height map | — | — | — | `heightMap()` | missing | missing | missing |

Notes:

- `LocationArgument` has no option for uncentered integers or block positions on Bukkit. Use `BlockPosArgument` on
  Forge / NeoForge. The Spigot module already has an NMS bridge for block positions (`NMSArgumentTileLocation`) that no
  public argument uses yet.
- On Forge / NeoForge, read a dimension with `ResourceLocationArgument` and look it up in the server's levels.

## Blocks, items, and effects

| CommandLib | Result | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `BlockDataArgument` (Bukkit) | `BlockData` | vanilla block state: `stone`, `oak_stairs[facing=east]`, NBT accepted | `BlockStateArgument` (`BlockState`) | `blockState()` (`BlockState`) | partial | partial | — |
| `BlockStateArgument` (Forge / NeoForge) | `BlockInput` | vanilla block state with NBT | — | — | — | — | equivalent |
| `ItemStackArgument` | `ItemStack` (amount 1) | vanilla item: `diamond_sword`, with NBT / components | `ItemStackArgument` | `itemStack()` | equivalent | equivalent | equivalent |
| `EnchantmentArgument` | Bukkit `Enchantment` / vanilla `Enchantment` (`Holder<Enchantment>` on 1.20.5+) | registry key such as `minecraft:sharpness` | `EnchantmentArgument` | `resource(ENCHANTMENT)` | equivalent | equivalent | equivalent |
| `PotionEffectArgument` (Bukkit) | `PotionEffect` (duration 1, amplifier 0) | effect key such as `minecraft:speed` | `PotionEffectArgument` (`PotionEffectType`) | `resource(MOB_EFFECT)` | partial | partial | — |
| `EffectArgument` (Forge / NeoForge) | `MobEffect` (`Holder<MobEffect>` on 1.20.5+) | effect key | — | — | — | — | equivalent |
| `ParticleArgument` | Bukkit `Particle` / vanilla `ParticleOptions` | Spigot, Forge / NeoForge: vanilla particle syntax with options; Paper: particle key only | `ParticleArgument` (`ParticleData`) | `resource(PARTICLE_TYPE)` | partial | partial | equivalent |
| block predicate | — | — | `BlockPredicateArgument` | `blockInWorldPredicate()` | missing | missing | missing |
| item predicate | — | — | `ItemStackPredicateArgument` | `itemPredicate()` | missing | missing | missing |

Notes:

- `BlockDataArgument` drops the block entity NBT the input may carry, since `BlockData` cannot hold it.
- `PotionEffectArgument` really selects an effect type. Use `effect.getType()`; the duration and amplifier are
  placeholders.
- Spigot's `ParticleArgument` accepts the full vanilla syntax (for example dust colors) but returns only the
  `Particle` type, so the options are lost. Paper's accepts only the key. Forge / NeoForge return the full options.

## Registry-backed values

| CommandLib | Result | Input and suggestions | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `NamespacedKeyArgument` (Bukkit) | `NamespacedKey` | `namespace:path`; no namespace means `minecraft` | `NamespacedKeyArgument` | `namespacedKey()` | equivalent | equivalent | — |
| `ResourceLocationArgument` (Forge / NeoForge) | `ResourceLocation` | `namespace:path` | — | — | — | — | equivalent |
| `SoundArgument` (Bukkit) | `Sound` | sound key such as `minecraft:entity.pig.ambient`; suggests registry keys | `SoundArgument` | `resource(SOUND_EVENT)` | equivalent | equivalent | missing |
| `BiomeArgument` (Bukkit) | `Biome` | biome key; suggests registry keys | `BiomeArgument` | `resource(BIOME)` | equivalent | equivalent | missing |
| `AttributeArgument` (Bukkit) | `Attribute` | attribute key; suggests registry keys | — | `resource(ATTRIBUTE)` | equivalent | equivalent | missing |
| `EntityTypeArgument` (Bukkit) | `EntityType` | Spigot: enum name such as `zombie`; Paper: registry key such as `minecraft:zombie` | `EntityTypeArgument` | `resource(ENTITY_TYPE)` | partial | equivalent | missing |
| `AdvancementArgument` (Bukkit) | `Advancement` | advancement key; suggests loaded advancements | `AdvancementArgument` | — | equivalent | equivalent | missing |
| `LootTableArgument` (Bukkit) | `LootTable` | loot table key; suggests registered loot tables | `LootTableArgument` | — | equivalent | equivalent | missing |
| `RecipeArgument` (Bukkit) | `Recipe` | recipe key; suggests keyed recipes | `RecipeArgument` (`ComplexRecipe`) | — | equivalent | equivalent | missing |
| generic registry value | — | — | per-type arguments | `resource(RegistryKey)`, `resourceKey(RegistryKey)` | missing | missing | missing |

Notes:

- On Spigot the registry-backed arguments use the vanilla resource-location argument type and look the key up in
  Bukkit's `Registry` (or `Bukkit.getAdvancement` / `getLootTable` / `getRecipe`). The client sees a resource location,
  so it does not know which keys exist; an unknown key is reported by CommandLib as `<key> is not a known <kind>`.
  Suggestions are the registry keys containing the typed text.
- On Paper, `SoundArgument`, `BiomeArgument`, `AttributeArgument`, `EntityTypeArgument`, `EnchantmentArgument`,
  `PotionEffectArgument`, and `ParticleArgument` use Paper's `ArgumentTypes.resource(...)`, so Paper parses, suggests,
  and rejects unknown keys natively during Brigadier parsing.
- Spigot's `EntityTypeArgument` is still an enum argument: it accepts `zombie` but not `minecraft:zombie`, and it
  suggests every constant, including `unknown`. Converging it on the registry key form is tracked in the roadmap.
- Forge / NeoForge have only `ResourceLocationArgument`; look the value up in the registry yourself.

## Scoreboard and game systems

| CommandLib | Result | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `TeamArgument` | Bukkit `Team` / vanilla `PlayerTeam` | team name; `scoreboard(Supplier)` selects another scoreboard | `TeamArgument` | — | equivalent | equivalent | equivalent |
| `ObjectiveArgument` (Bukkit) | `Objective` | objective name; `scoreboard(Supplier)` selects another scoreboard | `ObjectiveArgument` | — | equivalent | equivalent | missing |
| `ScoreboardDisplaySlotArgument` (Bukkit) | `DisplaySlot` | Bukkit enum names such as `sidebar`, `player_list`, `below_name`, `sidebar_team_red` | `ScoreboardSlotArgument` | `scoreboardDisplaySlot()` | partial | partial | missing |
| `GameModeArgument` (Bukkit) | `GameMode` | `survival`, `creative`, `adventure`, `spectator` | — | `gameMode()` | equivalent | equivalent | missing |
| `ChatColorArgument` (Bukkit) | legacy `ChatColor` | any `ChatColor` constant name, including formats such as `bold` | `ChatColorArgument` | `namedColor()` | partial | partial | missing |
| objective criteria | — | — | `ObjectiveCriteriaArgument` | `objectiveCriteria()` | missing | missing | missing |
| math operation | — | — | `MathOperationArgument` | — | missing | missing | missing |

Notes:

- On Spigot without `scoreboard(...)`, `TeamArgument` uses vanilla's team argument (server scoreboard, vanilla
  suggestions and `Unknown team` error). Paper's and the custom-scoreboard variants are a word argument with
  CommandLib's suggestions and the "Incorrect argument for command" error.
- `ScoreboardDisplaySlotArgument` uses Bukkit's enum names, not vanilla's `list`, `belowName`, `sidebar.team.red`.
- `ChatColorArgument` accepts formatting codes as well as colors, while vanilla's color argument accepts only colors
  and `reset`. On Paper it returns the legacy `ChatColor`, not Adventure's `NamedTextColor`.
- On Forge / NeoForge, `EnumArgument<GameType>` works as a game mode argument.

## Text, NBT, and other advanced arguments

| Capability | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|
| text component / JSON text | `ChatComponentArgument` | `component()` | missing | missing | missing |
| signed chat message | `ChatArgument` | `signedMessage()` | missing | missing | missing |
| style, hex color | — | `style()`, `hexColor()` | missing | missing | missing |
| NBT / SNBT compound | `NBTCompoundArgument` | — | missing | missing | missing |
| integer / double range (`1..10`) | `IntegerRangeArgument`, `DoubleRangeArgument` | `integerRange()`, `doubleRange()` | missing | missing | missing |
| time / duration (`1d`, `20t`) | `TimeArgument` | `time()` | missing | missing | missing |
| function / function tag | `FunctionArgument` | — | missing | missing | missing |
| command | `CommandArgument` | — | missing | missing | missing |
| list | `ListArgument` | — | partial | partial | partial |
| map | `MapArgument` | — | missing | missing | missing |
| structure mirror / rotation | — | `templateMirror()`, `templateRotation()` | missing | missing | missing |
| optional / default argument | `withOptionalArguments`, `setOptional` | — | equivalent | equivalent | equivalent |
| custom argument type | `CustomArgument<T, B>` | `CustomArgumentType` | partial | partial | partial |

Notes:

- Custom arguments: extend the platform's `Argument<T, SELF>` with an existing Brigadier or vanilla `ArgumentType` and
  implement `cast` and `parseImpl`, or adapt an existing argument with `validator`, `transformer`, and
  `additionalParser`. A new `ArgumentType` class must be known to the client, which CommandLib does not register for
  you (only the internal raw word type of `UnparsedArgument` is), so it is `partial`.
- Optional and variadic arguments: `optional(defaultValue)` lets the sender omit the last arguments of a branch, and
  `optional(extractor)` computes the default, e.g. `optional(Command.playerRequirer())` for the sender. `variadic()`
  takes the last argument one or more times (up to 16, or `variadic(maxCount)`) and gives a `List<T>`. Only the last
  arguments of a branch can be optional or variadic, and such a branch cannot have child commands; registration fails
  otherwise. Help shows `[count]` and `<targets...>`. Each value of a variadic argument is a separate token parsed by the
  element's own type, with its own suggestions, so it is `partial` against CommandAPI's `ListArgument`, which reads one
  greedy string split by a delimiter. See [the design note](design/optional-variadic-arguments.md).

## Not checked yet

These points are open, separate from the missing arguments below:

- Paper `PlayersArgument` / `EntitiesArgument` with a selector that matches nothing (see above).
- Error texts and suggestion lists have not been compared one by one with CommandAPI's; only input syntax and result
  types were compared.
- Mohist runs the Spigot module. Registry-backed arguments there have not been checked with modded registry entries.
- How each client renders suggestion tooltips; see [suggestions](suggestions.md).

## Missing arguments, by priority

The order weighs plugin demand, the CommandAPI / Paper parity gap, and how much of the work already exists.

1. **Integer / double range** (`1..10`, `..5`, `3..`) and **time / duration**. Common in admin commands; Paper has native
   types for both.
2. **Text component and signed message** for Paper (`component()`, `signedMessage()`), and a portable component
   argument for Spigot and Forge / NeoForge.
3. **Block position on Bukkit** and an uncentered location option. The Spigot NMS bridge (`NMSArgumentTileLocation`)
   and its test mock already exist.
4. **Item predicate and block predicate.** The Spigot NMS bridge for item predicates (`NMSArgumentPredicateItemStack`)
   and its mock already exist.
5. **NBT / SNBT compound.**
6. **Registry semantics on Spigot**: `EntityTypeArgument` by `minecraft:` key, and a generic registry argument
   (Paper `resource(RegistryKey)` / `resourceKey(RegistryKey)` equivalent) on both Bukkit modules.
7. **Forge / NeoForge parity**: dimension / world, sound, biome, attribute, entity type, objective, game mode, and the
   other Bukkit-only registry arguments.
8. **Rotation, angle, 2D location, axes.**
9. **Score holder, objective criteria, math operation**, the remaining scoreboard set.
10. **Function, command, list, map** arguments.
11. **Vanilla display slot names, `NamedTextColor` on Paper**, `style`, `hexColor`, `heightMap`, `entityAnchor`,
    structure mirror / rotation.

## Testing each argument

The testing artifacts run commands without a server (see [testing](testing.md)). How much of an argument runs for real
depends on the artifact:

- **real**: the argument's own parser and lookup run; production input works.
- **mock**: the tester stubs the platform type with a simplified parser. Use `withFake...` / `withPlayer` to provide
  the values; selector filters (`@e[type=...]`), relative coordinates, and NBT are not parsed.
- **static**: the argument calls Bukkit static APIs (`Bukkit.getOfflinePlayers()`, `Registry`, scoreboards,
  `Bukkit.getAdvancement`, ...). Stub them with `Mockito.mockStatic(Bukkit.class)` declared before the `CommandTester` in
  the same try-with-resources, or use MockBukkit.
- **server only**: needs state the tester cannot provide; cover it with an integration test.

| Argument | `spigot-testing` | `paper-testing` | `forge-testing` / `neoforge-testing` |
|---|---|---|---|
| Boolean, numbers, `StringArgument`, `UnparsedArgument`, `LiteralArgument`, `EnumArgument`, `ObjectArgument`, `NameableObjectArgument` | real | real | real |
| `GameModeArgument`, `EntityTypeArgument`, `ChatColorArgument`, `ScoreboardDisplaySlotArgument` | real (enum) | `GameMode` mock; `EntityType` mock (`withFakeEntityType`); others real (enum) | — |
| `PlayerArgument`, `PlayersArgument` | mock: names, `@s`, `@p`, `@r`, `@a` (`withFakePlayer`) | mock: names, `@a` for the list variant (`withFakePlayer`) | real selectors over the fake players (`withPlayer`) |
| `EntityArgument`, `EntitiesArgument` | mock: names, `@s`, `@p`, `@r`, `@a`, `@e` (`withFakeEntity`) | mock: names, `@a` (`withFakeEntity`) | real selectors; only players exist |
| `OfflinePlayerArgument`, `OfflinePlayersArgument`, `UUIDArgument`, `UUIDsArgument` | static | static | `UUIDArgument`, `UUIDsArgument`, `GameProfileArgument`: online fake players only |
| `LocationArgument` | mock: `x y z` numbers | mock: `x y z` numbers, world `null` | real |
| `BlockPosArgument` | — | — | real |
| `WorldArgument` | mock (`withFakeWorld("minecraft:overworld", world)`) | mock (`withFakeWorld(world)`, by key) | — |
| `BlockDataArgument` / `BlockStateArgument` | mock | mock (`withFakeBlockData`) | real |
| `ItemStackArgument` | mock | mock (`withFakeItemStack`) | real |
| `EnchantmentArgument` | mock | mock (`withFakeEnchantment`) | real (1.20.5+: from the bootstrapped registries) |
| `PotionEffectArgument` / `EffectArgument` | mock | mock (`withFakePotionEffectType`) | real |
| `ParticleArgument` | mock | mock (`withFakeParticle`) | real |
| `NamespacedKeyArgument` / `ResourceLocationArgument` | real key parsing | real key parsing | real |
| `SoundArgument`, `BiomeArgument`, `AttributeArgument` | static (`Registry`) | mock (`withFakeSound`, `withFakeBiome`, `withFakeAttribute`) | — |
| `AdvancementArgument`, `LootTableArgument`, `RecipeArgument` | static | static | — |
| `TeamArgument` | mock vanilla type plus static scoreboard; real with `scoreboard(Supplier)` | static, or real with `scoreboard(Supplier)` | real with `scoreboard(Supplier)`; the server scoreboard is not available |
| `ObjectiveArgument` | static, or real with `scoreboard(Supplier)` | static, or real with `scoreboard(Supplier)` | — |

`paper-testing`'s registry mocks return the single fake value for any token, so they do not check that a key exists.
Tests that depend on unknown-key errors, real registries, tags, or worlds belong in integration tests.
