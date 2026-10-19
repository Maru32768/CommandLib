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
