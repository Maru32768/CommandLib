# CommandLib 作業まとめ（2026-10-15〜16）

`docs/roadmap.md` の項目を、subagent と git worktree を使って並行で進めた作業のまとめ。

- **範囲**: master の `2c8ffd7a..1b8f17dc`（約 190 commit、1,666 ファイル、+65,533 / −3,167 行）。commit 一覧は `git log --reverse 2c8ffd7a..master`。
- **進め方**: 各 agent が自分の worktree の branch で実装し、master に fast-forward で取り込んだ。`docs/roadmap.md` / `docs/roadmap-history.md` は取り込みごとに更新した。
- **検証**: 各変更は対象 module の focused test で確認した（`spotlessCheck`、`:common:test`、`:spigot:test`、`modded-testing` の各 node など）。最後に Docker 上の実サーバー 29 target で integration test を回し、すべて通った（5.8 節）。
- **使用例のコード**: 現在のソースとシグネチャを照合して書いた。ただし compile はしていない。

## 目次

1. [Options と arguments](#1-options-と-arguments)
2. [実行と runtime](#2-実行と-runtime)
3. [Message・help・documentation](#3-messagehelpdocumentation)
4. [Annotation API と Kotlin DSL](#4-annotation-api-と-kotlin-dsl)
5. [Platform・testing・build](#5-platformtestingbuild)
6. [判断が必要な箇所と未対応事項](#6-判断が必要な箇所と未対応事項)

## 破壊的変更の一覧

0.17.x からの移行で影響があるもの。詳細は各節を参照。

| 変更 | commit | 節 |
|---|---|---|
| `CommandOption` の public flag constructor を削除（`Options.flag` を使う）。option のエラー型が `OptionParseException` になった | 4848c3d8 | 1.1 |
| `argument(Consumer<ArgumentBuilder>)` と `ArgumentBuilder` を削除（8 個以上の argument も `argument(...)` で書ける） | 10dd80ea | 1.8 |
| modded の `BlockPosArgument` を `BlockPositionArgument` に rename | dfe862dc | 1.11 |
| Spigot の `EntityTypeArgument` を namespaced key（`minecraft:zombie`）で読むように変更 | e6b1a4a1 | 1.12 |
| `ScoreboardDisplaySlotArgument` / color を vanilla の名前で読むように変更 | 2bbc4dbd | 1.12 |
| `sendSuccess(String, boolean)` / `sendSuccess(RichText, boolean)` が抽象メソッドになった。失敗は `CommandSyntaxException` を投げる。`CommandTester#execute` が `int` を返す | 2f8d4d37 | 2 |
| preprocess / confirm / cooldown を middleware で実装し直した。`disableParentPreprocess()` を `disableParentMiddleware()` に改名 | fecaf526 | 2 |
| 組み込み message を caption（key + locale 別 bundle）にした。一部の英語の文言が変わった | 1458ede5 | 3 |
| Paper の resolve error を vanilla の translatable component で送る。selector の結果が空なら reject する | a3f8ae9b | 3 |


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

## 2. 実行と runtime

### コマンド結果と vanilla 風フィードバック (2f8d4d37, 201bba85, f253444c)

**変更内容**

- `ctx.setResult(int)` を追加。`/execute store result` が保存する値になる（未設定なら 1）。
- 失敗（`CommandPrerequisiteException`、preprocess が false、permission / argument / prerequisite の失敗、予期しない例外）は失敗メッセージを持つ `CommandSyntaxException` として platform に投げるようにした。vanilla のエラーと同じ扱いになり、`store result` / `store success` はどちらも 0 を保存する。silent source（function）や `/execute as @a` の各 target には失敗メッセージが届かない。
- `sendSuccess` は全 platform で `CommandSourceStack#sendSuccess` を通す。`sendCommandFeedback` gamerule と silent source に従う。`sendSuccess(message, true)` は他の OP（灰色斜体）にも送り、`logAdminCommands` が有効ならログにも出す。Spigot は各 typed NMS module の `NMSCommandListenerWrapper` から、Paper は reflection で source にアクセスする。
- **破壊的変更**: `CommonCommandContext` の抽象メソッドが `sendSuccess(String, boolean)` / `sendSuccess(RichText, boolean)` になり、引数 1 つの `sendSuccess` は `final` になった。失敗時の戻り値は 0 ではなく例外になった。function 内の `sendSuccess` は出力されなくなった。testers の `CommandTester#execute` は `void` から結果の `int` を返すように変わった（失敗時は 0）。
- `sendSuccessFor(targets, single, multiple[, broadcastToOps])`（201bba85）は、target が 1 つなら名前を、複数なら数を入れた vanilla 風の文言でフィードバックを送る。
- `ctx.fail(...)`（f253444c）は、checked exception を投げられない typed executor（`(target, ctx) -> ...`）からでも `CommandPrerequisiteException` を sneaky throw してコマンドを失敗させる。`String...`、`Caption`、`Caption` + placeholders、例外そのものを受ける overload があり、`throw ctx.fail(...)` と書く。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandFailure.java`
- `spigot/src/main/java/dev/marulabs/commandlib/CommandContext.java`, `spigot/src/main/java/dev/marulabs/commandlib/util/nms/command/NMSCommandListenerWrapper.java`
- `paper/src/main/java/dev/marulabs/commandlib/CommandContext.java`, `modded/src/main/java/dev/marulabs/commandlib/CommandContext.java`
- `common/src/test/java/dev/marulabs/commandlib/CommandResultTest.java`, `common/src/test/java/dev/marulabs/commandlib/CommonCommandContextTest.java`
- `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/ScenarioTest.java`（paper も同様）
- `docs/errors.md`（Results and failures）、`README.md`（Feedback And Results）

**使用例**

```java
argument(new PlayersArgument("targets")).execute((targets, ctx) -> {
    if (targets.isEmpty()) {
        throw ctx.fail("No player was found");
    }
    targets.forEach(x -> x.setHealth(20));
    // "Healed Steve" / "Healed 3 players"。true で他の OP とログにも送る
    ctx.sendSuccessFor(targets, x -> "Healed " + x.getName(), count -> "Healed " + count + " players", true);
    ctx.setResult(targets.size()); // /execute store result score @s count run ...
});
```

### Middleware と `executeAsync` (fecaf526)

**変更内容**

- コマンドの実行は middleware chain（`addMiddleware`, `CommandMiddleware`）を通るようになった。middleware は `(ctx, next)` を受け、`next.proceed()` が返す `CompletionStage` を返す。前処理（check、`ctx.put`）、後処理（`whenComplete`）、例外の変換（`exceptionally`）を sync / async の両方で同じように書ける。`proceed()` は例外を投げず、失敗は stage を例外で完了させる。
- **破壊的変更**: `addPreprocess`、`confirm()`、`cooldown()` は middleware として実装し直した。`disableParentPreprocess()` は `disableParentMiddleware()` に改名した。
- `executeAsync` を command、argument、argument builder、全 typed branch に追加。executor は `CompletionStage` を返し、stage が完了した時点でコマンドが終わる。
- `CommandScheduler`（`sync(ctx)` / `async(ctx)`、`CommandScheduler.of(sync, async)`）で platform ごとの executor を抽象化した。Spigot / Paper は Bukkit scheduler、Forge / NeoForge は source の `MinecraftServer` と CommandLib の daemon pool、Velocity は呼び出しスレッドと proxy scheduler を使う。testers は既定で `Runnable::run` を使い、builder の `scheduler(...)` で差し替えられる。
- `ctx.supplyAsync` / `ctx.runAsync` は async executor で作業し、sync thread で完了する future を返す。`ctx.syncExecutor()` / `ctx.asyncExecutor()` も公開した。
- dispatch が返る時点で stage が未完了なら、その時点の result（既定 1）を返す。後から起きた失敗は sender に直接送り、sync thread で `UncaughtExceptionHandler` に渡す。`TimeoutException` は "The command timed out." として報告する。
- spigot-testing は sender を Brigadier source として渡すようにした。`execute` が返った後に動く follow-up からも sender を参照できる。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/command/CommandMiddleware.java`, `common/src/main/java/dev/marulabs/commandlib/command/AsyncCommandExecutor.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandScheduler.java`, `common/src/main/java/dev/marulabs/commandlib/CommandRunner.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`, `common/src/main/java/dev/marulabs/commandlib/branch/*`
- `spigot/src/main/java/dev/marulabs/commandlib/BukkitCommandScheduler.java`, `paper/src/main/java/dev/marulabs/commandlib/BukkitCommandScheduler.java`, `modded/src/main/java/dev/marulabs/commandlib/ServerCommandScheduler.java`, `velocity/src/main/java/dev/marulabs/commandlib/VelocityCommandScheduler.java`
- `common/src/test/java/dev/marulabs/commandlib/CommandMiddlewareTest.java`, `common/src/test/java/dev/marulabs/commandlib/AsyncExecutionTest.java`, `*-testing/src/test/java/dev/marulabs/commandlib/AsyncCommandTest.java`
- `docs/execution.md`, `docs/design/execution-pipeline.md`（4c3d7524 で追加）

**使用例**

```java
addMiddleware((ctx, next) -> {
    if (Maintenance.isActive()) {
        throw new CommandPrerequisiteException("The server is in maintenance.");
    }
    long start = System.nanoTime();
    return next.proceed()
               .whenComplete((ignored, e) -> log(ctx.getSender(), System.nanoTime() - start, e));
});

argument(new OfflinePlayerArgument("player")).executeAsync((player, ctx) ->
        ctx.supplyAsync(() -> database.loadStats(player.getUniqueId()))   // async executor
           .thenAccept(stats -> ctx.sendSuccess(player.getName() + " has " + stats.kills() + " kills"))); // sync thread
```

### Folia 対応 (35263cb9, b8c86169)

**変更内容**

- `paper` artifact は実行時に Folia（`io.papermc.paper.threadedregions.RegionizedServer`）を検出し、Bukkit scheduler の代わりに `FoliaCommandScheduler` を使う。sync executor は source を所有するスレッドになる。`/execute as` の entity なら entity scheduler、command block なら region、console なら global region。すでにそのスレッド上にいるときは即時に実行する。async 作業は Folia の async scheduler に渡す。
- Folia は data pack function を実行せず reload もないため、`ServerFunctions` は再コンパイルしない。`COMMANDS` event を逃した登録は `Bukkit.reloadData()` を呼ばず、警告を出す。
- `spigot` artifact は Folia 上では `CommandLib.register` で `UnsupportedOperationException` を投げる。何かを変更する前に投げ、paper artifact を案内する。

**主要ファイル**

- `paper/src/main/java/dev/marulabs/commandlib/FoliaCommandScheduler.java`, `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `paper/src/main/java/dev/marulabs/commandlib/ServerFunctions.java`
- `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java`
- `paper-testing/src/test/java/dev/marulabs/commandlib/FoliaCommandSchedulerTest.java`
- `docs/execution.md`（Folia 節、4cc0a9a0）

**使用例**（API は変わらず、`plugin.yml` で宣言するだけ）

```yaml
folia-supported: true
```

### 修正: modded の sync executor (37f4f909)

sync executor が `MinecraftServer` そのものだった。`MinecraftServer#execute` は server が task を実行している間は task を queue に積むため、player が打ったコマンドの executor が dispatch 中に動かず、後続のコマンドからその結果が見えなかった。server thread 上では即時に実行するように直した。`refresh` / `refreshAll` も同じ理由で、server thread 上なら tree を即時に送る。Forge 1.20.1 / NeoForge 1.21.1 の実サーバーで見つかった。

- `modded/src/main/java/dev/marulabs/commandlib/ServerCommandScheduler.java`, `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java`
- `modded-testing/src/test/java/dev/marulabs/commandlib/ServerCommandSchedulerTest.java`

### `confirm()` / `cooldown(Duration)` (5dba1116, f59d49a0)

**変更内容**

- `confirm()` を追加。初回の実行では確認を求め（クリックで再実行できる `[Confirm]` 付き）、10 秒以内に同じ入力が来たら実行する。`confirm(Confirmation.within(...).message(...).clock(...))` でカスタマイズできる。
- `cooldown(Duration)` / `cooldown(Cooldown)` を追加。`Cooldown.of(d).bypassPermission(...).message(...).clock(...)` で設定する。unique id ごとに管理するため、console と command block は対象外になる。
- 実行順は cooldown check → confirmation → その他の middleware → cooldown の開始。確認されなかった実行では cooldown が始まらない。どちらも argument branch と、自分で設定していない子 command に継承される。
- f59d49a0 で照会とリセットを追加した: `remaining(UUID)`, `remaining(ctx)`, `reset(UUID)`, `resetAll()`。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/Confirmation.java`, `common/src/main/java/dev/marulabs/commandlib/Cooldown.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `common/src/test/java/dev/marulabs/commandlib/CommonCommandConfirmCooldownTest.java`
- `README.md`（Confirmation and Cooldown）、`docs/errors.md`

**使用例**

```java
private final Cooldown cooldown = Cooldown.of(Duration.ofMinutes(5))
                                          .bypassPermission("myplugin.cooldown.bypass");

public ResetCommand() {
    super("reset");
    confirm();              // もう一度同じ入力を 10 秒以内に送ると実行
    cooldown(cooldown);
    execute(ctx -> ctx.sendSuccess("Reset."));
}

// 管理コマンドなどから
Duration left = cooldown.remaining(player.getUniqueId());
cooldown.reset(player.getUniqueId());
```

### Context values: `provide` / `ctx.put` / `ctx.get` / `ContextKey` (d98d2c97, 28465b3f, 4e2ac97b)

**変更内容**

- 型付きの値を実行に載せる仕組みを追加した。command が `provide(type|key, ctx -> ...)` で提供し、middleware が `ctx.put(...)` で設定し、どこからでも `ctx.get(...)`（無ければ `IllegalStateException`）または `ctx.find(...)`（`Optional`）で読める。sender mapping、middleware からの値渡し、dependency 注入をこの 1 つの仕組みでまかなう。
- `ContextKey.of(Type.class)` は型のキー、`ContextKey.of("name", Type.class)` は同じ型の 2 つ目の値のキー。provider は初回の読み取り時に 1 回だけ実行され、その実行中は値を使い回す。provider は argument branch と子 command に継承される。`disableParentMiddleware()` は provider に影響しない。provider が投げた `CommandPrerequisiteException` はコマンドを失敗させる。
- 28465b3f で Java builder（`CommandBuilder#provide`）にも追加した。4e2ac97b は fluent / builder / annotated の各スタイルで sender mapping を確認する tests。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/ContextKey.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`, `common/src/main/java/dev/marulabs/commandlib/CommandBuilder.java`
- `common/src/test/java/dev/marulabs/commandlib/ContextValueTest.java`, `spigot-testing/src/test/java/dev/marulabs/commandlib/SenderMappingTest.java`, `paper-testing/src/test/java/dev/marulabs/commandlib/SenderMappingTest.java`
- `docs/context-values.md`（6a05b8bb で追加）、`docs/java-builder.md`

**使用例**

```java
static final ContextKey<Instant> STARTED = ContextKey.of("started", Instant.class);

public TeamCommand(Games games) {
    super("team");
    provide(GamePlayer.class, ctx -> games.of(playerRequirer().extract(ctx))); // 実行ごとに 1 回
    addMiddleware((ctx, next) -> {
        ctx.put(STARTED, Instant.now());
        return next.proceed();
    });
    addChildren(new Command("show") {{
        execute(ctx -> ctx.sendMessage(ctx.get(GamePlayer.class).team()));
    }});
}
```

### `CommandDefaults`: 登録全体の middleware と provider (30b4e6c9)

**変更内容**

- `CommandDefaults.of(ContextType.class).addMiddleware(...).provide(...)` にまとめた middleware と provider を、登録したすべての command に適用する。全 platform に `CommandLib.register` の overload を追加した（Spigot / Paper: `register(plugin[, permissionPrefix], defaults, commands)`、Velocity: `register(plugin, proxy[, permissionPrefix], defaults, commands)`、modded: `register(permissionPrefix, defaults, commands)`）。Paper の `bootstrap` と testers の builder（`defaults(...)`）にも追加した。
- この middleware は各 command の cooldown / confirmation / middleware より先に動き、`disableParentMiddleware()` した子 command でも動く。provider は command 側が提供していないキーにだけ値を与える。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommandDefaults.java`, `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`
- `spigot/.../CommandLib.java`, `paper/.../CommandLib.java`, `velocity/.../CommandLib.java`, `modded/.../CommandLib.java`（いずれも `src/main/java/dev/marulabs/commandlib/`）
- `*-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java`, `common/src/test/java/dev/marulabs/commandlib/ContextValueTest.java`
- `docs/execution.md`（Registration-wide middlewares and providers）、`docs/context-values.md`

**使用例**

```java
CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class)
        .addMiddleware(new AuditMiddleware(getLogger()))
        .provide(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)));

CommandLib.register(this, defaults, List.of(new TeamCommand(), new ArenaCommand()));

CommandTester tester = CommandTester.builder()
                                    .command(new TeamCommand())
                                    .permissionPrefix("myplugin.command")
                                    .defaults(defaults)
                                    .build();
```

### `CommandLib.refresh` / `refreshAll` と `visibleWhen` (6d113014, fd559a9f)

**変更内容**

- server は permission が変わっても command tree を送り直さない。そのため、実行時に付与・剥奪した command が rejoin するまで隠れたまま、または見えたままになっていた。`CommandLib.refresh(player)` / `refreshAll()` で tree を送り直せるようにした。Spigot / Paper は main thread 専用で、それ以外のスレッドからは `IllegalStateException` を投げる。Forge / NeoForge / Fabric はどのスレッドからでも呼べ、tree は server thread で送る。Velocity には API がないため提供しない。
- `visibleWhen(predicate)` を command、argument branch、Java builder に追加した。permission 以外の状態（例: game が始まっていないときだけ `/game start` を出す）で表示を切り替える。predicate が false の sender には、command tree、suggestion、help のどこにも出ず、実行すると unknown command として扱われる。状態が変わったら `refresh` を呼ぶ。

**主要ファイル**

- `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `paper/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonCommand.java`, `common/src/main/java/dev/marulabs/commandlib/branch/ArgumentBranch.java`, `common/src/main/java/dev/marulabs/commandlib/CommandNodeCreator.java`, `common/src/main/java/dev/marulabs/commandlib/PlatformAdapter.java`, `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java`
- `spigot/src/test/java/dev/marulabs/commandlib/CommandTreeRefreshTest.java`, `paper-testing/src/test/java/dev/marulabs/commandlib/CommandTreeRefreshTest.java`, `common/src/test/java/dev/marulabs/commandlib/VisibilityTest.java`
- `docs/permissions.md`（Refreshing the command tree）

**使用例**

```java
addChildren(new Command("start") {{
    visibleWhen(ctx -> !game.isRunning());
    execute(ctx -> {
        game.start();
        CommandLib.refreshAll(); // これで player には /game stop が見える
    });
}}, new Command("stop") {{
    visibleWhen(ctx -> game.isRunning());
    execute(ctx -> game.stop());
}});
```

### Paper context の Spigot との parity (dfba1c85, 5e81f3f9)

**変更内容**

- Paper の `Command` に `playerRequirer()` / `consoleRequirer()`（`Extractor`）を追加した。Spigot と同じく `require(...)` で型付き sender を executor に渡せる。
- Paper の `CommandContext` に `getEntity()`（`CommandSourceStack#getExecutor`、nullable）、`getLocation()`（`/execute at|positioned|rotated|in` を反映、yaw / pitch 付き）、`getWorld()` を追加した。paper-testing の source は entity sender を executor として返す。

**主要ファイル**

- `paper/src/main/java/dev/marulabs/commandlib/Command.java`, `paper/src/main/java/dev/marulabs/commandlib/CommandContext.java`
- `paper-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java`
- `paper-testing/src/test/java/dev/marulabs/commandlib/CommandBehaviorTest.java`, `paper-testing/src/test/java/dev/marulabs/commandlib/CommandContextTest.java`

**使用例**

```java
require(Command.playerRequirer()).execute((player, ctx) -> player.setHealth(20));

execute(ctx -> {
    Entity entity = ctx.getEntity();      // /execute as の entity。console や command block では null
    Location at = ctx.getLocation();      // 位置と yaw / pitch
    ctx.sendSuccess((entity == null ? "nobody" : entity.getName()) + " at " + at.getBlockX());
});
```

### 修正: context と prerequisite の細部 (ee4cd09e, 189ca4ce)

- ee4cd09e: Forge / NeoForge の `CommandContext#getLocation` で yaw / pitch が 0 のままだった。source の rotation を入れるように直し、`/execute rotated` やプレイヤーの向きを読めるようにした。`modded/src/main/java/dev/marulabs/commandlib/CommandContext.java`
- 189ca4ce: `CommandPrerequisiteException` の既定メッセージ "You don't have permission ..." を、runner の permission check と同じ "You do not have permission to execute this command." に揃えた。`common/src/main/java/dev/marulabs/commandlib/exception/CommandPrerequisiteException.java`, `common/src/test/java/dev/marulabs/commandlib/CommonCommandRequireTest.java`

### Vanilla integration: `/execute`、command block、function tag、`#load` (664b77f7, c60d2345, ccc14672)

**変更内容**

- 664b77f7（tests）: Spigot / Paper の integration fixture に `VanillaExecutionTest` を追加した。確認項目は次のとおり。`/execute as` の後も sender は console のままで entity は player になる。`positioned ... rotated` が `getLocation` に反映される。`@s` や distance selector が executor 基準で解決される。command block の sender は `BlockCommandSender` で、`SuccessCount` は result 42 のとき 1、失敗のとき 0 になる。`DataPackFunctionTest` には function tag と `#minecraft:load` が reload 後にちょうど 1 回動くことの確認を追加した。Forge / NeoForge の `ActorTest` では actor / entity が `/execute as` に従うことと、位置・rotation を確認した。
- c60d2345（修正）: reload 時、server 自身のコンパイルは CommandLib の command を知らないため、`Couldn't load tag ... missing following references` を出す。CommandLib の再コンパイルで直る tag エラーなので、`FunctionLoadErrorFilter` で function エラーと同じように抑制するようにした。他の tag のエラーと CommandLib 自身のコンパイルのエラーは引き続きログに出る。
- ccc14672（修正、Spigot）: plugin task から `/minecraft:reload` や `Bukkit#reloadData` を実行すると、server が同じ tick に自前のコンパイル（tag 欠落版）で `#minecraft:load` を消費してしまい、load function が一度も動かなかった。`FunctionCompilationMarker#reloaded` で resource の変更を検出し、server の library に load tag が欠けていた場合は、次 tick の再コンパイルで 1 回だけ実行するようにした。全 typed NMS module の `NMSDedicatedServer` と codegen も更新した。
- `docs/vanilla-integration.md`（9a72281c）に `/execute` 各 subcommand と context の対応、command block、function / tag のコンパイル時期、platform 差分表をまとめた。2026-10-16 に計 32 target で全ケースが通った。
- 未決（roadmap `[?]`）: `/execute as` 後の actor が platform で異なる。Spigot / Paper では `getSender()` / `getActor()` は元の sender（console）のままで、実行 entity は `getEntity()` で読む。Forge / NeoForge では actor が実行 entity（`PLAYER`）に従い、permission 判定もそれに従う。揃えるかどうかはまだ決めていない。

**主要ファイル**

- `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/VanillaExecutionTest.java`, `integration-test/shared/spigot/src/main/java/dev/marulabs/testplugin/DataPackFunctionTest.java`（paper も同様）, `integration-test/shared/modded/src/main/java/dev/marulabs/testmod/ActorTest.java`
- `common/src/main/java/dev/marulabs/commandlib/FunctionLoadErrorFilter.java`, `common/src/test/java/dev/marulabs/commandlib/FunctionLoadErrorFilterTest.java`
- `common/src/main/java/dev/marulabs/commandlib/FunctionCompilationMarker.java`, `common/src/test/java/dev/marulabs/commandlib/FunctionCompilationMarkerTest.java`, `spigot/src/main/java/dev/marulabs/commandlib/util/nms/server/NMSDedicatedServer.java`, `spigot/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `tools/nms-codegen/mojang.py`, `tools/nms-codegen/spigot_legacy.py`
- `docs/vanilla-integration.md`, `docs/design/command-registration-audit.md`, `docs/roadmap.md`

## 3. Message・help・documentation

### Captions とローカライズ (1458ede5, 03c2c57e, 594eda87, 84d5337e)

**変更内容**

- **破壊的変更**: CommandLib が送る組み込みメッセージ (権限エラー、option エラー、cooldown / confirmation、help の見出し、duration 表記など) を、安定したキーと英語デフォルトを持つ `Caption` にした。送信者の `ctx.getLocale()` で解決される。英語の文言の一部を変更 (`is not a valid namespaced key`、`is neither a known player nor a valid UUID`、`is not a valid selector`)。registry 系 argument にはそれぞれ専用の caption を用意した。
- vanilla に同じ意味のメッセージがある場合は caption を使わず translatable で送る (`argument.entity.notfound.player`、`advancement.advancementNotFound`、`recipe.notFound`、`arguments.objective.notFound`)。`Caption.UNEXPECTED_ERROR` は、テキストを設定しなければ vanilla の `command.failed` になる。
- `Captions` は locale ごとのテキストを保持する。英語・日本語の同梱 bundle を持ち、`set` / `load(ResourceBundle)` / `bundle(baseName, classLoader)` (UTF-8 の properties) で上書き・追加できる。検索は `ja_JP` → `ja` → root の順。`CaptionSender` を差し替えると、送信の方法を一括で変えられる。
- `captions(...)` は argument branch と子コマンドに継承される。`CommandBuilder#captions` も追加 (03c2c57e)。
- context に `sendMessage` / `sendSuccess` / `sendWarn` / `sendFailure(caption[, placeholders])` と `caption(...)` を追加し、各例外にも caption を受け取るコンストラクタを追加した。
- `RegistryArgument`、Spigot の `ScoreboardDisplaySlotArgument`、1.19.4 未満の `TimeArgument` の最小値チェックを、それぞれ caption `UNKNOWN_REGISTRY_ENTRY` / `UNKNOWN_DISPLAY_SLOT` / `TICK_COUNT_TOO_LOW` にした (594eda87)。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/Caption.java`, `Captions.java`, `CaptionSender.java`
- `common/src/main/java/dev/marulabs/commandlib/CommonCommandContext.java`, `CommonCommand.java`, `CommandBuilder.java`
- `common/src/main/java/dev/marulabs/commandlib/exception/*.java`
- `common/src/main/resources/dev/marulabs/commandlib/lang/commandlib_en.properties`, `commandlib_ja.properties`
- `common/src/test/java/dev/marulabs/commandlib/CaptionsTest.java`
- `docs/localization.md`

**使用例**

```java
public static final Caption HOME_SET = Caption.of("myplugin.home.set", "Home {name} set.");

Captions captions = Captions.create()
        .set(Caption.PERMISSION_DENIED, "You can't do that.")
        .set(Locale.JAPANESE, Caption.PERMISSION_DENIED, "それはできません。")
        .bundle("lang/messages", getClass().getClassLoader()); // lang/messages_ja.properties など

Command home = Command.builder("home")
        .captions(captions)
        .addChild(Command.builder("set")
                .argument(new StringArgument("name"), branch -> branch.execute((name, ctx) ->
                        ctx.sendSuccess(HOME_SET, Map.of("name", name)))))
        .build();
```

### Forge / Fabric でのプレイヤー言語取得 (26cefce1, 1fc68806)

**変更内容**

- Forge / NeoForge では、リフレクションによる取得 (本番 Forge では SRG 名になる) をやめ、`ServerPlayer#getLanguage` で読むようにした (26cefce1)。
- `getLanguage` は Forge / NeoForge が追加するメソッドなので、Fabric ノードがコンパイルできなかった。修正後、Fabric 1.21.1 は vanilla の client information から読む。Fabric 1.20.1 には公開 getter がないため、デフォルト言語のままとした (1fc68806)。

**主要ファイル**

- `modded/src/main/java/dev/marulabs/commandlib/CommandContext.java`
- `modded-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java`
- `modded-testing/src/test/java/dev/marulabs/commandlib/CaptionLocaleTest.java`

### Bukkit fixture plugin の caption bundle 欠落修正 (e8b42e13)

integration-test の fixture は platform module の resources しか取り込んでいなかった。そのため caption bundle が jar に入らず、全サーバーで `Caption` の初期化に失敗し、失敗メッセージと help が `NoClassDefFoundError` になっていた。`common/src/main/resources` も取り込むようにした。

- `integration-test/shared/integration-test.gradle.kts`

### Argument エラーの送信修正 (0926dd52, 8ad3be8d, a3f8ae9b, 253f85a7)

**変更内容**

- modded: argument の resolve 中に投げられた `CommandSyntaxException` が `sendSuccess` で送られていたため、赤字にならず、success feedback を抑止する環境では消えていた。failure として送るよう修正した。カスタム argument type の plain な Brigadier メッセージで起きていた `Component` へのキャスト失敗も修正した (0926dd52)。
- 全 platform: resolve エラーの例外が input と cursor を持つ場合、vanilla と同じく、メッセージの下に `...<input><--[HERE]` の context line を出すようにした (8ad3be8d)。
- **破壊的変更** (Paper): resolve エラーを英語の `getMessage()` ではなく、vanilla の translatable component (`MessageComponentSerializer` 経由) で送るようにした。4 つの selector argument (`PlayerArgument` / `PlayersArgument` / `EntityArgument` / `EntitiesArgument`) は、空の結果を `argument.entity.notfound.*` で拒否する。これまで `PlayersArgument` / `EntitiesArgument` は空リストを返していた (a3f8ae9b)。
- 無効な `@Default` のエラー文言を `which is not a valid <Type>` に統一し、どの型でも意味が通るようにした (253f85a7)。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/exception/ArgumentParseException.java`, `CommonArgument.java`, `VariadicArgument.java`
- `modded/src/main/java/dev/marulabs/commandlib/PlatformAdapterImpl.java`, `spigot/.../PlatformAdapterImpl.java`, `paper/.../PlatformAdapterImpl.java`
- `paper/src/main/java/dev/marulabs/commandlib/argument/SelectorResults.java`, `PlayersArgument.java`, `EntitiesArgument.java`, `PlayerArgument.java`, `EntityArgument.java`
- `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java`
- `docs/errors.md`, `docs/arguments.md`

### Help / usage / error formatter と `examples(...)` (ab600a49, 59baaa24, ed082499)

**変更内容**

- `HelpFormatter` は型付きの `CommandHelp` を受け取って help 全体を置き換える。`CommandHelp` は path、description、権限でフィルタ済みの usage と token、options、examples を持つ。`UsageFormatter` は usage 行ごとの整形、`ErrorFormatter` は CommandLib が報告する各失敗 (`CommandError`: `kind()`、`exception()`、`usage()`、`sendDefault()`) を扱う。どれも command 単位で設定し、captions と同じように継承される。デフォルトは従来の help をそのまま再現する。
- `examples(...)` で入力例を help の `Examples:` に出す (先頭に `/` がなければ補う)。
- `ErrorFormatter.SYNTAX_ADVICE` は、`ARGUMENT` / `VALIDATION` エラーのあとに、失敗した argument branch の `Usage:` 行を追加する。現在は opt-in。既定にすると全 module の argument error 出力が変わるため、既定にするかは roadmap に未決 (`[?]`) として残している。
- 修正: child command にしか続かない argument branch (`/team <team>`) を usage から外した。入力がそこで終われないためである (59baaa24)。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/HelpFormatter.java`, `UsageFormatter.java`, `ErrorFormatter.java`, `CommandHelp.java`, `CommandError.java`, `DefaultHelpFormatter.java`, `HelpMessageAction.java`, `CommandRunner.java`
- `common/src/test/java/dev/marulabs/commandlib/HelpErrorFormatterTest.java`、`common/src/test/resources/snapshots/command-help-*.snap`
- `docs/help.md`

**使用例**

```java
public final class ArenaCommand extends Command {
    public ArenaCommand() {
        super("arena");
        examples("/arena create lobby 16", "/arena lobby join");
        helpFormatter((ctx, help) -> {
            ctx.sendMessage(RichText.text(help.description()).color(0xFFAA00));
            for (CommandHelp.Usage usage : help.usages()) {
                ctx.sendMessage(RichText.text(usage.plainText())
                                        .click(ClickAction.suggestCommand(usage.plainText())));
            }
        });
        errorFormatter(ErrorFormatter.SYNTAX_ADVICE); // opt-in
        // ...
    }
}
```

### CommandDocs の Markdown / JSON export (dd4fc90a, 774645dd, 7cf4eebb)

**変更内容**

- `CommandDocs.of(prefix, commands...)` を追加した。`markdown()`、`json()`、`permissionsMarkdown()`、`permissionsJson()`、`tree()` を出力できる。command ごとに usage、alias、argument (optional / variadic の印)、option (alias、値、required、repeatable)、permission を出す。permission export は、登録時に生成される全 node を default と route 付きで列挙する。sender に依存する description (`description(ctx -> ...)`) は出力しない。
- command tree の走査を `CommandTree` に一本化し、testing の snapshot renderer もこれを使うようにした。
- export に `examples` も含めるようにした (774645dd)。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommandDocs.java`, `CommandTree.java`, `CommandTreeRenderer.java`, `Json.java`
- `common/src/test/java/dev/marulabs/commandlib/CommandDocsTest.java`、`common/src/test/resources/snapshots/command-docs*.snap`
- `docs/diagnostics.md`

**使用例**

```java
// 登録せずにビルドスクリプトやテストからも使える
CommandDocs docs = CommandDocs.of("myplugin.command", new SpawnCommand(), new ConfigCommand());
// 登録済みなら registration.docs() でも同じものが得られる
Files.writeString(Path.of("commands.md"), docs.markdown());
Files.writeString(Path.of("commands.json"), docs.json());
Files.writeString(Path.of("permissions.md"), docs.permissionsMarkdown());
```

### Runtime diagnostics と opt-in の diagnostics command (3aa4dc07, 2d0380b0, cc9869ae)

**変更内容**

- Spigot / Paper / Forge / NeoForge / Fabric / Velocity に `CommandLib.diagnostics()` を追加した。platform 情報 (サーバー、Minecraft、Java の version、dispatcher への登録経路、Spigot ではそのサーバーで使える typed NMS module) と、この CommandLib コピーの全 registration (owner、path、labels、command tree、permission node) を返す。
- `CommandLibDiagnosticsCommand` はレポートを sender に送る command である。CommandLib は自動登録しない。サブコマンドは `commands` / `permissions`。
- 各 registration に `docs()` を追加した。Forge / NeoForge の `register()` は registration を返すようにした。Velocity にも同じものを追加した (2d0380b0)。modded-testing では全ノードで diagnostics command を実行するテストを追加した (cc9869ae)。

**主要ファイル**

- `common/src/main/java/dev/marulabs/commandlib/CommandLibDiagnostics.java`
- `{spigot,paper,modded,velocity}/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `CommandLibDiagnosticsCommand.java`
- `spigot/src/main/java/dev/marulabs/commandlib/util/nms/NMSClassRegistry.java`
- `modded-testing/src/test/java/dev/marulabs/commandlib/CommandLibDiagnosticsCommandTest.java`
- `docs/diagnostics.md`

**使用例**

```java
// Spigot / Paper: 自分で選んだ名前で登録する
CommandLib.register(this, new SpawnCommand(), new CommandLibDiagnosticsCommand("myplugin-diagnostics"));
// Forge / NeoForge / Fabric
CommandLib.register("mymod.command", new SpawnCommand(), new CommandLibDiagnosticsCommand("mymod-diagnostics"));

// コードから (main thread で)
getLogger().info(CommandLib.diagnostics().report());
```

### ユーザー向けドキュメント (dc564766, f4b35e54, f7b5e27c, 758c0d85, 0275bba6, 3832e2f1, 01197beb, 9a72281c, 6fc802e9, e86693b7)

- 仕様: `docs/errors.md`、`docs/suggestions.md`、`docs/permissions.md`、`docs/testing.md`、`docs/troubleshooting.md` (dc564766)
- 機能ガイド: `docs/localization.md` (84d5337e)、`docs/help.md` (ed082499)、`docs/diagnostics.md` (7cf4eebb)、`docs/vanilla-integration.md` (`/execute`、command block、data pack function での挙動。9a72281c)
- 移行ガイド: `docs/migration/from-commandapi.md` (f4b35e54)、`from-cloud.md` / `from-lamp.md` / `from-acf.md` (f7b5e27c)。confirmation / cooldown の対応表を追加 (758c0d85)。
- `docs/arguments.md`: argument カタログ (CommandAPI / Paper との比較。3832e2f1)。旧 `docs/design/argument-catalog.md` は削除した (01197beb)。
- 比較: `docs/comparison.md` (0275bba6 で DI と context values の比較を追加)
- パッケージング: `docs/packaging.md`。全 platform で shade + relocate を推奨する。Jar-in-Jar / `include()` は最新のネストしたコピーしか読み込まず、0.x では minor でも binary 互換が壊れうるためである。README と `samples/README.md` も更新した (6fc802e9)。
- サイト: `mkdocs.yml` (MkDocs Material。Getting started / Guides / Reference / Migration / Platform notes / Design の構成)、`tools/docs-site/hooks.py` (GitHub 上の Markdown のリンクをサイト用に解決する)、`tools/docs-site/requirements.txt`、`docs/javadoc.md` (e86693b7)

### 比較用 example suite (98db08cb, 4f86ecc9)

- `examples` module: 7 つのユースケース (admin options、optional / variadic、argument-first tree、async I/O、DI、locale ごとのメッセージ、vanilla result) を、fluent API / Java builder / annotation / Kotlin DSL の 4 方式で実装した。4 方式の tree、help、結果が一致することを `CommandTester` と snapshot で検証する (98db08cb)。
- `docs/comparison.md`: 各ユースケースを CommandAPI / Cloud / Lamp / ACF / Paper と並べ、検証済みかどうかを明記した。移行ガイドからは、captions / middleware / locale を「未実装」とする記述を削除した (4f86ecc9)。
- 主要ファイル: `examples/build.gradle.kts`、`examples/src/main/java/dev/marulabs/commandlib/examples/**`、`examples/src/main/kotlin/**`、`docs/comparison.md`

## 4. Annotation API と Kotlin DSL

どちらも fluent API の `Command` を組み立てる薄い層で、help・permission node・option・suggestion・テストは fluent で書いた同じコマンドと一致する。テストでも、同じコマンドを両方の書き方で作り、ツリー・help・実行結果が一致することを確認している。

依存する artifact（jitpack: `com.github.Maru32768.CommandLib:<artifact>:<version>`）:

| 用途 | artifact |
|---|---|
| Annotation API | 各プラットフォーム artifact に同梱（`spigot`、`paper`、`velocity`、`forge-1.20.1`、`neoforge-1.21.1` など）。コア部分は `common` |
| Kotlin DSL | `spigot-kotlin`、`paper-kotlin`、`velocity-kotlin`（それぞれ platform artifact と `common-kotlin` を引き込む）。coroutine 関数を使う場合は `org.jetbrains.kotlinx:kotlinx-coroutines-core` も追加する |

### 4.1 `AnnotationParser` と基本アノテーション (4eb6b3dc)

**変更内容**
- `@Command` を付けたクラスを `AnnotationParser#parse` で通常の `Command` に変換する。`@Execute` / `@Subcommand` のメソッドが executor と argument branch になり、パラメータの種類によって context、context value、option（`@Flag`、`@Option`）、型ごとに登録した factory で作る argument のいずれかに割り当てられる。
- 引数まわり: `@Optional`、`@Default`、`@Range`、`@Suggest`、`@Greedy`、`@Name`、`List<T>` を受ける variadic。コマンドまわり: `@Permission`、`@Description`、`@Confirm`、`@Cooldown`。戻り値は `void`、`int`（`/execute store` 向け）、`CompletionStage`（`executeAsync` として実行）を受け付ける。
- 独自のパラメータアノテーションは `registerAnnotation(annotation, valueType, handler)` で追加できる。
- ACF 風の書き方を求めるユーザー向けの入口として追加した。fluent API を内部で使うので挙動の差は出ない。

**主要ファイル**
- `common/src/main/java/dev/marulabs/commandlib/annotation/AnnotationParser.java`
- `common/src/main/java/dev/marulabs/commandlib/annotation/{Command,Execute,Subcommand,Flag,Option,Default,Optional,Range,Suggest,Greedy,Name,Sender,Permission,Description,Confirm,Cooldown}.java`
- `common/src/main/java/dev/marulabs/commandlib/annotation/{AnnotatedParameter,ArgumentFactory,ArgumentAnnotationHandler}.java`
- `common/src/test/java/dev/marulabs/commandlib/AnnotationParserTest.java`
- `docs/annotations.md`、`docs/design/annotation-api.md`

**使用例**
```java
@Command(value = "arena", aliases = "ar")
@Description("Manage arenas")
public final class ArenaCommand {
    @Subcommand("create")
    @Permission(defaultPermission = DefaultPermission.ALL, description = "Create arenas")
    public void create(CommandContext ctx,
                       @Flag(value = "force", shortName = 'f') boolean force,
                       String name,
                       @Range(min = 1, max = 64) @Default("16") int size) {
        ctx.sendSuccess("Created " + name + " with " + size + " slots");
    }

    @Subcommand("list")
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    public void list(CommandContext ctx,
                     @Option(value = "limit", shortName = 'n') @Range(min = 1, max = 100) @Default("10") int limit) {
    }
}
```
引数名はパラメータ名から取るため、`-parameters` 付きでコンパイルするか `@Name` を付ける。

### 4.2 プラットフォーム別 `AnnotatedCommands` (9413279b, 7cb8c037)

**変更内容**
- Spigot / Paper / Velocity / modded（Forge・NeoForge・Fabric）向けに `AnnotatedCommands.parse(instance)` と `AnnotatedCommands.parser()` を追加した。組み込み argument 型（`Player`、`World`、`RegisteredServer`、`ServerPlayer`、`BlockPos` など）と sender 型（`CommandSender`、`CommandSource`、`CommandSourceStack`、`ServerPlayer` など）の解決があらかじめ登録されている。
- modded では、1.20.5+ の `Holder<Enchantment>` / `Holder<MobEffect>` / `Holder<Attribute>` を型引数から判別して対応する argument を選ぶ。あわせて modded の `Command` に、他プラットフォームと同じ `playerRequirer()`、`consoleRequirer()`、`requirePlayer()`、`requireConsole()` を追加した。

**主要ファイル**
- `spigot/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java`
- `paper/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java`
- `velocity/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java`
- `modded/src/main/java/dev/marulabs/commandlib/annotation/AnnotatedCommands.java`、`modded/src/main/java/dev/marulabs/commandlib/Command.java`
- `modded-testing/src/test/java/dev/marulabs/commandlib/{AnnotatedArenaCommand,FluentArenaCommand,AnnotationStyleTest}.java`

**使用例**
```java
// Spigot / Paper (onEnable)
CommandLib.register(this, AnnotatedCommands.parse(new ArenaCommand()));
// Velocity
CommandLib.register(this, proxy, AnnotatedCommands.parse(new ArenaCommand()));
// Forge / NeoForge / Fabric
CommandLib.register("mymod.command", AnnotatedCommands.parse(new ArenaCommand()));
```

### 4.3 fluent との等価性テスト (5bf6fe66)

**変更内容**
- 同じ arena コマンドを annotation と fluent の両方で書き、ツリー・permission・suggestion・help・実行の記録が Spigot / Paper ごとに同じ 1 つのスナップショットに一致することを `AnnotationStyleTest` で検証する。後に modded（7cb8c037）と guild コマンド（ef33c423）も同じ方式で追加した。

**主要ファイル**
- `spigot-testing/src/test/java/dev/marulabs/commandlib/{AnnotatedArenaCommand,FluentArenaCommand,AnnotationStyleTest}.java`
- `paper-testing/src/test/java/dev/marulabs/commandlib/{AnnotatedArenaCommand,FluentArenaCommand,AnnotationStyleTest}.java`
- `spigot-testing/src/test/resources/snapshots/annotation-arena.snap`、`paper-testing/src/test/resources/snapshots/annotation-arena.snap`

### 4.4 context value と `@Dependency` (ae3fce04)

**変更内容**
- `registerContextValue(type, extractor)` は extractor を parse 後のコマンドの provider として登録し、パラメータを `ctx.get(type)` で解決する。そのため、メソッド・後から足した fluent の子コマンド・middleware が 1 回の実行で同じ値を共有する。
- `registerContextValue(type)` は、middleware が `put` する値や親コマンドが provide する値を宣言するだけのもの。
- `registerDependency(type, instance)` は `@Dependency` フィールドに instance を注入し、同時に provide もする。

**主要ファイル**
- `common/src/main/java/dev/marulabs/commandlib/annotation/{AnnotationParser,Dependency}.java`
- `common/src/test/java/dev/marulabs/commandlib/AnnotationParserTest.java`
- `docs/context-values.md`（「Annotated commands」節）

**使用例**
```java
@Command("team")
public final class TeamCommand {
    @Dependency
    private Games games;

    @Subcommand("join")
    public void join(GamePlayer player, String team) { games.join(player, team); }

    @Subcommand("stats")
    public void stats(CommandContext ctx, Account account) { ctx.sendMessage("Balance " + account.balance()); }
}

Command team = AnnotatedCommands.parser()
        .registerDependency(Games.class, games)
        .registerContextValue(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)))
        .registerContextValue(Account.class)    // a middleware puts it
        .parse(new TeamCommand());
```

### 4.5 `@Condition` (817308c3)

**変更内容**
- `registerCondition(name, prerequisite)` で名前を付けた `Prerequisite` を、クラスまたは `@Subcommand` メソッドの `@Condition({...})` から参照できるようにした（ACF の conditions に相当）。登録されていない名前を指定すると parse 時にエラーになる。fluent 側は `Prerequisite` 定数を `addPrerequisite` に渡す方法を `docs/errors.md` に記載した。

**主要ファイル**
- `common/src/main/java/dev/marulabs/commandlib/annotation/Condition.java`
- `docs/errors.md`（Reusable conditions）、`docs/migration/from-acf.md`

**使用例**
```java
@Subcommand("start")
@Condition("daytime")
public void start(CommandContext ctx) { }

AnnotatedCommands.parser().registerCondition("daytime", Conditions.DAYTIME).parse(new ArenaCommand());
```

### 4.6 placeholder パスと残りの fluent 機能 (ef33c423, 5137c9d8)

**変更内容**
- `@Subcommand("<team> add")`: placeholder は「argument の後に子コマンドが続く」ことを表し、`argument(team).child(team -> new Command("add") ...)` と同じ構造になる。隣り合う placeholder は 1 つの branch にまとまる。placeholder で終わるパスは、その branch の executor になる。
- 名前で参照するもの: `@Middleware({...})` → `registerMiddleware`、`@VisibleWhen(name)` → `registerVisibility`。
- option の拡張: 登録済みの任意の argument 型を値に取れる（例: `@Option("world") World world`）。さらに option 単位の `@Permission`（`value` か `defaultPermission` のどちらか）、`@Requires(value, equalTo)`（repeatable）、クラス／メソッドに付ける `@MutuallyExclusive`（repeatable）を追加した。
- その他: `@OptionsAnywhere`、`@Examples`、`@CaptionBundle`、`@Description(caption = ...)`（ロケール別の説明）、`@Variadic(max)`。
- 5137c9d8 で `docs/annotations.md` と `docs/design/annotation-api.md` を更新し、examples の team / home コマンドを新しいアノテーションだけで書き直した。これにより parse 後に fluent コードを足す必要がなくなった。`docs/comparison.md` の行数比較も合わせて更新した。

**主要ファイル**
- `common/src/main/java/dev/marulabs/commandlib/annotation/{Middleware,VisibleWhen,OptionsAnywhere,Requires,MutuallyExclusive,Examples,CaptionBundle,Variadic}.java`
- `spigot-testing/src/test/java/dev/marulabs/commandlib/{AnnotatedGuildCommand,FluentGuildCommand}.java`（paper-testing にも同じものがある）、`*/src/test/resources/snapshots/annotation-guild.snap`
- `examples/src/main/java/dev/marulabs/commandlib/examples/team/AnnotatedTeamCommand.java`、`examples/src/main/java/dev/marulabs/commandlib/examples/home/AnnotatedHomeCommand.java`
- `docs/annotations.md`、`docs/design/annotation-api.md`、`docs/comparison.md`、`docs/migration/from-cloud.md`

**使用例**（`spigot-testing` の `AnnotatedGuildCommand` を短縮したもの）
```java
@Command("guild")
@Description(value = "Manage guilds", caption = "test.guild.description")
@Examples({"/guild red invite Steve", "/guild red kick -s Steve"})
@Middleware("audit")
public final class AnnotatedGuildCommand {
    @Subcommand("<guild> invite")
    public void invite(CommandContext ctx, String guild, @Variadic(max = 3) List<Player> players) { }

    @Subcommand("<guild> kick")
    @OptionsAnywhere
    @MutuallyExclusive({"silent", "loud"})
    @VisibleWhen("players")
    public void kick(CommandContext ctx,
                     @Flag(value = "silent", shortName = 's') boolean silent,
                     @Flag(value = "loud", shortName = 'l') @Permission(defaultPermission = DefaultPermission.ALL) boolean loud,
                     @Option(value = "reason", shortName = 'r') @Requires("by") String reason,
                     @Option(value = "by", shortName = 'b') @Optional Player by,
                     String guild, Player target) { }
}

AnnotatedCommands.parser()
        .registerMiddleware("audit", FluentGuildCommand.AUDIT)    // a CommandMiddleware<CommandContext>
        .registerVisibility("players", ctx -> ctx.getSender() instanceof Player)
        .parse(new AnnotatedGuildCommand());
```

### 4.7 Kotlin DSL モジュール (3008dd28, bf7888bd)

**変更内容**
- `common-kotlin` はプラットフォームに依存しない部分を持つ。内容は、型付きの `ArgRef` を受け取る `argument(...) { a, b -> }` ブロック（1〜7 引数）、delegated property として読める option（`val force by flag("force", 'f')`）、`ctx[ref]` / `ctx[option]`、coroutine 関数（`executeSuspend`、`addSuggestionProviderSuspend`、`ctx.syncDispatcher` / `ctx.asyncDispatcher`）。ブロックには `@DslMarker` が付いている。
- `spigot-kotlin`、`paper-kotlin`、`velocity-kotlin` は、`common-kotlin/src/platform/kotlin/.../Commands.kt` の `command()` / `subcommand()` を source dir として取り込み、各プラットフォームの `Command` に対してコンパイルする。テスト（`src/platformTest`）も同じ仕組みで共有している。
- `kotlinx-coroutines` は `compileOnly`。coroutine 関数を呼ばないプラグインは実行時に不要。`common-kotlin` は package-private のクラスで現在のコマンド context を読むため、`common` と同じ class loader に載せる（同じ relocation でまとめて shade する）必要がある。
- bf7888bd で、Spigot 上で Bukkit の型を DSL に通すテストを追加した。

**主要ファイル**
- `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/{Dsl,Coroutines}.kt`、`common-kotlin/src/main/kotlin/dev/marulabs/commandlib/CurrentCommandContexts.kt`
- `common-kotlin/src/platform/kotlin/dev/marulabs/commandlib/kotlin/Commands.kt`
- `common-kotlin/src/platformTest/kotlin/dev/marulabs/commandlib/kotlin/KotlinDslTest.kt`、`spigot-kotlin/src/test/kotlin/dev/marulabs/commandlib/kotlin/BukkitDslTest.kt`
- `{common,spigot,paper,velocity}-kotlin/build.gradle.kts`、`build.gradle.kts`（Kotlin plugin と ktlint の設定）、`settings.gradle.kts`
- `docs/kotlin.md`

**使用例**（依存の追加）
```kotlin
dependencies {
    implementation("com.github.Maru32768.CommandLib:spigot-kotlin:latest.release")
    // Only for executeSuspend, addMiddlewareSuspend, addSuggestionProviderSuspend and the dispatchers.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
```

### 4.8 reified context value (01077436)

**変更内容**
- `provide { ctx -> ... }` は lambda の戻り値の型をキーにして値を provide する。`ctx.get<T>()`、`ctx.getOrNull<T>()`、`ctx.put(value)` は reified 型で読み書きするので、`Class` を渡したりキャストしたりする必要がない。

**主要ファイル**
- `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/ContextValues.kt`
- `common-kotlin/src/platformTest/kotlin/dev/marulabs/commandlib/kotlin/KotlinDslTest.kt`

**使用例**
```kotlin
command("team") {
    provide { ctx -> games.of(ctx.player) }    // GamePlayer
    subcommand("show") { execute { ctx -> ctx.sendMessage(ctx.get<GamePlayer>().team) } }
}
```

### 4.9 `arguments(...)`、`require {}`、`addMiddlewareSuspend` (3554fece, cac78665)

**変更内容**
- `arguments(arg1, ..., argN) { }`: 引数の数に上限がない branch（8 個以上）。ref は渡されないので、値は `ctx[arg]` で読む。
- `require(extractor) { }`: extractor が値を返したときだけ実行される branch。中の `execute { value, ctx -> }` / `executeSuspend { value, ctx -> }` が値を受け取る。`argument(...)` ブロックの中でも使える。
- `addMiddlewareSuspend { ctx, next -> }`: suspend 版の middleware。sync thread で始まり、suspend から戻るときも sync thread で再開する。`next()` は残りの middleware と executor が終わるまで待ち、そこで起きた例外はそのまま投げ直す。`next()` を呼ばなければ executor をスキップする。

**主要ファイル**
- `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/kotlin/{Dsl,Coroutines}.kt`、`common-kotlin/src/platform/kotlin/dev/marulabs/commandlib/kotlin/Commands.kt`
- `common-kotlin/src/platformTest/kotlin/dev/marulabs/commandlib/kotlin/KotlinDslTest.kt`
- `docs/kotlin.md`

**使用例**（argument・option・suspend middleware・suspend executor を使うコマンド）
```kotlin
CommandLib.register(this, command("heal") {
    permission(PermissionDefault.OP)
    val silent by flag("silent", 's', "Do not tell the players")
    addMiddlewareSuspend { ctx, next ->
        val banned = withContext(ctx.asyncDispatcher) { bans.contains(ctx.actor.name) }
        if (banned) throw CommandPrerequisiteException("You are banned.")
        next()
    }
    argument(IntegerArgument("amount"), PlayerArgument("targets").variadic()) { amount, targets ->
        execute { ctx ->
            targets.get().forEach { it.health = minOf(it.health + amount.get(), 20.0) }
            if (!silent) ctx.sendSuccess("Healed ${targets.get().size} players")
        }
    }
    require(Command.playerRequirer()) {
        subcommand("self") { execute { ctx -> ctx.player.health = 20.0 } }
    }
    subcommand("stats") {
        executeSuspend { ctx ->
            val healed = withContext(ctx.asyncDispatcher) { database.countHeals() }
            ctx.sendSuccess("$healed heals so far")
        }
    }
})
```

### 4.10 Kotlin サンプルプラグイン (a4715551)

**変更内容**
- `samples/paper-kotlin` を追加した。`paper-kotlin` artifact を使う Paper プラグインで、`/feed`（flag と variadic player）と `/notes add|list`（optional の page）を実装している。shadow では `dev.marulabs.commandlib` を 1 つの relocation でまとめる。このため platform artifact・`common`・`common-kotlin` が一緒に移動し、`common-kotlin` が同じ class loader に載る条件を満たす。同じコミットで Java の `samples/velocity` も追加した。

**主要ファイル**
- `samples/paper-kotlin/build.gradle.kts`、`samples/paper-kotlin/src/main/kotlin/dev/marulabs/samplekotlin/SamplePlugin.kt`、`samples/paper-kotlin/src/main/resources/plugin.yml`
- `samples/velocity/src/main/java/dev/marulabs/sampleproxy/{SampleProxyPlugin,SendCommand,FindCommand}.java`

**使用例**
```kotlin
private fun feedCommand() = command("feed") {
    description("Fill the hunger bars of players")
    val silent by flag("silent", 's', "Do not report the result")
    argument(PlayerArgument("targets").variadic()) { targets ->
        execute { ctx ->
            targets.get().forEach { it.foodLevel = 20 }
            if (!silent) ctx.sendSuccess("Fed ${targets.get().size} players")
        }
    }
}
```

## 5. Platform・testing・build

### 5.1 Velocity 対応（a11ef94c, 8898351a, 2d0380b0, 508adede）

**変更内容**

- `velocity` artifact を新設。Velocity 3.4+ の proxy に CommandLib の command を `BrigadierCommand` として登録する。tree は common の `CommandNodeCreator` が `CommandSource` 用の `PlatformAdapter` 経由で組み立てる。
- permission は Velocity の `PermissionSubject` を使う。proxy には permission registry も operator もないため、未設定の node には CommandLib の `DefaultPermission` を適用する。message は Adventure component。
- argument は Brigadier primitive・string・duration・literal・enum・object に加え、proxy 固有の `PlayerArgument`（online player）と `ServerArgument`（登録済み server）。どちらも string として parse・補完する。
- 2d0380b0 で他 platform と揃えて `docs()`・`diagnostics()`・`CommandLibDiagnosticsCommand` を追加。diagnostics は proxy の version と、`register` 呼び出しごとの `BrigadierCommand` 登録（plugin id が owner）を報告する。
- `velocity-testing` を新設。Mockito の `ProxyServer` に fake player / server を持たせた `CommandTester` と `FakeSender` を提供し、508adede で `CommandLibExtension` と `@Sender` を追加。
- 8898351a: Velocity `3.4.0`（Java 17）と `4.2.0`（Adventure 5、Java 25）の実 proxy で手動確認した結果を記録した。repository に proxy の integration target はない。

**主要ファイル**

- `velocity/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `VelocityCommandActor.java`, `VelocityPermissions.java`, `PlatformAdapterImpl.java`, `CommandLibDiagnosticsCommand.java`
- `velocity/src/main/java/dev/marulabs/commandlib/argument/PlayerArgument.java`, `ServerArgument.java`
- `velocity-testing/src/main/java/dev/marulabs/commandlib/CommandTester.java`, `FakeSender.java`, `CommandLibExtension.java`, `Sender.java`
- `docs/permissions.md`, `docs/arguments.md`, `docs/compatibility.md`, `docs/diagnostics.md`

**使用例**（`samples/velocity` より）

```java
@Plugin(id = "sampleproxy", name = "SampleProxy", version = "1.0.0")
public final class SampleProxyPlugin {
    private final ProxyServer proxy;

    @Inject
    public SampleProxyPlugin(ProxyServer proxy) { this.proxy = proxy; }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // permission prefix は sampleproxy.command
        CommandLib.register(this, proxy, new Command("send") {{
            argument(new PlayerArgument("player"), new ServerArgument("server")).execute((player, server, ctx) -> {
                player.createConnectionRequest(server).fireAndForget();
                ctx.sendSuccess("Sending " + player.getUsername());
            });
        }}, new CommandLibDiagnosticsCommand("sampleproxy-diagnostics"));
    }
}
```

### 5.2 Fabric と modded matrix の拡張（5197348d, 74db4532）

**変更内容**

- 5197348d: `modded` / `modded-testing` の Stonecutter tree に `fabric-1.20.1` と `fabric-1.21.1` node を追加。Forge / NeoForge と同じソースを新しい `fabric` 定数で分岐する。
  - command は Fabric API の `CommandRegistrationCallback`（v2）で登録する。permission は mod が `fabric-permissions-api` を同梱していればそれを使い、なければ node の `DefaultPermission` と vanilla の permission level に fallback する。
  - jar は `fabric.mod.json` を持ち、その entrypoint が server を追跡し始める。
  - Fabric にはグローバルな server accessor がないので、全 loader で `ServerLifecycleHooks` の直接呼び出しを `util.CurrentServer` に置き換えた（modded-testing ではこれを mock）。
  - Fabric には `registerByClass` がないため、`UnparsedArgument` は vanilla の class map に argument type を登録する。actor は private な raw source を reflection で読む。
- 74db4532: `forge-1.18.2`、`forge-1.19.2`、`neoforge-1.21.4`、`fabric-1.21.4` を追加し、node は計 9 になった（Forge 1.16.5 / 1.18.2 / 1.19.2 / 1.20.1、NeoForge 1.21.1 / 1.21.4、Fabric 1.20.1 / 1.21.1 / 1.21.4）。Forge / NeoForge の新 node には integration test mod と target も付く。共通ソースで吸収した差分は次の通り。
  - 1.19〜1.19.2: `CommandBuildContext` は final class で、受け取るのは item / block の argument だけ。static registry から作る。
  - Forge 1.18 以前: `CommandSourceStack.source` が private なので reflection で読む。
  - 1.21.2: `listRegistryKeys`、`CommandBuildContext#enabledFeatures`、Registry を返す `lookupOrThrow`、advancement / recipe の `ResourceKeyArgument` 化、`ServerPlayer` が `CommandSource` でなくなった点、attribute key から `generic.` prefix が消えた点。
  - modded-testing: feature flag は 1.19.3 から、ForgeHooks の entity selector check は 1.19 から、version 検出は 1.18 から有効にした。

**主要ファイル**

- `settings.gradle.kts`, `build.gradle.kts`, `gradle/modded-node.gradle.kts`
- `modded/versions/<node>/gradle.properties`, `modded-testing/versions/<node>/gradle.properties`
- `modded/src/main/java/dev/marulabs/commandlib/CommandLib.java`, `ForgeCommandActor.java`, `util/CurrentServer.java`
- `modded/src/main/java/dev/marulabs/commandlib/argument/BuiltInCommandBuildContext.java`, `RawWordArgumentType.java`, `RecipeArgument.java`, `AdvancementArgument.java`
- `modded/src/main/resources/fabric.mod.json`
- `integration-test/targets/forge-1.18.2/`, `forge-1.19.2/`, `neoforge-1.21.4/`
- `docs/design/forge-version-support.md`, `docs/agents/modded.md`, `docs/packaging.md`

**使用例**（新しい artifact、`docs/packaging.md` より）

```kotlin
dependencies {
    // Velocity
    implementation("com.github.Maru32768.CommandLib:velocity:<version>")
    testImplementation("com.github.Maru32768.CommandLib:velocity-testing:<version>")

    // Fabric（Loom。shadow + relocate してから remapJar する。samples/fabric を参照）
    modImplementation("com.github.Maru32768.CommandLib:fabric-1.21.1:<version>")
    testImplementation("com.github.Maru32768.CommandLib:fabric-testing-1.21.1:<version>")

    // 追加の Forge / NeoForge node
    implementation("com.github.Maru32768.CommandLib:forge-1.19.2:<version>")
    implementation("com.github.Maru32768.CommandLib:neoforge-1.21.4:<version>")
}
```

### 5.3 modded の修正（eca17e02, ee4cd09e）

- eca17e02: world を持たない `Location` の `clone()` が null の world を参照して例外になっていた。`modded/src/main/java/dev/marulabs/commandlib/util/Location.java`
- ee4cd09e: Forge / NeoForge の `CommandContext#getLocation` が yaw / pitch を 0 のままにしていたため、player や `/execute rotated` の向きが読めなかった。source の rotation を設定する（spigot と同じ挙動）。integration fixture で `/execute rotated` を確認している。`modded/src/main/java/dev/marulabs/commandlib/CommandContext.java`

### 5.4 Folia（35263cb9）

`paper` artifact が実行時に Folia を検出し（`RegionizedServer`）、`FoliaCommandScheduler` を使う。sync 側は source の entity / region / global region scheduler、async 側は async scheduler で動く。Folia は function を実行せず data reload もないため、`ServerFunctions` は再 compile しない。確認は `paper-testing` の `FoliaCommandSchedulerTest`（mock scheduler）までで、実 Folia server の target はない。API の詳細は別 section で扱う。

### 5.5 Testing tool: JUnit extension・`suggest()`・command tree snapshot（10c922b3, 6c0b8060, c475687a, badcf457, 735e644b）

**変更内容**

- 10c922b3: `common-testing` / `spigot-testing` / `paper-testing` に追加。
  - `CommandLibExtension`: test ごとに `CommandTester` を作って test 後に close し、`CommandTester` と `FakeSender` の parameter を test method と `@BeforeEach` / `@AfterEach` に注入する。lifecycle method とその test には同じ tester が渡る。
  - `@Sender`: 注入する sender を設定する。要素は `value`（名前）、`console`、`op`、`permissions`、`denyPermissions`。Spigot / Paper では注入した player が fake player として登録され、player / entity argument で名前解決できる。
  - `CommandTester#suggest(input, sender)`: 補完テキストを `List<String>` で返す。
  - `CommandTester#commandTree()` / `assertCommandTreeMatchesSnapshot(name)`: 登録済み command の alias・permission・option・argument branch を text に描画し、`src/test/resources/snapshots/<name>` と比較する。snapshot が無ければ書き出して fail する。`COMMANDLIB_UPDATE_SNAPSHOTS=true` で全 snapshot を書き換える。common の help snapshot test も同じ `Snapshots` を使うようにした。
- 6c0b8060: `modded-testing` の `CommandTester` に `withLevel(ServerLevel)`（`WorldArgument` 用）、`withServer(Consumer<MinecraftServer>)`（server mock の残りを stub）、`suggest`、`commandTree`、snapshot を追加。data-driven registry を 1.20.5 からではなく 1.19.3 から server に持たせ、1.20.1 でも biome を解決できるようにした。
- c475687a: `modded-testing` に `CommandLibExtension` と `@Sender` を追加。注入した player は `withPlayer` で登録される。
- badcf457: `docs/errors.md` と `docs/testing.md` が説明する error 挙動を、docs と同じ例で検証する `DocumentedErrorBehaviorTest` を `spigot-testing` に追加。
- 735e644b（fix）: `FakeSender.player` が `ServerPlayer` を mock するとき class 初期化が走る。Minecraft の bootstrap 前に呼ぶと失敗し、その JVM では以降ずっと `ServerPlayer` が使えなくなっていた。`FakeSender` 側で先に bootstrap する。`modded-testing/src/main/java/dev/marulabs/commandlib/FakeSender.java`, `CommandTester.java`

**主要ファイル**

- `{common,spigot,paper,velocity,modded}-testing/src/main/java/dev/marulabs/commandlib/CommandLibExtension.java`, `Sender.java`, `CommandTester.java`
- `common/src/main/java/dev/marulabs/commandlib/CommandTreeRenderer.java`, `Snapshots.java`
- `spigot-testing/src/test/java/dev/marulabs/commandlib/DocumentedErrorBehaviorTest.java`
- `docs/testing.md`

**使用例**（`velocity-testing/src/test/java/dev/marulabs/commandlib/CommandLibExtensionTest.java` を簡略化。他の testing artifact でも同じ形で書ける）

```java
class HealCommandTest {
    @RegisterExtension
    final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(HealCommand::new,
                                                                                           "proxy.command"));

    @Test
    void heals_and_suggests(CommandTester tester, @Sender("Steve") FakeSender steve, @Sender("Admin") FakeSender admin) {
        tester.execute("heal Steve 5", admin);
        assertThat(admin.getSentMessageTexts()).containsExactly("Healed Steve by 5");
        assertThat(tester.suggest("heal St", admin)).containsExactly("Steve");
    }

    @Test
    void guests_cannot_heal(CommandTester tester,
                            @Sender(value = "Guest", permissions = "proxy.command.heal") FakeSender guest) {
        assertThatThrownBy(() -> tester.execute("heal Guest 5", guest)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void command_tree_is_unchanged(CommandTester tester) {
        tester.assertCommandTreeMatchesSnapshot("heal.snap");
    }
}
```

### 5.6 Integration test の拡充（9818ea38, 664b77f7, bde20cbb, 907bc135）

**変更内容**

`integration-test/` は Testcontainers で Docker 上に実 server を起動し、MCProtocolLib の bot（`Maru32768`）を接続して fixture plugin / mod の test suite を server 内で実行する。結果は JUnit XML で回収する。今回 fixture に次の case を追加した。

- 9818ea38: block position、item predicate、uncentered location の parse と、`0 200 0` に置いた block に対する block predicate。Paper fixture の block predicate は `blockInWorldPredicate()` が API に入った 1.21.8 から実行する。
- 664b77f7: vanilla 経由の実行。
  - Spigot / Paper の `VanillaExecutionTest`: `/execute as` で sender が console のまま entity が player になること、`/execute positioned ... rotated` が `getLocation` に届くこと、`@s` / `distance` selector が executor から解決されること、2 つの command block が `BlockCommandSender` として実行し、`SuccessCount` が成功時 1・失敗時 0 になること。
  - `DataPackFunctionTest`: function tag と `#minecraft:load` が reload 後にちょうど 1 回実行されること。
  - Forge / NeoForge の `ActorTest`: actor / entity が `/execute as` に従うこと、`getLocation` が `positioned ... rotated` の位置と向きを持つこと。
  - 後の tick で走る check は `TestBase#afterReload` で reload の数 tick 後に dispatch する。test server では command block を有効にした。
- bde20cbb（Bukkit 系）: count / repeatable option、redirect 後に読む `PlayerArgument` の option、`optionsAnywhere`、alias と排他 option、option permission、`visibleWhen` と `CommandLib.refreshAll`（再送された tree を bot が decode する）、server scheduler 上の middleware 付き `executeAsync`、diagnostics command。bot 側では、permission の無い option が拒否されることと、caption が player の locale で引かれることを確認する。
- 907bc135（Forge / NeoForge）: flag / value / count / repeatable option、`PlayerArgument` の option、`optionsAnywhere`、`visibleWhen` + `refreshAll`、middleware 付き `executeAsync`、diagnostics。async の完了を待つため、report は 2 秒後に書く。

**主要ファイル**

- `integration-test/shared/spigot/.../testplugin/{VanillaExecutionTest,DataPackFunctionTest,OptionTest,RuntimeFeatureTest,ArgumentTest,TestBase,TestMain}.java`（`shared/paper` も同構成）
- `integration-test/shared/modded/src/main/java/dev/marulabs/testmod/{ActorTest,OptionTest,RuntimeFeatureTest,ArgumentTest,TestMain}.java`
- `integration-test/src/test/java/dev/marulabs/commandlib/integration/IntegrationTest.java`
- `integration-test/gradle/integration-test-target.gradle.kts`, `integration-test/README.md`, `docs/vanilla-integration.md`

**使用例**（Docker が必要。無い環境では skip ではなく失敗する）

```powershell
# 1 target
.\gradlew.bat :integration-test:targets:paper-1.20.4:minecraftIntegrationTest
# 設定済みの全 target（server test の既定並列数は 4）
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.minecraftIntegrationMaxParallel=1
# container あたりの CPU 上限（既定 2、0 で無効）
.\gradlew.bat :integration-test:minecraftIntegrationTest -Pcommandlib.containerCpuLimit=1.5
```

report は `integration-test/shared/test-results/` に出力される。

### 5.7 実 server 検証で見つかった不具合の修正（99a6aa7e, 0c3a262c, ccc14672, 5dc77d92, dd21e62e, e51a8297, ad060861）

- 99a6aa7e: 26.2 で `MinMaxBounds` が `advancements.criterion` から `advancements.predicates` に移った。26.2 / 26.3 でも動く `spigot-26.1.2` module の `NMSArgumentIntRange` / `NMSArgumentFloatRange` がリンクに失敗し、plugin の enable が `NMSClassNotFoundException` で止まっていた。bound を `Object` で受け取り、`min()` / `max()` を reflection で読むように変更。`nms/.../NMSArgument{Int,Float}Range_spigot_26_1_2.java`, `spigot/src/main/java/dev/marulabs/commandlib/util/nms/argument/NMSArgument.java`, `tools/nms-codegen/mojang.py`
- 0c3a262c: single-entity の `EntityArgument` に `@e[type=player,distance=..0.01]` を渡しており、vanilla が実行前に拒否していた。selector に `limit=1` を付けた。`integration-test/shared/{spigot,paper}/.../VanillaExecutionTest.java`, `docs/vanilla-integration.md`
- ccc14672（ライブラリ修正）: plugin の task から `/minecraft:reload` や `Bukkit#reloadData` を実行すると、server は CommandLib command を含む `#minecraft:load` tag を落とした自前の compile でその tick の load を実行し、load function が一度も走らなかった（全 Spigot target で `functionLoadCount` が失敗）。`FunctionCompilationMarker#reloaded` で data pack の変更有無を渡し、server の library に `#minecraft:load` が無ければ `replaceLibrary` が予約した実行を残す。これで 1 tick 遅れて 1 回だけ実行される。`common/.../FunctionCompilationMarker.java`, 各 typed NMS の `NMSDedicatedServer_*.java`, `docs/design/command-registration-audit.md`
- 5dc77d92: Forge 1.16.5 には `Component.nullToEmpty` が無く、`runTests` が `NoSuchMethodError` で落ちて report 待ちが timeout していた。`integration-test/shared/modded/.../TestMain.java`
- dd21e62e: Mohist の image は fixture 入りの bootstrap 実行から作られるため、起動時にも `#minecraft:load` が走る。test command 実行後の回数だけを数えるようにした。`integration-test/shared/*/.../DataPackFunctionTest.java`
- e51a8297: 1.21.2 以降は `RecipeHolder#id` が `ResourceKey` になった。neoforge-1.21.4 の recipe 比較を registry key の location で行うよう修正。`integration-test/shared/modded/.../ArgumentTest.java`
- ad060861: Mohist 1.20.1 は player の command が `CommandSyntaxException` で失敗すると、Bukkit の "Unknown command" を返す（vanilla の `/gamemode not_a_mode` でも同じ）。mohist-1.20.1 では bot がこの reply も受け入れるようにした。`integration-test/src/test/java/.../IntegrationTest.java`

### 5.8 2026-10-16 の実 server 検証結果（23456442）

**変更内容**

設定済みの 74 target のうち、各 minor version を代表する target を実行し、すべて全 case が通った。結果は `docs/compatibility.md`、`docs/vanilla-integration.md`、`integration-test/README.md` に記録した。実行したのは計 29 target（`docs/roadmap.md` の当初の「32」は数え違いで、`1b8f17dc` で 29 に直した）。

| Platform | module | 実行した target | 結果 |
|---|---|---|---|
| Spigot | `spigot` | 1.16.5, 1.17.1, 1.18.2, 1.19.4, 1.20.4, 1.21.1, 1.21.11, 26.1.2, 26.2, 26.3（10） | 全 pass |
| Paper | `spigot` | 1.16.5, 1.18.2, 1.20.4, 1.20.6, 1.21.1（5） | 全 pass |
| Paper | `paper` | 1.21, 1.21.4, 1.21.11, 26.1.2, 26.3（5） | 全 pass |
| Paper（非対応構成） | `spigot` | `paper-26.3-spigot-jar`（paper artifact を案内する enable 失敗を期待） | pass |
| Mohist | `spigot` | 1.16.5, 1.20.1（2） | 全 pass |
| Forge | `forge-*` | 1.16.5, 1.18.2, 1.19.2, 1.20.1（4） | 全 pass |
| NeoForge | `neoforge-*` | 1.21.1, 1.21.4（2） | 全 pass |

対象は argument 全般（range / duration / block position / predicate を含む）、`/execute`（`store` を含む）、command block、function / function tag / `#minecraft:load`、option、`visibleWhen` + `refreshAll`、`executeAsync` + middleware、diagnostics、player locale の caption（Bukkit 系のみ）。

**カバーしていないもの**

- Fabric、Folia、Velocity の integration target が無い（Velocity は 2026-10-15 の手動 proxy 実行のみ。option の redirect 化も Velocity は未検証）。
- optional / variadic argument の実 server fixture が無い。
- caption の locale は bot 既定の `en_us` しか確認していない。
- Forge / NeoForge の command block と function。
- Mohist 1.20.1 は player の構文エラーに "Unknown command" を返すため、CommandLib の失敗 message（caption を含む）が player に届かない。test 側で受け入れているだけで、ライブラリとしては未解決。
- 上記以外の patch release の target（設定はあるが今回は未実行）。

**主要ファイル**

- `docs/compatibility.md`, `docs/vanilla-integration.md`, `integration-test/README.md`, `docs/roadmap.md`

### 5.9 Build・品質（b0224d77, a972b06b, a8077449, f1aa6ad1, 56fb3076, 020eda43）

**変更内容**

- b0224d77: Error Prone と NullAway の対象を、modded / modded-testing の Forge / NeoForge node と typed NMS module を含む全 published module に広げた。modded node も Bukkit 系と同じ doclint 設定で javadoc jar を publish する（Maven Central の要件）。compiler は JDK 21、toolchain がそれより新しければ toolchain（spigot-26.1.2 は Java 25 の server jar に対して compile する）。指摘への対応:
  - modded: nullable field に annotation を付けた。`Location` は world が無いとき素の NPE ではなく明示的に報告する。`CommandContext` の空 catch を削除。
  - `RawWordArgumentType` が `ArgumentTypeInfos` を lock するのは意図通り（shade された各 copy が同じ map に登録するため）なので、理由を書いて suppress した。
  - typed NMS: generator が `getHandle()` ではなく新しい `NMSClass.requireHandle()` を呼ぶようにした。`NMSCommandListenerWrapper` の nullable override に `@Nullable` を付けた。全 module を再生成。
- a972b06b: published される全 archive（sources / javadoc / remapped jar を含む）に `LICENSE` と `THIRD_PARTY_NOTICES.md` を入れる。依存 license の監査用に `tools/license-audit.init.gradle` を追加（`./gradlew -I tools/license-audit.init.gradle licenseDependencyReport`）。
- a8077449: `generate.py --all` の出力と一致するよう、typed NMS の生成ソースの import 順を揃えた（package 移動前の順のままだった）。
- f1aa6ad1: root と samples の `gradlew` が mode 100644 で commit されており、Linux / macOS で実行できなかった。実行権限を付けた。
- 56fb3076: `.gitignore` に Kotlin の `.kotlin` を追加し、`t` という名前を全部無視していた紛れ込みの pattern を削除。
- 020eda43: `spigot-kotlin` の test classpath に `:common` の caption bundle を追加。

**主要ファイル**

- `build.gradle.kts`, `docs/agents/testing.md`, `THIRD_PARTY_NOTICES.md`, `tools/license-audit.init.gradle`, `.gitignore`, `spigot-kotlin/build.gradle.kts`
- `modded/src/main/java/dev/marulabs/commandlib/util/Location.java`, `nms/**`（再生成）

### 5.10 Benchmark（9cf955f2, 1b928a3d, 608256e9）

**変更内容**

- 9cf955f2: JMH の `benchmark` module を追加。server なしで、`common-testing` の `CommandTester` 経由で common を駆動する。publish はしない。`check` では compile だけ行い、実行は専用 task のみ。対象は `RegistrationBenchmark`（10 / 100 / 1000 command）、`OptionTreeBenchmark`（short flag 2 / 4 / 6）、`ExecuteBenchmark`、`SuggestBenchmark`。結果を取る前に regression 許容値（p50 +10%、p95 +20%、throughput -10%、registration 平均 +15%、allocation +5%）を `docs/benchmarks.md` に決めておき、`:benchmark:jmhCompare` がそれを超えたら fail する。
- 1b928a3d: 最初の baseline（`benchmark/baseline.json`）を記録し、`:benchmark:jmhBaseline` を追加。負荷のある desktop での実行だったので、信頼できるのは median と allocation。short flag 6 個の command は、展開後の command packet で 491,607 node になることが分かった。
- 608256e9: option redirect 化の後に再測定。1 command あたりの node は 61→47、展開後は 305→61。flag 6 個では展開後 491,607→5,490。allocation はすべて減少か 1.2% 以内。

**主要ファイル**

- `benchmark/build.gradle.kts`, `benchmark/baseline.json`
- `benchmark/src/main/java/dev/marulabs/commandlib/benchmark/{Registration,OptionTree,Execute,Suggest}Benchmark.java`, `Trees.java`
- `docs/benchmarks.md`, `docs/design/command-options.md`

**使用例**

```bash
./gradlew -Pcommandlib.typedNms=false --configure-on-demand :benchmark:jmh         # 全 benchmark（約 9 分）
./gradlew -Pcommandlib.typedNms=false --configure-on-demand :benchmark:jmhCompare  # baseline.json と比較
./gradlew -Pcommandlib.typedNms=false :benchmark:jmh --args="-f 1 -wi 3 -i 5 -prof gc -rf json -rff build/results/jmh/results.json SuggestBenchmark"
./gradlew -Pcommandlib.typedNms=false :benchmark:jmhBaseline                       # baseline を更新
```

### 5.11 Samples（40d106ba, a4715551, 723987b7, 4e080cc6）

**変更内容**

`samples/*` は独立した Gradle build で、各自 wrapper を持つ。

- 40d106ba: Spigot / Paper sample に optional / variadic argument、confirmation と cooldown、middleware 付き `executeAsync`、annotation style、Java builder、diagnostics command を追加。Paper sample では `paper-testing` を使った `SampleCommandsTest` で実行確認する。
- a4715551: Velocity sample（`/send <player> <server>`、`/find <players...>`）と Paper Kotlin DSL sample を追加。
- 723987b7: Fabric sample。CommandLib を `modImplementation` で取り込み、shadow で relocate してから `remapJar` で intermediary に戻す。CommandLib 自身の `fabric.mod.json` は jar から除く。`-PcommandlibLocal=true` を付けると mavenLocal に publish したこの repository の build を使う。
- 4e080cc6: Forge sample に optional / variadic argument、confirmation、diagnostics を追加。

**主要ファイル**

- `samples/spigot/`, `samples/paper/`（`ArenaCommand`, `FeedCommand`, `LookupCommand`, `ResetCommand`, `SampleCommandsTest`）
- `samples/velocity/`, `samples/paper-kotlin/`, `samples/fabric/`, `samples/forge/`

## 6. 判断が必要な箇所と未対応事項

詳細と最新状態は `docs/roadmap.md`（`[?]` が要判断、`[ ]` が未着手、`[~]` が部分対応）。

### 6.1 判断が必要な箇所

| 項目 | 現状 | 選択肢と影響 | 関連ファイル |
|---|---|---|---|
| 構文エラー時の usage 表示を既定にするか | `ErrorFormatter.SYNTAX_ADVICE` は opt-in | 既定にすると全 module の argument error の出力が変わる（既存 test と docs の期待値も変わる） | `common/src/main/java/dev/marulabs/commandlib/ErrorFormatter.java`、`docs/help.md` |
| `/execute as` の後の actor を platform 間で揃えるか | Spigot / Paper は元の sender（console）のまま、Forge / NeoForge は実行 entity に従う（permission 判定も） | Bukkit 側を entity に寄せる（vanilla に近いが、Bukkit plugin の慣習と違う）／modded 側を元の sender に寄せる／差分のまま docs に明記する | `docs/vanilla-integration.md`、`spigot/.../CommandContext.java`、`modded/.../CommandContext.java` |
| 0.18.0 の release 時期 | 未 release。`net.kunmc.lab` → `dev.marulabs` の package 移動、`AbstractCommandContext` 削除など 0.17.x と binary 互換でない変更を含む | release 後に japicmp の基準（`build.gradle.kts` の `apiBaselines`、現在 0.17.2）を移す | `build.gradle.kts` |
| JitPack → Maven Central 移行の時期 | javadoc jar と doclint は設定済み | 署名・namespace 登録・publish 設定が必要。docs site の公開先もこの方針に依存 | `buildSrc/`、各 module の `build.gradle.kts` |
| help から「子 command にだけ続く argument branch」を外した変更（`59baaa24`）を残すか | 外した状態 | 戻すと usage 一覧に実行できない行（`/cmd <arg>` だけで子が必須のもの）が再び出る | `common/src/main/java/dev/marulabs/commandlib/HelpMessageAction.java` |
| modded の loader 非依存層を分離するか | 分離していない（Fabric 追加時点で loader 依存は登録・permission・server 取得・reflection 2 箇所・metadata のみ） | 分離すると module が増える。いまは不要という判断 | `modded/` |
| config-driven customization（優先度低） | 未着手 | server admin が YAML で command 名・alias・message を変える機能を作るか | — |

確認だけが必要なもの:

- JitPack の javadoc URL が実際にブラウザで表示されるか（未確認）。

### 6.2 未対応事項

**実サーバーでの検証が残っているもの**

- optional / variadic argument: integration test の fixture がない。
- option の redirect 化（`OptionGraph`）: Velocity が未検証。
- caption の locale: bot の既定 `en_us` だけで、他の locale の bot がない。
- Paper の resolve error の translatable component が実 client で翻訳されるか、`MessageComponentSerializer` が Paper 1.21.3〜26.x で動くか。
- command block と function tag の tests、Forge / NeoForge での command block / function。
- 表に載っていない Spigot / Paper の patch release（Spigot 1.18、1.19〜1.19.3 など。一覧は `integration-test/README.md`）。
- `ExecuteBenchmark` を idle な環境で baseline と比べ直す（並列 build 中の計測だったため）。

**integration target がないもの**

- Fabric（test mod と server setup の Fabric 対応、`samples/fabric` の shade jar の読み込み）。
- Folia（共有 paper fixture が `Bukkit.getScheduler()` を使うため、Folia 用 fixture が必要）。
- Velocity（proxy + backend + bot。`ServerArgument` の補完で quote が必要な名前の quote もある）。

**見つかった問題**

- Mohist 1.20.1 は player の構文エラーに Bukkit の "Unknown command" で答えるため、CommandLib の失敗 message（caption を含む）が player に届かない。回避策は未調査。
- `modded-testing` の fake source は level を持たないため selector を解決できない。`docs/arguments.md` の「real selectors」の記述を見直す必要がある。

**機能の未対応**

- modded の 1.21.5〜1.21.11 と 26.x。1.21.11 は `ResourceLocation` → `Identifier`、permission level → `PermissionSet` などで約 50 箇所が壊れる。26.x は `loom-no-remap` と Java 25 が必要。
- Kotlin DSL の Forge / NeoForge / Fabric 版（Stonecutter の Loom node ごとに module が要る）。Kotlin 関数の default 引数から command を作る形。
- cooldown の永続化（restart 後の復元 hook）、argument branch 単位の confirm / cooldown / `@Condition`。
- option の継承と配置の拡張（subcommand への継承、固定 arity の multi-value）。
- 組み込み message の他言語 bundle（現在は en / ja）。
- annotation の `java.util.Optional<T>` 引数（core が null default を許さない）。
- rich help（hover / click の usage 行）と MiniMessage。
- capability-based registration、sender 側の abstraction。
- integration test 用 mod（`integration-test/shared/modded`）への Error Prone / NullAway。

**CI と配布（roadmap に記録するだけで、まだ作っていない）**

- CI（typed NMS の BuildTools の cache、integration test の matrix、japicmp の実行）。
- docs site の公開（`mkdocs.yml` はあり、`mkdocs build --strict` が通る）。

### 6.3 コード中の shortcut（既知の制限）

| 場所 | 制限 | 直す時期 |
|---|---|---|
| `modded/src/main/java/dev/marulabs/commandlib/CommandContext.java` | Fabric 1.20.1 は player の言語を読まず既定言語を使う（vanilla 1.20.1 は言語を intermediary 名の private field に持つ） | mod が必要とするなら accessor mixin で読む |
| `common-kotlin/src/main/kotlin/dev/marulabs/commandlib/CurrentCommandContexts.kt` | package-private の `CurrentCommandContext` に同じ package から触るため、common-kotlin は common と同じ class loader で読まれる必要がある（1 つの plugin に shade すれば満たす） | 別 class loader で使う需要が出たら `CurrentCommandContext` を public にする |
| `paper/src/main/java/dev/marulabs/commandlib/argument/BlockPredicateArgument.java` | Paper 1.21 API に対して compile するため、1.21.8+ の `blockInWorldPredicate` を reflection で呼ぶ | module の最低 version を 1.21.8 に上げたら直接呼ぶ |

