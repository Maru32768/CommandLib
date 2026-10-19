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
