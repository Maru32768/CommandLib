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
