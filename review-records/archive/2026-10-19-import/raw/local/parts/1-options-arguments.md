## 1. Options と arguments

### 1.1 Option の種類拡張: count / repeatable / required / aliases / mutuallyExclusive (4848c3d8)

**変更内容**

- `Options.count(name[, short], max)` を追加。`-vvv` や `-v --verbose` の出現回数を `Integer` で返す。
- `CommandOption#repeatable(max)` で値 option を `List<T>` にする。値は入力順に並ぶ。
- `CommandOption#aliases(...)` で long name を増やせる。`option()` は他の option と重なる name / alias / short name を拒否し、name は Brigadier の unquoted word に制限した。
- `CommandOption#required()` を追加。指定されないと `Missing required option --x.` で失敗する。ただし入力が help の出る位置で終わる場合は help を表示する。
- `CommonCommand#mutuallyExclusive(...)` を追加。グループ内の option が 1 つ指定されると、残りの option を command tree から外す。`-jy` のような結合 flag にも効く。
- option のエラー (required の欠落、`requires(...)` 不成立) は新設の `OptionParseException` で投げる。positional argument のエラーは従来どおり `ArgumentParseException`。
- help に aliases と `(required)` / `(up to N times)` を表示する。
- option 数が同じ経路は子ノードを共有し、option の並び順ごとに tree が増えないようにした。
- **破壊的変更**: `CommandOption` の public な flag constructor を削除。代わりに `Options.flag` を使う。option のエラー型は `ArgumentParseException` から `OptionParseException` に変わった。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/Options.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandOption.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `common/src/main/java/dev/marulabs/commandlib/exception/OptionParseException.java`
- `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java`
- `common/src/test/java/dev/marulabs/commandlib/CommandOptionTest.java`
- `docs/options.md`, `docs/design/command-options.md`

**使用例**

```java
public final class ScanCommand extends Command {
    public ScanCommand() {
        super("scan");
        CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f').aliases("yes"));
        CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
        CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "").repeatable(3));
        CommandOption<String, CommandContext> env = option(Options.string("env", 'e', "").required());
        CommandOption<Boolean, CommandContext> json = option(Options.flag("json", 'j'));
        CommandOption<Boolean, CommandContext> yaml = option(Options.flag("yaml", 'y'));
        mutuallyExclusive(json, yaml);

        argument(new PlayerArgument("target")).execute((target, ctx) -> {
            int verbosity = ctx.getOption(verbose);      // -vv → 2
            List<String> tagList = ctx.getOption(tags);  // --tag a -t b → [a, b]
        });
    }
}
```

### 1.2 Option トークンの数え方の修正 (595e9af2)

`CommandRunner` は名前が `-` で始まるノードをすべて自 command の option として数えていた。そのため `/config -f <key> set` のような argument child で、親 `/config` の `-f` を自分の `-f` と取り違えていた。現在は option ノードの requirement が自分の command の option graph を指し、runner はそのノードだけを読む。Paper の dispatcher mirror のようにノードをコピーする platform でも requirement は引き継がれる。

- `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java` (新規)
- `common/src/main/java/dev/marulabs/commandlib/OptionNode.java` (新規)
- `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java`
- `common/src/test/java/dev/marulabs/commandlib/CommandOptionTest.java`

### 1.3 任意の argument を option の値に: `Options.of(...)` (5c3fa141)

**変更内容**

`Options.of(name[, short], argument)` を追加。CommandLib の任意の argument を option の値として使え、型・suggestions・validator・transformer は argument のものがそのまま効く。option が指定されなければ `optional(...)` の default を使い (実行ごとに計算するので `playerRequirer()` も使える)、optional でない argument の option は `required()` になる。variadic な argument は拒否するので、複数の値には `repeatable(max)` を使う。redirect 後に option graph がどの値型でも再 parse できるようになったため、これらの command も compact tree になる。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/Options.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandOption.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonArgument.java`
- `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionArgumentTest.java`
- `docs/options.md`

**使用例**

```java
public final class HealCommand extends Command {
    public HealCommand() {
        super("heal");
        CommandOption<Player, CommandContext> target =
                option(Options.of("target", 't', new PlayerArgument("target").optional(playerRequirer())));
        CommandOption<World, CommandContext> world = option(Options.of("world", new WorldArgument("world")));
        execute(ctx -> {
            Player player = ctx.getOption(target); // -t がなければ sender
            World in = ctx.getOption(world);       // optional でないので required
        });
    }
}
```

### 1.4 Option ごとの permission (1ba78745)

**変更内容**

`CommandOption#permission(DefaultPermission)` は `<command node>.option.<name>` の node を、`permission(node)` / `permission(node, DefaultPermission)` は任意の node を要求する。権限のない sender には、その option が command tree・suggestions・help のどこにも出ず、結合トークン (`-fs`) の中でも使えない。runner は実行前にもう一度権限を確認する。node は command の他の node と一緒に登録・export される。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommandOption.java`
- `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandTree.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandDocs.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionPermissionTest.java`
- `docs/options.md`, `docs/permissions.md`

**使用例**

```java
option(Options.flag("silent", 's')
              .permission(DefaultPermission.OP));    // myplugin.command.ban.option.silent
```

### 1.5 Positional argument の後ろにも option を許可: `optionsAnywhere()` (11f4294f)

**変更内容**

command で `optionsAnywhere()` を呼ぶと、option を各 argument の後ろにも書ける (`/ban Steve -s`)。option state は直前の argument ノードごとにも区別される。各 argument ノードは option 数ごとにコピーされ、コピー同士が redirect するので、player が受け取る tree には各ノードが 1 回ずつしか現れない。count の上限・mutuallyExclusive・permission は入力全体で数える。variadic argument の値の後ろには option を書けない。`CommandBuilder` にも `optionsAnywhere()` を追加した。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandBuilder.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionsAnywhereTest.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionTreeSizeTest.java`
- `docs/options.md`

**使用例**

```java
public final class BanCommand extends Command {
    public BanCommand() {
        super("ban");
        optionsAnywhere();
        CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's'));
        argument(new PlayerArgument("target"), new StringArgument("reason").optional(""))
                .execute((target, reason, ctx) -> {
                    boolean isSilent = ctx.getOption(silent);   // /ban Steve spam -s
                });
    }
}
```

### 1.6 OptionGraph の redirect 化と tree サイズ削減 (c31bc162, 86e93411, 89cdaaff, 94fda3c8, 2027d83e, 608256e9)

**変更内容 (perf)**

以前の option は literal トークンの tree で、option の並び順ごとに別の経路を持っていた。vanilla の command packet は経路ごとにノードをコピーするため、short flag が 6 個ある command では player 1 人につき 491,607 ノードになっていた。現在は、ある option count の state に最初に到達したトークンがその先のノードを持ち、同じ state に到達する他のトークンはそこへ redirect する。argument ノードも 1 回だけ構築して全 state で共有する。redirect をまたぐと Brigadier の context が新しくなるので、runner は tree の元になった option graph を使って入力から option を parse し直す。argument child と非 primitive の値型は、redirect なしの tree のまま残す。

`docs/benchmarks.md` の計測 (「expanded」は player ごとにコピーされる tree):

| tree | nodes 前 → 後 | expanded 前 → 後 |
|---|---|---|
| `Trees.command` (1 command) | 61 → 47 | 305 → 61 |
| `Trees.flags(4)` | 202 → 187 | 2,395 → 202 |
| `Trees.flags(6)` | 5,490 → 5,427 | 491,607 → 5,490 |

登録時の allocation も約 2 割減った (1000 command で 126 MB → 100 MB)。

**付随する修正**

- 86e93411: suggestion provider には入力全体の context が渡されるが、その arguments は最初の redirect までしか入っていない。そのため `execute run` の後や、label が `<plugin>:<label>` に redirect する Paper で、argument child が親の argument を読めなかった。`ChainedCommandContext` で、redirect で分かれた各部分を 1 つの context として読めるようにした。
- 89cdaaff: `/scan -f ` のように末尾に空白があると、Brigadier は redirect を追って空の残りを word argument として受理し、command を実行していた。runner がこれを `Incorrect argument` で拒否する。
- 94fda3c8: `PlatformAdapter.get()` は呼ばれるたびに reflection で adapter を作っていたので、最初の 1 つを `PlatformAdapterHolder` に保持するようにした。`hasPermission(command, source, prefix)` overload は削除した。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/OptionGraph.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandNodeCreator.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java`
- `common/src/main/java/dev/marulabs/commandlib/ChainedCommandContext.java`
- `common/src/main/java/dev/marulabs/commandlib/PlatformAdapterHolder.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionRedirectTest.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionTreeSizeTest.java`
- `docs/design/command-options.md`, `docs/benchmarks.md`

### 1.7 Optional / variadic な末尾 argument (6f508cd3)

**変更内容**

`optional(defaultValue)` / `optional(Extractor)` で branch の末尾の argument を省略可能にし、`variadic()` / `variadic(maxCount)` (default 上限 16) で末尾の argument を 1 回以上受け取って `List<T>` を返す。variadic は同じ名前のノードを繰り返し、各トークンを要素の型で parse し直すので、既存のどの argument も要素にできる。help には `[count]` や `<targets...>` と表示する。optional だけの branch は引数なしの command でも実行される。optional / variadic を末尾以外に置く、その後ろに child を付ける、引数なし command の action を 2 つ定義する、のいずれも登録時にエラーになる。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommonArgument.java`
- `common/src/main/java/dev/marulabs/commandlib/VariadicArgument.java` (新規)
- `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`
- `common/src/test/java/dev/marulabs/commandlib/OptionalVariadicArgumentTest.java` (spigot-testing / paper-testing にも同名テスト)
- `docs/arguments.md`, `docs/design/optional-variadic-arguments.md`

**使用例**

```java
new Command("arena") {{
    addChildren(new Command("invite") {{
        argument(new StringArgument("arena"), new PlayerArgument("players").variadic())
                .execute((arena, players, ctx) -> {});          // players: List<Player>
    }}, new Command("join") {{
        argument(new StringArgument("arena"), new PlayerArgument("player").optional(playerRequirer()))
                .execute((arena, player, ctx) -> {});           // 省略時は sender
    }});
}};
```

### 1.8 8 個以上の argument の branch (`ArgumentBuilder` 削除) (10dd80ea)

**変更内容**

`argument(a, b, ..., h)` (varargs) と `argument(List)` で、8 個以上の argument を持つ branch を作れるようにした。executor では `ctx.getArgument(argument)` で各値を型付きで読む。Kotlin DSL も同じ varargs 形式を使う。
**破壊的変更**: `argument(Consumer<ArgumentBuilder>)` と `ArgumentBuilder` を削除した。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/Dsl.kt`
- `common/src/test/java/dev/marulabs/commandlib/CommandStructureTest.java`
- `docs/kotlin.md`

**使用例**

```java
argument(x, y, z, yaw, pitch, world, speed, label).execute(ctx -> {
    double value = ctx.getArgument(x);
    World w = ctx.getArgument(world);
});
```

### 1.9 Java builder: `Command.builder(name)` / `CommandBuilder` (608e14f1)

**変更内容**

各 platform (`spigot`, `paper`, `velocity`, `modded`) の `Command.builder(name)` は、`common` の `CommandBuilder<CommandContext, Command>` を返す。subclass や初期化ブロックを書かずに、1 つの chain で fluent API と同じ `Command` を作れる。branch を返す `argument(...)` / `require(...)` は builder では callback を受け取り、platform 固有のメソッドには `configure(...)` から届く。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommandBuilder.java` (新規)
- `spigot/src/main/java/dev/marulabs/commandlib/Command.java` (paper / velocity / modded も同様)
- `spigot-testing/src/test/java/dev/marulabs/commandlib/CommandBuilderTest.java`, `paper-testing/src/test/java/dev/marulabs/commandlib/CommandBuilderTest.java`
- `docs/java-builder.md`

**使用例**

```java
CommandOption<Boolean, CommandContext> silent = Options.<CommandContext>flag("silent", 's');

Command heal = Command.builder("heal")
                      .permission(DefaultPermission.OP, "Heal players")
                      .option(silent)
                      .require(Command.playerRequirer(),
                               branch -> branch.execute((player, ctx) -> player.setHealth(20)))
                      .argument(new PlayerArgument("target"), new IntegerArgument("amount"),
                                branch -> branch.execute((target, amount, ctx) -> {
                                    if (!ctx.getOption(silent)) target.sendMessage("You were healed");
                                }))
                      .addChild(Command.builder("all").execute(ctx -> {}))
                      .build();
```

### 1.10 Argument 入力・suggestion のバグ修正 (d66d3aed, bb99da2d, 4b43c2ec)

- d66d3aed: `collectInputs` が variadic の繰り返しと誤認して、直前の literal と同名のノードを argument に含めていた。そのため `/amount <amount>` の入力が `amount 5` になり、validation エラーに command 名が 2 回出ていた。
- bb99da2d: 1.20.2 以前の vanilla text component argument は、cursor を component の 1 文字先まで進める。入力末尾ではその range が入力の外まで伸び、`StringIndexOutOfBoundsException` が起きていた。
- 4b43c2ec: item / block / entity selector など vanilla の parser は、独自の builder (`createOffset`) に suggestion を積む。CommandLib は Brigadier 側の builder に積まれた分しか返していなかったため、これらの補完が空になっていた。両方を merge して返すようにした。

- `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`
- `common/src/main/java/dev/marulabs/commandlib/ArgumentCommandNodeCreator.java`
- `common/src/test/java/dev/marulabs/commandlib/CommonCommandContextTest.java`, `common/src/test/java/dev/marulabs/commandlib/CommonArgumentTest.java`

### 1.11 新しい argument class (0367aee5, d5867cbd, 4ff649e6, dfe862dc, fb2c3c92, 816bc2cd, 594eda87)

**変更内容**

vanilla の argument type に乗せた argument を Spigot / Paper / Forge・NeoForge・Fabric (`modded`) に揃えて追加した。client が parse・ハイライト・補完する。Spigot は NMS wrapper (各 `nms` module の typed class)、Paper は `ArgumentTypes`、modded は vanilla の class を使う。

| class | platform | parse 対象 / 戻り値 |
|---|---|---|
| `IntegerRangeArgument`, `DoubleRangeArgument` | Spigot, Paper, modded | vanilla `int_range` / `float_range` (`1..10`, `..5`) → Guava `Range<Integer>` / `Range<Double>` |
| `DurationArgument` | Spigot, Paper, modded, Velocity | `1h30m`, `20t`, `2d` (単位 `d h m s t`、実時間) → `java.time.Duration` |
| `TimeArgument(name[, minimum])` | Spigot, Paper, modded | vanilla `time` (`20`, `10s`, `0.5d`、1 日 = 24000 tick) → `Integer` tick |
| `RegistryArgument<T>(name, registry)` | Spigot (`Registry<T>`), Paper (`RegistryKey<T>`), modded (`ResourceKey<Registry<T>>`) | 任意の registry の key → `T` |
| `BlockPositionArgument` | Spigot, Paper, modded | vanilla `block_pos` → Bukkit `Location` / vanilla `BlockPos` |
| `ItemPredicateArgument` | Spigot, Paper, modded | vanilla `item_predicate` → `Predicate<ItemStack>` |
| `BlockPredicateArgument` | Spigot, Paper (1.21.8+), modded | vanilla `block_predicate` → `Predicate<Block>` / `Predicate<BlockInWorld>` |
| `ComponentArgument` | Spigot, Paper, modded | text component → Bungee `BaseComponent` / Adventure `Component` / vanilla `Component` |
| `MessageArgument` | Spigot, Paper, modded | 残りの入力 (`/say` 相当、selector 解決済み、署名なし) → 同上 |
| `NBTCompoundArgument` | Spigot, Paper, modded | SNBT compound → SNBT `String` (Bukkit) / vanilla `CompoundTag` |

補足:

- `LocationArgument(name, centerIntegers)` を追加。`false` を渡すと整数の x / z を中央 (`+0.5`) に寄せない。
- Paper の `BlockPredicateArgument` は `blockInWorldPredicate()` を reflection で呼ぶため、1.21.8 未満では constructor が `UnsupportedOperationException` を投げる。
- 594eda87: `RegistryArgument`、Spigot の `ScoreboardDisplaySlotArgument`、1.19.4 未満での `TimeArgument` の minimum の失敗メッセージを、caption `UNKNOWN_REGISTRY_ENTRY` / `UNKNOWN_DISPLAY_SLOT` / `TICK_COUNT_TOO_LOW` (英語・日本語) で送るようにした。
- **破壊的変更** (dfe862dc): modded の `BlockPosArgument` を Bukkit 側と同じ名前の `BlockPositionArgument` に rename した。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/argument/CommonDurationArgument.java`
- `spigot/src/main/java/dev/marulabs/commandlib/argument/{IntegerRange,DoubleRange,Duration,Time,Registry,BlockPosition,ItemPredicate,BlockPredicate,Component,Message,NBTCompound}Argument.java` (`paper/`, `modded/` も同名)
- `paper/src/main/java/dev/marulabs/commandlib/util/brigadier/VanillaArgumentTypes.java`
- `spigot/src/main/java/dev/marulabs/commandlib/util/nms/argument/` と `nms/*/` の typed class、`tools/nms-codegen/`
- `spigot-testing/src/test/java/dev/marulabs/commandlib/argument/*ArgumentTest.java` (paper-testing / modded-testing も同様)
- `docs/arguments.md`, `docs/localization.md`

**使用例 (Spigot)**

```java
new Command("tools") {{
    addChildren(new Command("count") {{
        argument(new ItemPredicateArgument("item"), new IntegerRangeArgument("amount"))
                .execute((item, amount, ctx) -> { /* Predicate<ItemStack>, Range<Integer> */ });
    }}, new Command("check") {{
        argument(new BlockPositionArgument("pos"), new BlockPredicateArgument("block"))
                .execute((pos, block, ctx) -> ctx.sendMessage(String.valueOf(block.test(pos.getBlock()))));
    }}, new Command("mute") {{
        argument(new PlayerArgument("target"), new DurationArgument("for"))
                .execute((target, duration, ctx) -> { /* java.time.Duration */ });
    }}, new Command("enchant") {{
        argument(new RegistryArgument<>("enchantment", Registry.ENCHANTMENT), new TimeArgument("ticks", 1))
                .execute((enchantment, ticks, ctx) -> {});
    }});
}};
```

### 1.12 Registry key と vanilla 名への統一 (e6b1a4a1, 2bbc4dbd, 816bc2cd)

**変更内容**

- **破壊的変更** (e6b1a4a1): Spigot の `EntityTypeArgument` は `EnumArgument<EntityType>` をやめ、Paper と同じく namespaced key (`minecraft:zombie`) を `Registry.ENTITY_TYPE` から引くようにした。以前は `zombie` だけを受け付け、`unknown` まで補完に出していた。
- **破壊的変更** (2bbc4dbd): `ScoreboardDisplaySlotArgument` は Bukkit の enum 名ではなく vanilla の名前 (`list`, `sidebar`, `below_name`、Spigot では旧名 `belowName` も可、`sidebar.team.<color>`) で読む。`ChatColorArgument` は vanilla の color argument と同じく `bold` などの format を拒否する。Paper の `ChatColorArgument` は `ChatColor` ではなく `NamedTextColor` を返す (`ArgumentTypes.namedColor()`)。
- 816bc2cd: Spigot の `ScoreboardDisplaySlotArgument` / `ChatColorArgument` は NMS 経由で vanilla の `scoreboard_slot` / `color` 型を使うようになり、client が検証・補完する (戻り値の型は変わらない)。

**主要ファイル**

- `spigot/src/main/java/dev/marulabs/commandlib/argument/EntityTypeArgument.java`
- `spigot/src/main/java/dev/marulabs/commandlib/argument/ScoreboardDisplaySlotArgument.java`, `paper/src/main/java/dev/marulabs/commandlib/argument/ScoreboardDisplaySlotArgument.java`
- `spigot/src/main/java/dev/marulabs/commandlib/argument/ChatColorArgument.java`, `paper/src/main/java/dev/marulabs/commandlib/argument/ChatColorArgument.java`
- `spigot-testing/src/test/java/dev/marulabs/commandlib/argument/{EntityType,ScoreboardDisplaySlot,ChatColor}ArgumentTest.java`
- `docs/arguments.md`, `docs/migration/from-commandapi.md`, `docs/troubleshooting.md`

**使用例 (Paper)**

```java
argument(new ChatColorArgument("color"), new ScoreboardDisplaySlotArgument("slot"))
        .execute((color, slot, ctx) -> { /* NamedTextColor, DisplaySlot: /x dark_red below_name */ });
```

### 1.13 modded に Bukkit 相当の argument を追加 (b00165a5)

**変更内容**

vanilla の argument type か registry で実現できる Bukkit 側の argument を、Spigot / Paper と同じ名前で modded に追加した。戻り値は vanilla の型。対象は `WorldArgument`, `SoundArgument`, `BiomeArgument`, `AttributeArgument`, `EntityTypeArgument`, `AdvancementArgument`, `LootTableArgument`, `RecipeArgument`, `ObjectiveArgument`, `ScoreboardDisplaySlotArgument`, `GameModeArgument`, `ChatColorArgument`。registry の lookup は `RegistryEntries` にまとめ、`RegistryArgument` と共有する。

**主要ファイル**

- `modded/src/main/java/dev/marulabs/commandlib/argument/` (上記 12 class と `RegistryEntries.java`)
- `modded-testing/src/test/java/dev/marulabs/commandlib/argument/*ArgumentTest.java`
- `docs/arguments.md`

**使用例 (Forge / NeoForge)**

```java
argument(new WorldArgument("world"), new EntityTypeArgument("type"))
        .execute((level, type, ctx) -> { /* ServerLevel, EntityType<?> */ });
argument(new RegistryArgument<>("enchantment", Registries.ENCHANTMENT))
        .execute((enchantment, ctx) -> {});
```

### 1.14 実サーバーで見つかった reflection の修正 (7137e32f, 37a9fa81)

- 7137e32f: Spigot 26.2 で `ColorArgument` が `TeamColorArgument` に rename され、26.2 / 26.3 では `ChatColorArgument` が `NMSClassNotFoundException` を投げて plugin の enable に失敗していた。26.x module で class を実行時に探し、`ChatFormatting` / `TeamColor` を名前で変換するようにした。
- 37a9fa81: Spigot 1.16.5 では `ArgumentCriterionValue` の nested class (`$b` / `$a`) が難読化されたまま残っている。Mohist の remapper にはその mapping がないため、Mohist 1.16.5 で range argument の typed class が link できなかった。factory method は parse 後の bounds class から探し、parse 結果も class で読むようにした。

- `spigot/src/main/java/dev/marulabs/commandlib/util/nms/argument/NMSArgumentChatFormat.java`, `nms/spigot-26.1.2/src/main/java/dev/marulabs/commandlib/nms/spigot_26_1_2/NMSArgumentChatFormat_spigot_26_1_2.java`
- `spigot/src/main/java/dev/marulabs/commandlib/util/nms/argument/NMSArgument.java`, `nms/spigot-1.16.5/src/main/java/dev/marulabs/commandlib/nms/spigot_1_16_5/NMSArgument{Int,Float}Range_spigot_1_16_5.java`
- `tools/nms-codegen/mojang.py`, `tools/nms-codegen/spigot_legacy.py`
