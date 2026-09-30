# Testing Strategy

この文書は CommandLib のテスト戦略を定義する。特に NMS / Reflection 周りのテスト手法と、各テスト層の役割・方針を扱う。

## テスト層の全体像

```
Layer 1: unit tests  (spigot:test, common:test)
Layer 2: spigot-test (command-level unit tests with NMS mocks)
Layer 3: integration-test (Docker + 実 Minecraft サーバー)
```

各層は独立して実行できる。Layer 3 は opt-in（`COMMANDLIB_RUN_MINECRAFT_IT=true`）。

---

## Layer 1: ユニットテスト（spigot:test, common:test）

対象: parser ロジック、command tree、options、permissions、help、validation。

- Bukkit / NMS に依存しない純粋なロジックテスト。
- `common` module のテストはここが中心。
- Mockito によるスタブは最小限にとどめる。

---

## Layer 2: command-level テスト（spigot-test）

対象: 引数の parse・validation・suggestion、コマンド実行の結果。

### 基本設計

`CommandTester` が NMS を完全にスタブする。

```java
try (CommandTester tester = new CommandTester(command, "myplugin.cmd")) {
    FakeSender sender = FakeSender.player("Alice");
    tester.execute("heal Alice 10", sender);
    assertThat(sender.getSentMessages()).contains("Healed Alice for 10");
}
```

### NMS スタブの仕組み

`CommandTester` のコンストラクタで 2 つの static mock を張る。

| Mock 対象 | 効果 |
|---|---|
| `NMSReflection` | `findMinecraftClass` / `findCraftBukkitClass` を全て `Object.class` 返しにする |
| `NMSClassRegistry.findClass()` | デフォルト `UnsupportedOperationException`。引数ごとに Mock 実装クラスを返すよう上書き |

`NMSClassRegistry` のスタブで差し替えられる Mock 実装クラスは `spigot-test` に置く。
各 Mock クラスは対応する `NMSClass` サブクラスを extends し、実 NMS を使わずに引数 parse・値返却を実装する。

```
NMSArgumentPlayer (spigot/nms/argument)
  └─ MockNMSArgumentPlayer (spigot-test/nms/argument)
       parse() → CommandTester.getFakeEntity(name)
```

### 新引数追加時の手順

1. `spigot/nms/argument/NMSArgumentXxx.java` を追加する。
2. `spigot-test/nms/argument/MockNMSArgumentXxx.java` を追加する。
3. 共有 mock として常時使う場合は `CommandTester` の既定 mock 登録に追加する。個別テストだけで使う場合は `CommandTester.builder().mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)` で登録する。
4. `spigot-test/argument/XxxArgumentTest.java` を追加し、parse / suggestion / validation / error を網羅する。

**引数追加時は常に Mock と Test をセットで作成する。**

### CommandTester の拡張性課題と方針

`CommandTester` は既定の NMS mock 登録に加えて、Builder からテスト固有の mock を差し込める。

```java
CommandTester tester = CommandTester.builder()
    .mockNmsClass(NMSArgumentXxx.class, MockNMSArgumentXxx.class)
    .command(() -> new XxxCommand())
    .permissionPrefix("prefix")
    .build();
```

既存の `Mockito.mockStatic` 方式は維持する。
CommandAPI が採用している「実 NMS インスタンス + `Bootstrap.bootStrap()` + Mockito スパイ」は CommandLib では不要。
テスト速度と環境依存性の観点から、Mock 実装クラスによる完全スタブ方式を維持する。

---

## Layer 3: integration テスト（integration-test）

対象: 実 Minecraft サーバーでの command registration・実行・tab completion・permission。

### 仕組み

- Testcontainers で Docker コンテナ内に Minecraft サーバーを起動する。
- MCProtocolLib で TCP 接続し、実プロトコルでコマンドを送受信する。
- 各 target（`integration-test/targets/<platform-version>/test-plugin/`）に test plugin と `server.jar` を置く。
- Gradle の `IntegrationTarget` でバージョン・Java バージョン・MCProtocolLib バージョンを管理する。

### 現在対応バージョン

| ID | MC バージョン | Platform |
|---|---|---|
| 1165 | 1.16.5 | Spigot |
| 1165Mohist | 1.16.5 | Mohist |
| 1194 | 1.19.4 | Spigot |
| 1201 | 1.20.1 | Spigot |
| 1201Mohist | 1.20.1 | Mohist |
| 1204 | 1.20.4 | Spigot |
| 1205 | 1.20.5 | Spigot |
| 1206 | 1.20.6 | Spigot |
| 1210 | 1.21.0 | Spigot |

`IntegrationTarget` に `platform` フィールドが未追加（→ `testing-module-structure.md` の Roadmap Checklist 参照）。

### 実行方法

```bash
# 全バージョン
./gradlew :integration-test:minecraftIntegrationTest

# 特定バージョン
./gradlew :integration-test:minecraftIntegrationTest1165

# 環境変数でも制御可能
COMMANDLIB_RUN_MINECRAFT_IT=true ./gradlew :integration-test:test
```

### integration テストの追加判断基準

以下に該当する場合に integration テストを追加する。

- NMS 引数の parse 結果がバージョンによって異なる可能性がある。
- command registration / unregistration の副作用を検証したい。
- permission 登録・reload が正しく動作するか確認したい。
- `/execute` やコマンドブロック・function との連携を保証したい。

Layer 2 (spigot-test) で検証できる場合は integration テストを追加しない。

---

## NMSClassRegistry と NMSClassResolver の設計

### NMSClassRegistry

バージョン範囲とクラスの対応を静的に管理するレジストリ。

```java
NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayerImpl_v1_20.class, "1.20", "1.20.4");
NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayerImpl_v1_20_5.class, "1.20.5", "");
```

実行時は `BukkitUtil.getMinecraftVersion()` で範囲内の実装クラスを解決する。
テスト時は `Mockito.mockStatic` で差し替える。

### NMSClassResolver

`net.minecraft.server.{version}` 形式と 1.20.5 以降の非バージョン付きパッケージの両方に対応する。
`Bukkit.getServer().getClass().getClassLoader()` を使いランタイムのクラスローダーを取得する。

---

## CommandAPI との比較

CommandAPI は「実 NMS + Mockito スパイ + `Bootstrap.bootStrap()`」でテストする重量級戦略を採用している。
CommandLib の現行戦略（Mock 実装クラス + `Mockito.mockStatic`）はより軽量で：

- Minecraft サーバー jar のランタイム依存が不要。
- `Bootstrap.bootStrap()` による registry 初期化が不要。
- テストの起動が速い。
- バージョン別モジュール分割が不要。

代わりに Mock 実装クラスのメンテナンスコストがある。
integration テスト（Layer 3）で実 NMS 動作は保証されるため、この分担は合理的。

---

## 今後の対応方針

### 短期（P1 完了前）

- `CommandTester` の拡張性改善（Builder パターン化）を設計する。
- `IntegrationTarget` に `platform` フィールドを追加する。
- 新引数追加時の「Mock + Test セット作成」ルールをこの文書に従って運用する。

### 中期（P2: CommandAPI parity）

- 新規引数（NamespacedKey, Biome, Sound, Advancement 等）を実装する際、対応 `MockNMSArgument*` と `ArgumentTest` を同時に追加する。
- suggestions（sync / async）・tooltip の spigot-test coverage を追加する。
- `/execute`・command block・function の integration テスト coverage を追加する。

### 長期（P3 以降）

- **Paper module のテスト**: `paper-testing` は command-level behavior、Adventure component、suggestions / tooltip、Paper `ArgumentTypes` / selector / registry-backed argument の fake path を検証する。Paper 公式 lifecycle registration は `integration-test` の実 Paper server targets で保証するため、`paper-testing` では再現しない。
- **JUnit extension 化**: `@ExtendWith(CommandLibExtension.class)` + `@RegisterCommand` アノテーションで `CommandTester` を宣言的に使えるようにする（downstream 向け）。
- **Forge / Fabric**: `NMSClassRegistry` は Bukkit 専用。Forge / Fabric は Brigadier を直接使うため、別のテスト設計が必要（P4 で検討）。

---

## Roadmap Checklist

- `[x]` `spigot-test` に `CommandTester` / `FakeSender` を整備する。
- `[x]` NMS mock クラス群（引数・command listener・world 系）を整備する。
- `[ ]` `CommandTester` を Builder パターン化し、外部から Mock 登録を差し込めるようにする。
- `[ ]` `IntegrationTarget` に `platform` フィールドを追加する。
- `[ ]` suggestions / tooltip の spigot-test coverage を追加する。
- `[ ]` `/execute`・command block の integration テストを追加する。
- `[x]` `paper-testing` 設計を開始する（Paper module API 確定後）。
- `[ ]` JUnit extension を追加する（downstream 向け）。
- `[ ]` Forge testing 設計を開始する（Forge module version strategy 確定後）。
