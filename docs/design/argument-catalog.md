# Argument Catalog

この文書は、CommandLib の built-in argument の現状と、CommandAPI / Paper command API と比較した不足を整理する。

## Status の定義

| Status | 意味 |
|---|---|
| `Implemented` | public argument class が存在する。 |
| `Partial` | 近い argument はあるが、型、platform、version、または挙動が不足している。 |
| `Missing` | built-in argument として未実装。 |
| `Needs design` | 実装前に API shape または platform 差分の設計が必要。 |

## 現在の CommandLib argument

### Common

`common` には platform 非依存の基礎 argument がある。

| Argument | Status | Notes |
|---|---:|---|
| `CommonBooleanArgument` | `Implemented` | Bukkit / Forge の `BooleanArgument` の基礎。 |
| `CommonIntegerArgument` | `Implemented` | min / max 対応。 |
| `CommonLongArgument` | `Implemented` | min / max 対応。 |
| `CommonFloatArgument` | `Implemented` | min / max 対応。 |
| `CommonDoubleArgument` | `Implemented` | min / max 対応。 |
| `CommonStringArgument` | `Implemented` | `WORD`, `PHRASE_QUOTED`, `PHRASE`。 |
| `CommonLiteralArgument` | `Implemented` | literal node。 |
| `CommonEnumArgument` | `Implemented` | Java enum。 |
| `CommonObjectArgument` | `Implemented` | custom object mapping。 |
| `CommonNameableObjectArgument` | `Implemented` | named object mapping。 |

### Bukkit / Spigot / Paper 現行

| Argument | Status | Notes |
|---|---:|---|
| `BooleanArgument` | `Implemented` | common backed。 |
| `IntegerArgument` | `Implemented` | common backed。 |
| `LongArgument` | `Implemented` | common backed。 |
| `FloatArgument` | `Implemented` | common backed。 |
| `DoubleArgument` | `Implemented` | common backed。 |
| `StringArgument` | `Implemented` | word / quoted phrase / greedy phrase。 |
| `LiteralArgument` | `Implemented` | common backed。 |
| `EnumArgument` | `Implemented` | Java enum。 |
| `ObjectArgument` | `Implemented` | custom object mapping。 |
| `NameableObjectArgument` | `Implemented` | named object mapping。 |
| `UnparsedArgument` | `Implemented` | raw unparsed string。 |
| `PlayerArgument` | `Implemented` | single Bukkit player。 |
| `PlayersArgument` | `Implemented` | multiple Bukkit players。 |
| `EntityArgument` | `Implemented` | single Bukkit entity。 |
| `EntitiesArgument` | `Implemented` | multiple Bukkit entities。 |
| `OfflinePlayerArgument` | `Implemented` | Bukkit offline player。 |
| `OfflinePlayersArgument` | `Implemented` | multiple offline players。 |
| `UUIDArgument` | `Implemented` | UUID or player lookup behavior。 |
| `UUIDsArgument` | `Implemented` | multiple UUIDs / selector path。 |
| `LocationArgument` | `Implemented` | Bukkit `Location`。 |
| `WorldArgument` | `Implemented` | Bukkit / Paper `World`。Spigot は NMS `ArgumentDimension`、Paper は `ArgumentTypes.world()`。 |
| `BiomeArgument` | `Implemented` | Bukkit / Paper `Biome`。Spigot は enum argument、Paper は native registry argument。 |
| `BlockDataArgument` | `Implemented` | Bukkit `BlockData`。 |
| `ItemStackArgument` | `Implemented` | Bukkit `ItemStack`。 |
| `EnchantmentArgument` | `Implemented` | Bukkit `Enchantment`。 |
| `PotionEffectArgument` | `Implemented` | Bukkit `PotionEffect` / effect handling。 |
| `ParticleArgument` | `Implemented` | Bukkit particle data。 |
| `TeamArgument` | `Implemented` | Bukkit scoreboard team。 |
| `NamespacedKeyArgument` | `Implemented` | Bukkit `NamespacedKey`。namespace 省略時は `minecraft`。 |
| `SoundArgument` | `Implemented` | Bukkit / Paper `Sound` enum。 |
| `EntityTypeArgument` | `Implemented` | Bukkit / Paper `EntityType` enum。 |
| `AttributeArgument` | `Implemented` | Bukkit / Paper `Attribute` enum。 |
| `AdvancementArgument` | `Implemented` | Bukkit / Paper `Advancement` lookup by `NamespacedKey`。 |
| `LootTableArgument` | `Implemented` | Bukkit / Paper `LootTable` lookup by `NamespacedKey`。 |
| `RecipeArgument` | `Implemented` | Bukkit / Paper `Recipe` lookup by `NamespacedKey`。 |
| `ObjectiveArgument` | `Implemented` | Bukkit / Paper scoreboard objective lookup by name。 |
| `ScoreboardDisplaySlotArgument` | `Implemented` | Bukkit / Paper `DisplaySlot` enum。 |
| `ChatColorArgument` | `Implemented` | Bukkit / Paper legacy `ChatColor` enum。 |

### Forge 現行

| Argument | Status | Notes |
|---|---:|---|
| `BooleanArgument` | `Implemented` | common backed。 |
| `IntegerArgument` | `Implemented` | common backed。 |
| `LongArgument` | `Implemented` | common backed。 |
| `FloatArgument` | `Implemented` | common backed。 |
| `DoubleArgument` | `Implemented` | common backed。 |
| `StringArgument` | `Implemented` | word / quoted phrase / greedy phrase。 |
| `LiteralArgument` | `Implemented` | common backed。 |
| `EnumArgument` | `Implemented` | Java enum。 |
| `ObjectArgument` | `Implemented` | custom object mapping。 |
| `NameableObjectArgument` | `Implemented` | named object mapping。 |
| `UnparsedArgument` | `Implemented` | raw unparsed string。 |
| `PlayerArgument` | `Implemented` | single Forge server player。 |
| `PlayersArgument` | `Implemented` | multiple Forge server players。 |
| `EntityArgument` | `Implemented` | single Forge entity。 |
| `EntitiesArgument` | `Implemented` | multiple Forge entities。 |
| `UUIDArgument` | `Implemented` | UUID or player lookup behavior。 |
| `UUIDsArgument` | `Implemented` | multiple UUIDs / selector path。 |
| `LocationArgument` | `Implemented` | Forge / Minecraft position。 |
| `BlockPosArgument` | `Implemented` | block position。 |
| `BlockStateArgument` | `Implemented` | block state input。 |
| `ItemStackArgument` | `Implemented` | Forge `ItemStack`。 |
| `EnchantmentArgument` | `Implemented` | Forge enchantment。1.20.5+ は `Holder<Enchantment>`。 |
| `EffectArgument` | `Implemented` | Forge effect。1.20.5+ は `Holder<MobEffect>`。 |
| `ParticleArgument` | `Implemented` | Forge particle data。 |
| `TeamArgument` | `Implemented` | Forge scoreboard team。 |
| `GameProfileArgument` | `Implemented` | game profile list。 |
| `ResourceLocationArgument` | `Implemented` | Minecraft `ResourceLocation`。namespace 省略時は `minecraft`。 |

## Competitive coverage

### Primitive / string / literal

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| boolean | `Implemented` | Yes | Brigadier native | Done |
| integer / long / float / double | `Implemented` | Yes | Brigadier native | Done |
| ranged integer / long / float / double | `Partial` | Yes | Brigadier native / range providers | P1 |
| integer range object | `Missing` | Yes | Yes | P1 |
| double range object | `Missing` | Yes | Yes | P1 |
| string word / quoted / greedy | `Implemented` | Yes | Brigadier native | Done |
| text argument | `Partial` | Yes | Brigadier native | P2 |
| literal | `Implemented` | Yes | Brigadier native | Done |
| multi literal | `Missing` | Yes | Possible via literals | P2 |
| command argument | `Missing` | Yes | Brigadier command parsing | P2 |

### Entity / player / profile

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| one player | `Implemented` | Yes | Yes | Done |
| many players | `Implemented` | Yes | Yes | Done |
| one entity | `Implemented` | Yes | Yes | Done |
| many entities | `Implemented` | Yes | Yes | Done |
| offline player | `Implemented` | Not primary | Not primary | Done |
| UUID | `Implemented` | Yes | Yes | Done |
| player profile / game profile | `Partial` | Yes | Yes | P1 |
| score holder single / multiple | `Missing` | Yes | Brigadier / Paper support | P1 |

### World / position / rotation

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| world | `Implemented` | Yes | Yes | Done |
| location / fine position | `Partial` | Yes | Yes | P1 |
| 2D location | `Missing` | Yes | Paper location docs | P2 |
| block position | `Partial` | Forge only | Yes | P1 |
| rotation | `Missing` | Yes | Related Paper enum / angle support | P1 |
| angle | `Missing` | Yes | Brigadier / Paper support | P1 |
| entity anchor | `Missing` | Noted in Paper | Yes | P2 |
| height map | `Missing` | Paper-specific | Yes | P3 |

### Blocks / items / registry-backed values

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| block state / block data | `Implemented` | Yes | Yes | Done |
| block predicate | `Missing` | Yes | Predicates | P1 |
| item stack | `Implemented` | Yes | Yes | Done |
| item predicate | `Missing` | Yes | Yes | P1 |
| namespaced key | `Implemented` | Yes | Yes | Done |
| resource key | `Missing` | Paper-specific | Yes | P1 |
| generic registry resource | `Missing` | Partial via typed args | Yes | P1 |
| biome | `Implemented` | Spigot は enum argument。Paper は native registry argument。 | Yes | Done |
| loot table | `Implemented` | Yes | Registry | Done |
| recipe | `Implemented` | Yes | Registry | Done |
| sound | `Implemented` | Yes | Registry / key | Done |
| advancement | `Implemented` | Yes | Registry / key | Done |

### Scoreboard / game systems

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| team | `Implemented` | Yes | Brigadier / Paper support | Done |
| objective | `Implemented` | Yes | Paper objective criteria support | Done |
| objective criteria | `Missing` | Yes | Yes | P1 |
| scoreboard display slot | `Implemented` | Yes | Yes | Done |
| math operation | `Missing` | Yes | Brigadier support | P2 |
| game mode | `Implemented` | Spigot は `EnumArgument<GameMode>` ラッパー。Paper は `ArgumentTypes.gameMode()` ネイティブ実装。 | Yes | Done |
| entity type | `Implemented` | Bukkit / Paper enum argument。 | Yes | Done |
| attribute | `Implemented` | Bukkit / Paper enum argument。 | Yes | Done |

### Chat / Adventure / text component

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| legacy Bukkit component output | `Partial` | N/A | Not preferred on Paper | P0 |
| Adventure `Component` argument | `Missing` | Yes | Yes | P1 for Paper |
| signed message | `Missing` | Yes | Yes | P1 for Paper |
| named text color | `Missing` | Yes | Yes | P2 |
| style | `Missing` | Paper-specific | Yes | P2 |
| MiniMessage-friendly APIs | `Missing` | Not core | Adventure ecosystem | P2 |

### NBT / predicates / advanced command data

| Capability | CommandLib | CommandAPI | Paper | Priority |
|---|---:|---:|---:|---:|
| NBT / SNBT compound | `Missing` | Yes | Brigadier / Minecraft-specific | P1 |
| custom argument | `Partial` | Yes | Yes | P1 |
| list argument | `Missing` | Yes | Custom / composite | P2 |
| map argument | `Missing` | Yes | Custom / composite | P2 |
| predicate arguments | `Missing` | Yes | Paper predicates docs | P1 |
| function argument | `Missing` | Yes | Datapack function path | P1 |
| time / duration | `Missing` | Yes | Yes | P1 |
| template mirror / rotation | `Missing` | Paper-specific | Yes | P3 |

## Priority backlog

### P0: catalog accuracy

- Confirm each current argument return type and behavior with tests.
- Split Bukkit/Spigot/Paper-specific argument concerns before GA module split.
- Decide whether `LocationArgument` should represent block position, fine position, or both.
- Decide whether `PotionEffectArgument` should return type, instance, or both.
- Decide how Adventure component arguments fit into `paper` without leaking into `spigot`.

### P1: parity blockers

- `[x]` `NamespacedKeyArgument` / `ResourceLocationArgument`。
- Generic registry-backed arguments。
- `[x]` `BiomeArgument`。
- `[x]` `SoundArgument`。
- `[x]` `AdvancementArgument`。
- `[x]` `LootTableArgument`。
- `[x]` `RecipeArgument`。
- `[x]` `EntityTypeArgument`。
- `[x]` `AttributeArgument`。
- `[x]` `ObjectiveArgument`。
- `ObjectiveCriteriaArgument`。
- `ScoreHolderArgument`。
- `BlockPredicateArgument`。
- `ItemStackPredicateArgument`。
- `NBTCompoundArgument` / `SNBTArgument`。
- `TimeArgument`。
- `AngleArgument` / `RotationArgument`。
- Paper `ComponentArgument`。
- Paper `SignedMessageArgument`。

### P2: developer experience

- `ListArgument`。
- `MapArgument`。
- `MultiLiteralArgument`。
- `CommandArgument`。
- `[x]` `ChatColorArgument`。
- `NamedTextColorArgument`。
- `StyleArgument`。
- `[x]` `ScoreboardDisplaySlotArgument`。
- `MathOperationArgument`。
- Optional / default argument helpers。
- Alias-aware enum argument。

### P3: platform-specific expansion

- Paper `HeightMapArgument`。
- Paper `TemplateMirrorArgument`。
- Paper `TemplateRotationArgument`。
- Folia-safe async suggestion argument behavior。
- Velocity command argument equivalents。
- NeoForge / Fabric registry argument equivalents。

## Design notes

- Common argument concepts should live in `common`; platform modules should only adapt parse, suggestion, permission, message, and native registration behavior.
- Spigot arguments must avoid Paper-only APIs.
- Paper arguments may use Adventure and Paper command APIs directly.
- Registry-backed arguments should use a common concept where possible, but each platform needs its own registry adapter.
- For registry-backed Bukkit/Paper values, prefer Paper-native registry key semantics as the long-term public behavior.
  Arguments such as `SoundArgument`, `EntityTypeArgument`, and `AttributeArgument` should converge on `minecraft:...`
  style input and suggestions equivalent to Paper `ArgumentTypes.resource(RegistryKey...)`, with Spigot adapting to the
  same behavior where feasible.
- Argument docs must state return type, accepted syntax, default suggestions, permission filtering behavior, and supported platforms.

## External reference points

- CommandAPI argument list includes many advanced arguments such as advancement, biome, block predicate, ranges, entity selectors, functions, loot table, map/list, NBT, objective, player profile, recipe, rotation, score holder, sound, time, UUID, and world.
- Paper command API groups arguments into Minecraft-specific, location, entities and players, registry, Paper-specific, enums, predicates, and Adventure categories.
- Paper `ArgumentTypes` includes modern entries such as `component()`, `signedMessage()`, `resource(...)`, `resourceKey(...)`, `gameMode()`, `scoreboardDisplaySlot()`, `time(...)`, `world()`, and `uuid()`.
