# CommandLib Roadmap History

完了済み roadmap エントリのアーカイブ。現在進行中の項目は `roadmap.md` を参照。

## 完了済みバックログ

### P0: 方針を固定する（完了）

- `[x]` `docs/compatibility.md` を追加し、supported / tested / best-effort / unsupported を分ける。
- `[x]` `docs/design/argument-catalog.md` を追加し、CommandLib / CommandAPI / Paper の argument 対応表を作る。
- `[x]` 現行 `bukkit` module は Spigot 互換実装として扱う方針に決める。
- `[x]` GA 前 module split plan を作り、`bukkit` artifact は削除する方針に決める。
- `[x]` spigot / paper 向け test module と integration-test の整理方針を決める。

### P1: GA 前 module split を実行する（完了）

- `[x]` 現行 `bukkit` module を `spigot` に rename / move する。
- `[x]` `bukkit` artifact を削除し、publishing を `spigot` artifact に切り替える。
- `[x]` `bukkit-test` を `spigot-test` に rename / move し、`:spigot` 依存へ変更する。
- `[x]` `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
- `[x]` integration target metadata に platform を追加する（`IntegrationTarget` に `Platform` enum フィールドを追加）。
- `[x]` sample / fixtures / docs / README の `bukkit` artifact 参照を `spigot` に置き換える。
- `[x]` `:spigot:compileJava`, `:spigot:test`, `:spigot-test:test` を通す。

### P2: Paper module を新設する（完了）

- `[x]` `paper` module を追加する。
- `[x]` Paper official command / lifecycle API を使った registration path を実装する。
- `[x]` Adventure component を Paper module の標準 message path にする。
- `[x]` Paper-specific suggestions / tooltip behavior を実装する。
- `[x]` Paper registry-backed arguments の設計を開始する。
- `[x]` `paper-testing` を追加する。
- `[x]` Paper integration targets を `integration-test` に追加する。

### P3: CommandAPI parity（完了分）

- `[x]` suggestions with tooltip を実装する。`SuggestionBuilder.suggest(String, Message)` を common に追加済み。Paper では
  `PaperMessages.toMessage(Component)` でリッチ tooltip が使える。
- `[x]` suggestions / tooltip の spigot-test coverage を追加する。`CommandTester.suggestions()` を追加し、tooltip
  テストを追加済み。

## 完了済み Release Phases

### Phase 0: Freeze Direction（完了）

目的: GA 前に module / artifact / platform support の方針を固定する。

- compatibility 方針を文書化する。
- argument catalog を作る。
- module split 方針を文書化する。
- testing module 方針を文書化する。
- `bukkit` artifact を削除し、現行実装を `spigot` に移す方針を固定する。

Exit criteria（達成済み）:

- `docs/compatibility.md` が存在する。
- `docs/design/argument-catalog.md` が存在する。
- `docs/design/module-structure.md` が存在する。
- `docs/design/testing-module-structure.md` が存在する。

### Phase 1: GA Module Split（完了）

目的: GA 前に `bukkit` artifact を削除し、Spigot / Paper の platform contract を分ける。

- `bukkit` module を `spigot` に rename / move する。
- `bukkit-test` を `spigot-test` に rename / move する。
- `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
- `paper` module を新設する。
- `paper-testing` を新設する。
- samples / fixtures / README / publishing を platform-specific artifacts に更新する。

Exit criteria（達成済み）:

- `:spigot:compileJava` と `:spigot:test` が通る。
- `:spigot-test:test` が通る。
- `integration-test` aggregator に platform metadata がある。
- README が `bukkit` artifact を案内していない。
- `paper` module の最小 registration path がある。

## Immediate Next Actions（完了）

1. `[x]` `bukkit` module を `spigot` module に rename / move する。
2. `[x]` `bukkit` artifact を削除し、publishing を `spigot` artifact に切り替える。
3. `[x]` `bukkit-test` を `spigot-test` に rename / move する。
4. `[x]` `bukkit-integration-test` を `integration-test` aggregator に rename / move する。
5. `[x]` `paper` module の最小 skeleton を追加する。
6. `[x]` `IntegrationTarget` に `Platform` enum フィールドを追加する。
7. `[x]` `CommandTester` を Builder パターン化し、Mock クラスの外部登録を可能にする。
