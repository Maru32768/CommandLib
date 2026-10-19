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
