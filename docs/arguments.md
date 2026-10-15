# Arguments

This catalog lists CommandLib's built-in arguments, compares each one by behavior with
[CommandAPI](https://docs.commandapi.dev/create-commands/arguments/arguments) and Paper's
[`ArgumentTypes`](https://jd.papermc.io/paper/1.21.11/io/papermc/paper/command/brigadier/argument/ArgumentTypes.html),
and lists the gaps. The comparison looks at what the argument does, not at its name: the input it accepts, its
suggestions, its errors, and how it looks values up in a registry.

All argument classes live in `dev.marulabs.commandlib.argument`. The class names are the same on every platform where
an argument exists, so code written against the `spigot` artifact usually compiles against `paper` unchanged.

Fabric (`fabric-<minecraft-version>`) is built from the same source tree as Forge / NeoForge and has the same arguments
with the same behavior, so every "Forge / NeoForge" column and note below applies to Fabric of the same Minecraft
version.

For how arguments fail, see [errors](errors.md). For custom suggestions, see [suggestions](suggestions.md).

The tables below compare the server platforms. Velocity, a proxy, has its own smaller set; see [Velocity](#velocity).

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
or suggestions with the references. Fabric has no integration target yet, so its arguments are checked only by
`fabric-testing`.

## Primitive and string arguments

| CommandLib | Result | Input and suggestions | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `BooleanArgument` | `Boolean` | `true` / `false`, suggested by Brigadier | `BooleanArgument` | Brigadier `bool()` | equivalent | equivalent | equivalent |
| `IntegerArgument(name[, min, max])` | `Integer` | Brigadier integer; out-of-range input is a Brigadier error | `IntegerArgument` | Brigadier `integer()` | equivalent | equivalent | equivalent |
| `LongArgument`, `FloatArgument`, `DoubleArgument` | `Long`, `Float`, `Double` | as above | same names | Brigadier | equivalent | equivalent | equivalent |
| `IntegerRangeArgument` | Guava `Range<Integer>` | vanilla `int_range`: `1..10`, `..5`, `3..`, `5`; parsed and highlighted by the client; a minimum above the maximum is a syntax error | `IntegerRangeArgument` (`IntegerRange`) | `integerRange()` | equivalent | equivalent | equivalent |
| `DoubleRangeArgument` | Guava `Range<Double>` | vanilla `float_range`: `0.5..10`, `..5`, `3.5..`, `5` | `DoubleRangeArgument` (`DoubleRange`) | `doubleRange()` | equivalent | equivalent | equivalent |
| `DurationArgument` | `java.time.Duration` | a word of amounts with units `d h m s t`, each at most once from the largest: `1h30m`, `20t`, `2d`; a typed number is suggested with each unit | `TimeArgument` (`Integer` ticks) | `time()` (`Integer` ticks) | partial | partial | partial |
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
- The range arguments return a closed Guava `Range`, unbounded on an open side (`3..` is `[3..+∞)`, `5` is
  `[5..5]`), the type Paper's range providers return. Spigot reads them through NMS `RangeArgument`, Forge / NeoForge
  through Minecraft's `RangeArgument`.
- `DurationArgument` counts real time: `t` is 50 ms (a tick at 20 TPS) and `d` is 24 hours. Vanilla `time` (CommandAPI
  `TimeArgument`, Paper `time()`) counts game ticks, means the 24000 ticks of a Minecraft day by `d`, accepts decimals
  such as `1.5d` and a bare number of ticks, and takes one unit only. `DurationArgument` rejects decimals and bare
  numbers but combines units. The client sees a word, so malformed input fails when the command runs with
  "Incorrect argument for command". Convert with `duration.toMillis() / 50` for a tick count.

## Entities and players

| CommandLib | Result (Spigot, Paper / Forge, NeoForge) | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `PlayerArgument` | `Player` / `ServerPlayer` | vanilla single-player selector: name, UUID, `@p`, `@s`, `@r`, `@a[limit=1]` | `EntitySelectorArgument.OnePlayer` | `player()` | equivalent | equivalent | equivalent |
| `PlayersArgument` | `List<Player>` / `List<ServerPlayer>` | vanilla player selector | `EntitySelectorArgument.ManyPlayers` | `players()` | equivalent | equivalent | equivalent |
| `EntityArgument` | `Entity` | vanilla single-entity selector | `EntitySelectorArgument.OneEntity` | `entity()` | equivalent | equivalent | equivalent |
| `EntitiesArgument` | `List<Entity>` | vanilla entity selector | `EntitySelectorArgument.ManyEntities` | `entities()` | equivalent | equivalent | equivalent |
| `OfflinePlayerArgument` (Bukkit) | `OfflinePlayer` | a name of a player who has played before; suggests known names | — | — | — | — | — |
| `OfflinePlayersArgument` (Bukkit) | `List<OfflinePlayer>` | a known name, `@a` (all known players), `@r` (one random known player) | `PlayerProfileArgument` | `playerProfiles()` | partial | partial | — |
| `GameProfileArgument` (Forge / NeoForge) | `GameProfile` | the name of an online player, case-insensitive; suggests online players | `PlayerProfileArgument` | `playerProfiles()` | — | — | partial |
| `UUIDArgument` | `UUID` | a UUID, or a known player name resolved to its UUID; suggests names with the UUID as tooltip | `UUIDArgument` | `uuid()` | partial | partial | partial |
| `UUIDsArgument` | `List<UUID>` | a UUID, a known name, `@a`, `@r` | — | — | — | — | — |
| score holder | — | — | `ScoreHolderArgument.Single` / `.Multiple` | — | missing | missing | missing |

Notes:

- Selectors are parsed and resolved by Minecraft on Spigot and Forge / NeoForge, and by Paper's resolvers on Paper.
  Selector permissions (operator level 2 for `@e` etc.) are vanilla's.
- When a selector or name matches nothing, all four selector arguments fail with vanilla's translatable
  `No player was found` / `No entity was found` on every platform, as vanilla's `EntityArgument.getPlayers` /
  `getEntities` do; the executor never gets an empty list. Paper's `players()` / `entities()` resolvers return an empty
  list, so CommandLib's `PlayersArgument` / `EntitiesArgument` reject it there. (CommandAPI's `ManyPlayers` /
  `ManyEntities` allow empty results by default.)
- `UUIDArgument` is `partial` because it also accepts player names, which vanilla's `uuid` argument and CommandAPI's
  `UUIDArgument` do not. A UUID string always works.
- `OfflinePlayersArgument` and `UUIDsArgument` implement `@a` / `@r` themselves over `Bukkit.getOfflinePlayers()`;
  other selectors are rejected with `<input> is invalid selector`.

## Positions and worlds

| CommandLib | Result | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `LocationArgument(name[, centerIntegers])` | Bukkit `Location` / CommandLib `util.Location` (Forge) | vanilla `vec3`: `x y z`, `~`, `^`; integer x and z are centered (`1 64 1` → `1.5 64 1.5`) unless `centerIntegers` is false; world is the sender's | `LocationArgument` (`PRECISE_POSITION`) | `finePosition(centerIntegers)` | equivalent | equivalent | equivalent |
| `BlockPositionArgument` | Bukkit `Location` with integer coordinates in the sender's world / vanilla `BlockPos` | vanilla `block_pos`: integer `x y z`, `~`, `^`; neither the chunk nor the world height is checked | `LocationArgument` (`BLOCK_POSITION`) | `blockPosition()` | equivalent | equivalent | equivalent |
| `WorldArgument` (Bukkit) | `World` | dimension key such as `minecraft:overworld`, suggested by the server | `WorldArgument` | `world()` | equivalent | equivalent | missing |
| 2D location | — | — | `Location2DArgument` | `columnBlockPosition()`, `columnFinePosition()` | missing | missing | missing |
| rotation | — | — | `RotationArgument` | `rotation()` | missing | missing | missing |
| angle | — | — | `AngleArgument` | `angle()` | missing | missing | missing |
| axes | — | — | `AxisArgument` | `axes()` | missing | missing | missing |
| entity anchor | — | — | — | `entityAnchor()` | missing | missing | missing |
| height map | — | — | — | `heightMap()` | missing | missing | missing |

Notes:

- `BlockPositionArgument` resolves the position like Paper's `blockPosition()` resolver and vanilla's
  `Coordinates#getBlockPos`, not like `BlockPosArgument#getLoadedBlockPos`, so an unloaded or out-of-world position is
  not an error. Forge / NeoForge return the `BlockPos`; Bukkit returns the block's corner as a `Location`.
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
| `BlockPredicateArgument` | Bukkit: `Predicate<Block>`; Forge / NeoForge: `Predicate<BlockInWorld>` | vanilla `block_predicate`: `stone`, `#minecraft:logs`, `oak_stairs[facing=east]`, NBT | `BlockPredicateArgument` | `blockInWorldPredicate()` | equivalent | partial | equivalent |
| `ItemPredicateArgument` | `Predicate<ItemStack>` (Bukkit / vanilla) | vanilla `item_predicate`: `stone`, `#minecraft:logs`; NBT before 1.20.5, `*[damage>0]` component tests from then | `ItemStackPredicateArgument` | `itemPredicate()` | equivalent | equivalent | equivalent |

Notes:

- `BlockDataArgument` drops the block entity NBT the input may carry, since `BlockData` cannot hold it.
- The Bukkit `BlockPredicateArgument` tests a block in its world with its chunk loaded, as `/execute if block` does.
  On Forge / NeoForge, test `new BlockInWorld(level, pos, true)`. Paper added `blockInWorldPredicate()` in 1.21.8, so
  on Paper the argument needs 1.21.8 or later; its constructor throws `UnsupportedOperationException` on an older
  server, hence `partial`.
- The Bukkit `ItemPredicateArgument` tests a Bukkit item through a server copy of it. An unknown item or block fails
  while parsing; an unknown tag fails while parsing from 1.19 and, on Spigot and Forge before, when the command runs.
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
| `EntityTypeArgument` (Bukkit) | `EntityType` | entity type key such as `minecraft:zombie`; suggests registry keys | `EntityTypeArgument` | `resource(ENTITY_TYPE)` | equivalent | equivalent | missing |
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
- Forge / NeoForge have only `ResourceLocationArgument`; look the value up in the registry yourself.

## Scoreboard and game systems

| CommandLib | Result | Input | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|---|---|
| `TeamArgument` | Bukkit `Team` / vanilla `PlayerTeam` | team name; `scoreboard(Supplier)` selects another scoreboard | `TeamArgument` | — | equivalent | equivalent | equivalent |
| `ObjectiveArgument` (Bukkit) | `Objective` | objective name; `scoreboard(Supplier)` selects another scoreboard | `ObjectiveArgument` | — | equivalent | equivalent | missing |
| `ScoreboardDisplaySlotArgument` (Bukkit) | `DisplaySlot` | vanilla names: `list`, `sidebar`, `below_name`, `sidebar.team.red` | `ScoreboardSlotArgument` | `scoreboardDisplaySlot()` | partial | equivalent | missing |
| `GameModeArgument` (Bukkit) | `GameMode` | `survival`, `creative`, `adventure`, `spectator` | — | `gameMode()` | equivalent | equivalent | missing |
| `ChatColorArgument` (Bukkit) | Spigot: legacy `ChatColor`; Paper: `NamedTextColor` | vanilla color names such as `dark_red`, and `reset` on Spigot | `ChatColorArgument` | `namedColor()` | equivalent | equivalent | missing |
| objective criteria | — | — | `ObjectiveCriteriaArgument` | `objectiveCriteria()` | missing | missing | missing |
| math operation | — | — | `MathOperationArgument` | — | missing | missing | missing |

Notes:

- On Spigot without `scoreboard(...)`, `TeamArgument` uses vanilla's team argument (server scoreboard, vanilla
  suggestions and `Unknown team` error). Paper's and the custom-scoreboard variants are a word argument with
  CommandLib's suggestions and the "Incorrect argument for command" error.
- On Paper, `ScoreboardDisplaySlotArgument` and `ChatColorArgument` use Paper's `scoreboardDisplaySlot()` and
  `namedColor()`. On Spigot they are string arguments over a fixed list of vanilla names, so the client does not
  validate them and an unknown name is "Incorrect argument for command".
- Spigot's `ScoreboardDisplaySlotArgument` also accepts `belowName`, the name before 1.20.2, and offers the
  `sidebar.team.<color>` slots only where Bukkit's `DisplaySlot` has them (not on 1.16.5–1.18.2), hence `partial`.
- `ChatColorArgument` rejects formats such as `bold`, like vanilla's color argument. Spigot returns `ChatColor.RESET`
  for `reset`; `NamedTextColor` has no reset, and what Paper's `namedColor()` does with `reset` has not been checked.
- On Forge / NeoForge, `EnumArgument<GameType>` works as a game mode argument.

## Text, NBT, and other advanced arguments

| Capability | CommandAPI | Paper | Spigot | Paper | Forge / NeoForge |
|---|---|---|---|---|---|
| text component / JSON text | `ChatComponentArgument` | `component()` | missing | missing | missing |
| signed chat message | `ChatArgument` | `signedMessage()` | missing | missing | missing |
| style, hex color | — | `style()`, `hexColor()` | missing | missing | missing |
| NBT / SNBT compound | `NBTCompoundArgument` | — | missing | missing | missing |
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

## Velocity

A proxy has no worlds, entities, blocks, or registries, and the client knows only Brigadier's own argument types
through it, so the `velocity` artifact has a smaller set. The Spigot / Paper parity rule does not apply to it.

| CommandLib | Result | Input and suggestions | Status |
|---|---|---|---|
| `BooleanArgument`, `IntegerArgument`, `LongArgument`, `FloatArgument`, `DoubleArgument`, `StringArgument` | as on the servers | Brigadier types, validated by the client | equivalent to the server platforms |
| `DurationArgument`, `LiteralArgument`, `EnumArgument`, `ObjectArgument`, `NameableObjectArgument` | as on the servers | string arguments with CommandLib's suggestions | equivalent to the server platforms |
| `PlayerArgument` | Velocity `Player` | the name of a player online on the proxy, ignoring case (a word); suggests online players containing the input | — (no selectors) |
| `ServerArgument` | `RegisteredServer` | the name of a registered server, ignoring case (a word or a `"quoted name"`); suggests registered servers | — |

Notes:

- `PlayerArgument` and `ServerArgument` are string arguments that the proxy parses and suggests (`ask_server`), the
  `NameableObjectArgument` approach. An unknown name is "Incorrect argument for command".
- Selectors (`@a`, `@p`), UUIDs, and offline players are not accepted. Use `ObjectArgument` with your own map for
  other proxy objects.
- `UnparsedArgument` and the range arguments are missing: their argument types are CommandLib's or Minecraft's, not
  Brigadier's, so a proxy cannot send them to the client.
- `velocity-testing`'s `CommandTester` runs the real parsers. `PlayerArgument` and `ServerArgument` look up the
  players and servers added with `withFakePlayer` and `withFakeServer` on its Mockito `ProxyServer`.

## Not checked yet

These points are open, separate from the missing arguments below:

- Paper `PlayersArgument` / `EntitiesArgument` with a selector that matches nothing (see above).
- Error texts and suggestion lists have not been compared one by one with CommandAPI's; only input syntax and result
  types were compared.
- Mohist runs the Spigot module. Registry-backed arguments there have not been checked with modded registry entries.
- How each client renders suggestion tooltips; see [suggestions](suggestions.md).

## Missing arguments, by priority

The order weighs plugin demand, the CommandAPI / Paper parity gap, and how much of the work already exists.

1. **Text component and signed message** for Paper (`component()`, `signedMessage()`), and a portable component
   argument for Spigot and Forge / NeoForge.
2. **NBT / SNBT compound.**
3. **Generic registry argument** (Paper `resource(RegistryKey)` / `resourceKey(RegistryKey)` equivalent) on both
   Bukkit modules.
4. **Forge / NeoForge parity**: dimension / world, sound, biome, attribute, entity type, objective, game mode, and the
   other Bukkit-only registry arguments.
5. **Rotation, angle, 2D location, axes.**
6. **Score holder, objective criteria, math operation**, the remaining scoreboard set.
7. **Function, command, list, map** arguments.
8. `style`, `hexColor`, `heightMap`, `entityAnchor`, structure mirror / rotation.
9. **Vanilla `time` in game ticks** (`TimeArgument`, `time()`), for commands that schedule ticks rather than real time;
   `DurationArgument` covers real-time durations.

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

| Argument | `spigot-testing` | `paper-testing` | `forge-testing` / `neoforge-testing` / `fabric-testing` |
|---|---|---|---|
| Boolean, numbers, `StringArgument`, `UnparsedArgument`, `LiteralArgument`, `EnumArgument`, `ObjectArgument`, `NameableObjectArgument` | real | real | real |
| `GameModeArgument`, `ChatColorArgument`, `ScoreboardDisplaySlotArgument` | real | mock: vanilla names | — |
| `PlayerArgument`, `PlayersArgument` | mock: names, `@s`, `@p`, `@r`, `@a` (`withFakePlayer`) | mock: names, `@a` for the list variant (`withFakePlayer`) | real selectors over the fake players (`withPlayer`) |
| `EntityArgument`, `EntitiesArgument` | mock: names, `@s`, `@p`, `@r`, `@a`, `@e` (`withFakeEntity`) | mock: names, `@a` (`withFakeEntity`) | real selectors; only players exist |
| `OfflinePlayerArgument`, `OfflinePlayersArgument`, `UUIDArgument`, `UUIDsArgument` | static | static | `UUIDArgument`, `UUIDsArgument`, `GameProfileArgument`: online fake players only |
| `LocationArgument` | mock: `x y z` numbers | mock: `x y z` numbers, world `null` | real |
| `BlockPositionArgument` | mock: integer `x y z` | mock: integer `x y z`, world `null` | real |
| `ItemPredicateArgument`, `BlockPredicateArgument` | mock: an item or block id, matched by material; suggests ids | mock: a material id, matched by material; suggests ids (the block predicate needs the Paper 1.21.8 API on the test classpath) | real; tags are not loaded |
| `WorldArgument` | mock (`withFakeWorld("minecraft:overworld", world)`) | mock (`withFakeWorld(world)`, by key) | — |
| `BlockDataArgument` / `BlockStateArgument` | mock | mock (`withFakeBlockData`) | real |
| `ItemStackArgument` | mock | mock (`withFakeItemStack`) | real |
| `EnchantmentArgument` | mock | mock (`withFakeEnchantment`) | real (1.20.5+: from the bootstrapped registries) |
| `PotionEffectArgument` / `EffectArgument` | mock | mock (`withFakePotionEffectType`) | real |
| `ParticleArgument` | mock | mock (`withFakeParticle`) | real |
| `NamespacedKeyArgument` / `ResourceLocationArgument` | real key parsing | real key parsing | real |
| `SoundArgument`, `BiomeArgument`, `AttributeArgument`, `EntityTypeArgument` | static (`Registry`) | mock (`withFakeSound`, `withFakeBiome`, `withFakeAttribute`, `withFakeEntityType`) | — |
| `AdvancementArgument`, `LootTableArgument`, `RecipeArgument` | static | static | — |
| `TeamArgument` | mock vanilla type plus static scoreboard; real with `scoreboard(Supplier)` | static, or real with `scoreboard(Supplier)` | real with `scoreboard(Supplier)`; the server scoreboard is not available |
| `ObjectiveArgument` | static, or real with `scoreboard(Supplier)` | static, or real with `scoreboard(Supplier)` | — |

`paper-testing`'s registry mocks return the single fake value for any token, so they do not check that a key exists.
Tests that depend on unknown-key errors, real registries, tags, or worlds belong in integration tests.
