# Rich Text Messages

この文書は、CommandLib の message API（`TextComponentBuilder` 系、`sendMessageWithOption`、`sendComponent`）を、
platform 非依存で使える正式な rich text API に作り直すための設計案である。

Status: 提案（未実装）。決定待ちの項目は [Open questions](#open-questions) にまとめる。package 名は現行の
`net.kunmc.lab.commandlib` で書く（`dev.marulabs` への rename 後も構成は同じ）。

## 背景

`TextComponentBuilder` / `TranslatableComponentBuilder` は、`common` の `ArgumentParseException` が
platform ごとの component を組み立てるために作られた内部用の部品である。しかし class も `PlatformAdapter` の
factory method も public で、ConfigLib からも `sendMessageWithOption` が使われている。ここで正式な API として
設計し直すか、内部用に戻すかを決める。

Roadmap 上の関連項目:

- P5「sender / message abstraction（Adventure と vanilla `Component` の差分吸収）を Velocity 着手前に設計する」。
- Pillar 6 Stretch「MiniMessage / Adventure component support」「hover / click 対応の rich help」。
- `docs/compatibility.md` / `docs/design/module-structure.md` の open question
  「message API は Adventure を標準にするか、CommandLib 独自 abstraction を挟むか」。

## 現状

### 公開 API

| 型 / method | module | 内容 |
|---|---|---|
| `util.text.ComponentBuilder<B, E, T>` | common | 抽象 builder。`color(int rgb)`、`italic()`、`append(B)`、`build()` のみ。 |
| `util.text.TextComponentBuilder` / `TranslatableComponentBuilder` | common | 中身のない抽象 subclass。translatable の引数（`with`）は渡せない。 |
| `util.text.TextComponentBuilderImpl` / `TranslatableComponentBuilderImpl` | spigot / paper / modded | Bungee `BaseComponent` / Adventure `Component` / vanilla `Component` 版の実装。 |
| `PlatformAdapter#createTextComponentBuilder(String)` / `#createTranslatableComponentBuilder(String)` | common（interface） | 戻り値は `TextComponentBuilder<T, ? extends T, ?>`。 |
| `CommonCommandContext#sendMessageWithOption(String / Object, Consumer<MessageOption>)` | common（抽象）+ 各 platform 実装 | 文字列 1 個に RGB 色と hover text を付けて送る。 |
| `CommonCommandContext.MessageOption` | common | `rgb(int)`、`hoverText(String)` のみ。 |
| `CommonCommandContext#sendComponent(C)` | common（抽象） | platform の native component をそのまま送る。 |
| `sendMessage/sendSuccess/sendWarn/sendFailure(BaseComponent)` | spigot のみ | Paper / modded には `sendSuccess(Component)` 等がない（modded は `sendMessage(Component)` のみ）。 |
| `TestTextComponentBuilder` / `TestTranslatableComponentBuilder` | common-testing（package-private）、common/src/test に別コピー | `String` を component とする test 実装。色は `§x§r§r...` を前置、italic は無視。 |

### 呼び出し箇所

- CommandLib 内で builder を使うのは `common/.../exception/ArgumentParseException#buildIncorrectInputMessage` の
  1 箇所だけ。`command.unknown.argument` と、`§7` / `§c§n` の legacy code を埋め込んだ text +
  `command.context.here` を送る。型を合わせられないため raw type の `PlatformAdapter` と `CommonCommandContext` を
  使っている。
- help（`HelpMessageAction`）、権限 / 実行時 error（`CommandRunner`）、option error（`CommandOption`）、各 argument の
  parse error は、すべて `§` code 入りの `String` を `sendMessage` / `sendFailure` に渡している。
- `sendMessageWithOption` を CommandLib 本体で使う箇所はない（tests のみ）。

### ConfigLib の利用（`../ConfigLib` を確認）

- `ConfigListCommand`、`ConfigFieldCommand`（3 箇所）、`ConfigHistoryCommand` が、`CommonCommandContext<?, ?>` に対して
  `sendMessageWithOption(text, o -> o.rgb(ChatColorUtil.GREEN.getRGB()).hoverText(description))` を呼ぶ。
  `TextComponentBuilder` と `sendComponent` は使っていない。
- header は `ChatColorUtil` の `§` code を埋め込んだ `String` を `sendMessage` で送る。
- ConfigLib の command 生成は `common` に置かれ、component 型を `?` のまま扱う。このため現行 builder は
  ConfigLib から実質使えない（`build()` の戻り値が capture 型になり、別の `CommonCommandContext<?, ?>` の
  `sendComponent` に渡せない）。

推測（ConfigLib の roadmap には書かれていない）: 今後欲しくなるのは、値の click でのコピー、
`/config <field> set ` の click での入力欄への挿入、複数行の hover（history preview）程度で、item hover や
MiniMessage は必要ない。

### 問題点

1. common のコードから型安全に rich message を送れない。builder は platform の component 型を type parameter に持つため、
   `CommonCommandContext<?, ?>` を受け取るコード（CommandLib 本体の exception、ConfigLib）では raw type が必要になる。
2. 機能が足りない。bold などの装飾、click、translatable の引数、hover の rich text がない。
3. platform ごとに挙動が違う。Paper の builder は immutable な Adventure を包み、Spigot と modded は mutable な
   component を直接変更する。`build()` は同じ instance を返すので alias が残る。Spigot の
   `sendSuccess(BaseComponent)` は渡された component の色を上書きする。
4. `sendMessageWithOption` は hover text が空でも必ず `SHOW_TEXT` を付ける（3 platform とも）。ConfigLib は
   description がない entry に `""` を渡すので、空の tooltip が出る。
5. 同じ組み立てコードが 3 platform と 2 つの test context に重複している。

## Goals

- `common` に platform 非依存で immutable な rich text 値型を 1 つ置き、どの platform でも
  `ctx.sendMessage(text)` で送れるようにする。
- command 出力に必要な範囲（色、装飾、hover text、click、translatable 引数、子要素）を 3 platform で同じに描画する。
- 値で比較できる（`equals` / `hashCode` / 読める `toString`）ので、test で送信内容を assert できる。
- ConfigLib の既存呼び出しを壊さず、1 行で移行できる。

## Non-Goals（作らないもの）

- **Adventure の再実装はしない。** Paper 専用 plugin には、Adventure / MiniMessage を直接使い
  `sendComponent(Component)` で送ることを推奨し、README にもそう書く。この値型は、platform をまたぐコード
  （CommandLib 本体、ConfigLib のような library の common 層）のためのものである。
- Spigot / modded に Adventure を shade しない（`adventure-platform-*` が必要になり、relocate と衝突する）。
- MiniMessage や legacy `§` 文字列の parser / serializer は作らない。既存の `§` 入り文字列は text としてそのまま送る
  （client が描画する。現状と同じ挙動）。
- hover の item / entity 表示は扱わない。`ItemStack` / `Entity` は platform 型であり、必要な場合は native component を使う。
- keybind / score / selector / nbt component、font、shadow color（1.21.4+）、`change_page`（本専用）は扱わない。
- JSON serializer、mutable builder、独自の色クラスは作らない（色は `int rgb`）。

## 提案 API

### 値型 `RichText`（common、`net.kunmc.lab.commandlib.util.text`）

```java
public final class RichText {
    // 生成
    public static RichText text(@NotNull String content);
    public static RichText translatable(@NotNull String key, @NotNull RichText... args);

    // 変更はすべて新しい instance を返す（immutable）
    public RichText color(int rgb);
    public RichText decorate(@NotNull Decoration... decorations);           // = decoration(d, true)
    public RichText decoration(@NotNull Decoration decoration, boolean state); // false は親からの継承を打ち消す
    public RichText hover(@Nullable RichText text);                          // null で解除
    public RichText click(@Nullable ClickAction action);                     // null で解除
    public RichText append(@NotNull RichText... children);

    // 読み取り（platform renderer と test 用）
    public @Nullable String text();             // translatable なら null
    public @Nullable String translationKey();   // literal なら null
    public @NotNull List<RichText> arguments();
    public @NotNull List<RichText> children();
    public @Nullable Integer color();
    public @Nullable Boolean decoration(@NotNull Decoration decoration); // null = 未指定（継承）
    public @Nullable RichText hover();
    public @Nullable ClickAction click();

    /** text を連結した文字列。translatable は key を出し引数は出さない（Paper の plain serializer と同じ）。 */
    public @NotNull String plainText();

    // equals / hashCode / toString は値ベース
}

public enum Decoration { BOLD, ITALIC, UNDERLINED, STRIKETHROUGH, OBFUSCATED }

public final class ClickAction {
    public static ClickAction runCommand(@NotNull String command);
    public static ClickAction suggestCommand(@NotNull String command);
    public static ClickAction copyToClipboard(@NotNull String text);
    public static ClickAction openUrl(@NotNull String url);

    public @NotNull Type type();
    public @NotNull String value();

    public enum Type { RUN_COMMAND, SUGGEST_COMMAND, COPY_TO_CLIPBOARD, OPEN_URL }
}
```

名前を `Text` にしないのは、Fabric（yarn mappings）の `net.minecraft.text.Text` と Bungee の
`hover.content.Text` に衝突するため。`Message` は Brigadier の `com.mojang.brigadier.Message` に衝突する。

### 送信 API（`CommonCommandContext`）

```java
public abstract void sendMessage(@NotNull RichText text);   // 各 platform が native に変換して送る

public void sendSuccess(@NotNull RichText text) { sendMessage(text.colorIfAbsent(GREEN)); }
public void sendWarn(@NotNull RichText text)    { sendMessage(text.colorIfAbsent(YELLOW)); }
public void sendFailure(@NotNull RichText text) { sendMessage(text.colorIfAbsent(RED)); }
```

- `colorIfAbsent` は package-private の helper とし、root に色がないときだけ色を付ける。渡された値は変更しない
  （Spigot の `sendSuccess(BaseComponent)` のような上書きはしない）。
- modded は `sendFailure(RichText)` を override し、既存の `sendFailure(String)` と同じく
  `CommandSourceStack#sendFailure` に流す。
- 既存の `sendMessage(Object)` / `sendMessage(String)` overload と並ぶ。リテラル `null` を渡す呼び出しは曖昧になり
  compile error になる（repository 内には該当なし）。

使用例（ConfigLib の list）:

```java
ctx.sendMessage(RichText.text(entry.entryName() + ": " + value)
                        .color(ChatColorUtil.GREEN.getRGB())
                        .hover(description.isEmpty() ? null : RichText.text(description))
                        .click(ClickAction.suggestCommand("/config " + entry.entryName() + " set ")));
```

### platform 変換の公開

各 platform に `util.text.RichTexts`（static utility）を置く。

- spigot: `BaseComponent toBungee(RichText)`
- paper: `Component toAdventure(RichText)`
- modded: `Component toVanilla(RichText)`

`CommandContext#sendMessage(RichText)` はこれを使う。利用者もこれで変換し、item hover などの native 機能を
後から足せる。

## platform 実装方針

各 renderer は `RichText` の木を再帰的に native component に変換するだけで、1 platform あたり数十行で済む。
色は、legacy 16 色の RGB と完全に一致する場合は named color（Bungee `ChatColor.GREEN`、`NamedTextColor.namedColor(rgb)`、
`ChatFormatting`）に戻す。こうすると console 出力と既存 test の色の比較が今と変わらない。

| 機能 | Spigot（Bungee chat。compile は API 1.16.5、runtime は 1.16.4〜1.21.1） | Paper（Adventure、1.20.6+） | modded（vanilla。forge 1.16.5 / forge 1.20.1 / neoforge 1.21.1） |
|---|---|---|---|
| literal | `new TextComponent(s)` | `Component.text(s)` | <1.19 `new TextComponent(s)` / 1.19+ `Component.literal(s)` |
| translatable + 引数 | `new TranslatableComponent(key, (Object[]) args)` | `Component.translatable(key, args)` | <1.19 `new TranslatableComponent(key, args)` / 1.19+ `Component.translatable(key, args)` |
| hex 色 | `ChatColor.of(new Color(rgb))`（1.16+） | `TextColor.color(rgb)` | `TextColor.fromRgb(rgb)` |
| 装飾（tri-state） | `setBold(Boolean)` 等。5 種とも `Boolean` setter あり | `decoration(d, TextDecoration.State.byBoolean(b))` | `Style.withBold(Boolean)` 等。`[?]` 1.16.5 で strikethrough / obfuscated / underlined の `Boolean` setter がない場合は `applyFormat(ChatFormatting.X)` で代替し、explicit false は表現しない |
| hover text | `new HoverEvent(SHOW_TEXT, new Text(new BaseComponent[]{c}))`（1.16 の content API） | `HoverEvent.showText(c)` | `new HoverEvent(HoverEvent.Action.SHOW_TEXT, c)`。1.21.5+ は `HoverEvent.ShowText`（record）に変わる |
| click | `new ClickEvent(ClickEvent.Action.X, value)` | `ClickEvent.runCommand(v)` 等 | `new ClickEvent(ClickEvent.Action.X, value)`。1.21.5+ は `ClickEvent.RunCommand` 等の record に変わり、`open_url` は `URI` を取る |
| 子要素 | `addExtra(c)` | `append(c)` | `append(c)` |
| plain text（test） | `toPlainText()` | `PlainTextComponentSerializer` | `getString()` |

補足:

- 対応範囲の最古は 1.16.4 / 1.16.5 なので、hex 色、`copy_to_clipboard`（1.15+）、hover content API（1.16+）は
  すべての target で使える。downsampling はしない。
- 1.20.3 以降に起きた text component の network 形式の変更（NBT 化、1.21.5 の snake_case 化）は server 側が吸収し、
  API には影響しない。1.21.5+ で変わるのは modded の Java API（上表）だけで、Stonecutter の条件分岐で対応する。
  現行の target はどれも 1.21.5 未満。
- Velocity を追加するときは、Paper の Adventure renderer をそのまま使える（adventure-api は共通）。

## 既存 API の扱い

| 対象 | 方針 |
|---|---|
| `ArgumentParseException` の内部実装 | `RichText` で書き直し、raw type をなくす。出力は同じ（translatable 2 個 + `§` 入り text）。 |
| `ComponentBuilder` / `TextComponentBuilder` / `TranslatableComponentBuilder`、各 `*Impl`、`PlatformAdapter#create*Builder` | 本体から使わなくなるので `@Deprecated` にする。削除は package rename（`dev.marulabs`）と同じ release で行うことを推奨する。どちらにせよ全 import が変わるので、利用者の手間は増えない。 |
| `TestTextComponentBuilder` / `TestTranslatableComponentBuilder`（common-testing、common/src/test） | `PlatformAdapter#create*Builder` と一緒に削除する（package-private なので公開 API ではない）。 |
| `sendMessageWithOption` / `MessageOption` | 抽象 method をやめ、`CommonCommandContext` の final 実装 1 つにまとめる（中身は `sendMessage(RichText.text(msg).color(rgb).hover(...))`）。hover text が空なら hover を付けない（問題点 4 の修正）。呼び出し側は source / binary とも互換。存続させるかは Open question。 |
| `sendComponent(C)`、Spigot の `send*(BaseComponent)`、modded の `sendMessage(Component[, boolean])` | 残す。native 機能（item hover、MiniMessage 等）の逃げ道として必要。 |
| `String` を受け取る `send*` と、help / error の `§` 文字列 | Phase 1 では変えない。rich help は formatter API（Pillar 6）と一緒に扱う。 |

## テスト方針

- `common:test`: `RichText` の unit test（immutability、`equals` / `hashCode`、`plainText`、`colorIfAbsent`、
  `ClickAction` の factory）。
- 各 platform の `test`: renderer の unit test。既存の `spigot/src/test/.../ComponentBuilderTest` を
  `RichTextsTest` に置き換え、paper / modded にも同じ観点のものを足す（色、named color への戻し、装飾の tri-state、
  hover、click 4 種、translatable 引数、子要素）。
- `*-testing` の `CommandContextTest`: `sendMessageWithOption` の既存 test はそのまま通るはず。空の hover を付けない
  test と、`sendMessage(RichText)` を `FakeSender#getSentMessages()` の native component で assert する test を足す。
- `common-testing`: `TestCommandContext#sendMessage(RichText)` は、今の `TestTextComponentBuilder` と同じ legacy 文字列
  （色は `§x§r§r§g§g§b§b` を前置、translatable は key）を `messages()` に積む。加えて
  `List<RichText> richMessages()` を公開し、利用者が `assertThat(ctx.richMessages()).containsExactly(RichText.text(...)...)`
  と値で比較できるようにする。既存の `ArgumentErrorMessageTest` /
  `CommandTesterTest#message_colors_are_kept_in_legacy_texts` の期待値は変えない。
- integration test は追加しない。描画は native component の生成までを unit test で保証し、client への送信は
  platform の責務とする。

## 段階的導入

1. **Phase 1（CommandLib 内で完結）**: `RichText` / `Decoration` / `ClickAction`、`sendMessage/Success/Warn/Failure(RichText)`、
   3 platform の renderer と common-testing の対応。`ArgumentParseException` を移行し、`sendMessageWithOption` を
   common の実装 1 つにまとめる（空 hover の修正）。builder 群に `@Deprecated` を付ける。
2. **Phase 2（ConfigLib）**: ConfigLib の 5 箇所を `RichText` に移行し、必要なら click（値の copy、`set` の suggest）を付ける。
   package rename の release で deprecated API を削除する。
3. **Phase 3（需要があれば）**: rich help（usage 行の click で入力欄に挿入、description の hover）を formatter API
   （Pillar 6）と合わせて設計する。suggestion tooltip（現在は Brigadier `Message`）を `RichText` で書けるようにする。
   `insertion`、translatable の fallback（Paper と vanilla 1.19.4+ のみ。Bungee API 1.16.5 にはない）は利用者の要望が
   出てから追加する。

## Open questions

1. 型名は `RichText` でよいか（`Text` は Fabric yarn / Bungee の hover と、`Message` は Brigadier と衝突する）。
2. builder 群（`ComponentBuilder` 系、各 `*Impl`、`PlatformAdapter#create*Builder`）の扱い。案 A: Phase 1 で deprecate し、
   package rename と同じ release で削除する（推奨）。案 B: rename の前に deprecation を経ずに削除する。
3. `sendMessageWithOption` / `MessageOption` を便利 method として残し続けるか、`RichText` があるので deprecate して
   rename の release で削除するか。ConfigLib 以外の利用者は不明。
4. 装飾 API は enum（`decorate(Decoration.BOLD)`）だけでよいか、`bold()` などの短縮 method も足すか
   （足すと 3 renderer に影響はないが、public method が 5〜10 個増える）。
5. Phase 1 の範囲に `insertion` と translatable の fallback を入れるか（推奨: 入れない）。
6. Spigot との parity のため、Paper / modded にも native component を受け取る `sendSuccess/Warn/Failure(Component)` を
   足すか（`RichText` があれば不要とも言える）。
