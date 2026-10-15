# Rich Text Messages

This document describes how CommandLib's message API (formerly `TextComponentBuilder` and friends,
`sendMessageWithOption`, and `sendComponent`) was rebuilt as an official rich text API that works on every platform.

Status: Phase 1 is implemented. The decisions taken on the former open questions are listed in
[Decisions](#decisions). Package names use `dev.marulabs.commandlib`.

## Background

`TextComponentBuilder` / `TranslatableComponentBuilder` were internal parts written so that `ArgumentParseException` in
`common` could build a component for each platform. The classes and the `PlatformAdapter` factory methods were public
anyway, and ConfigLib used `sendMessageWithOption`. Phase 1 replaced them with a designed API.

Related roadmap items:

- P5 "design the sender / message abstraction (bridging Adventure and vanilla `Component`) before starting Velocity".
- Pillar 6 Stretch "MiniMessage / Adventure component support" and "rich help with hover / click".
- The open question in `docs/compatibility.md` / `docs/design/module-structure.md`: "should the message API standardize
  on Adventure or put a CommandLib abstraction in between?" Answer: a small CommandLib value type for code that spans
  platforms, plus native components for platform-specific code.

## State before Phase 1

### Public API

| Type / method | Module | Content |
|---|---|---|
| `util.text.ComponentBuilder<B, E, T>` | common | Abstract builder with only `color(int rgb)`, `italic()`, `append(B)`, `build()`. |
| `util.text.TextComponentBuilder` / `TranslatableComponentBuilder` | common | Empty abstract subclasses. Translatable arguments (`with`) could not be passed. |
| `util.text.TextComponentBuilderImpl` / `TranslatableComponentBuilderImpl` | spigot / paper / modded | Implementations for Bungee `BaseComponent` / Adventure `Component` / vanilla `Component`. |
| `PlatformAdapter#createTextComponentBuilder(String)` / `#createTranslatableComponentBuilder(String)` | common (interface) | Returned `TextComponentBuilder<T, ? extends T, ?>`. |
| `CommonCommandContext#sendMessageWithOption(String / Object, Consumer<MessageOption>)` | common (abstract) + each platform | Sent one string with an RGB color and a hover text. |
| `CommonCommandContext.MessageOption` | common | Only `rgb(int)` and `hoverText(String)`. |
| `CommonCommandContext#sendComponent(C)` | common (abstract) | Sends a native platform component as is. |
| `sendMessage/sendSuccess/sendWarn/sendFailure(BaseComponent)` | spigot only | Paper and modded had no `sendSuccess(Component)` etc. (modded had only `sendMessage(Component)`). |
| `TestTextComponentBuilder` / `TestTranslatableComponentBuilder` | common-testing (package-private), with a copy in common/src/test | Test implementations using `String` as the component. |

### Problems

1. Code in `common` could not send a rich message type-safely. The builders had the platform component type as a type
   parameter, so code holding a `CommonCommandContext<?, ?>` (CommandLib's exceptions, ConfigLib) needed raw types.
   ConfigLib could not use the builders at all: `build()` returned a capture type that another
   `CommonCommandContext<?, ?>`'s `sendComponent` did not accept.
2. Missing features: decorations such as bold, click, translatable arguments, rich hover text.
3. Behavior differed by platform. The Paper builder wrapped immutable Adventure components while Spigot and modded
   mutated their components in place, and `build()` returned the same instance, leaving aliases. Spigot's
   `sendSuccess(BaseComponent)` overwrote the color of the passed component.
4. `sendMessageWithOption` always attached `SHOW_TEXT`, even for an empty hover text (on all three platforms). ConfigLib
   passes `""` for entries without a description, which showed an empty tooltip.
5. The same building code was duplicated across three platforms and two test contexts.

ConfigLib (`../ConfigLib`) calls `sendMessageWithOption(text, o -> o.rgb(ChatColorUtil.GREEN.getRGB()).hoverText(description))`
from `ConfigListCommand`, `ConfigFieldCommand` (3 places), and `ConfigHistoryCommand`, on a `CommonCommandContext<?, ?>`.
It does not use the builders or `sendComponent`. Guess (not in ConfigLib's roadmap): it will later want click-to-copy
values, click to insert `/config <field> set `, and multi-line hover (history preview), but not item hover or
MiniMessage.

## Goals

- One platform-independent, immutable rich text value type in `common` that any platform sends with
  `ctx.sendMessage(text)`.
- Render what command output needs (color, decorations, hover text, click, translatable arguments, children) the same
  way on all three platforms.
- Value semantics (`equals` / `hashCode` / readable `toString`) so tests can assert what was sent.
- ConfigLib migrates each call in one line.

## Non-goals

- **No Adventure re-implementation.** Paper-only plugins should use Adventure / MiniMessage directly and send with
  `sendComponent(Component)` or the native `sendMessage/sendSuccess/...(Component)` overloads; the README says so.
  `RichText` is for code that spans platforms (CommandLib itself, the common layer of libraries such as ConfigLib).
- Adventure is not shaded into Spigot / modded (it would need `adventure-platform-*` and conflict with relocation).
- No MiniMessage or legacy `§` parser / serializer. Existing strings with `§` codes are sent as text (rendered by the
  client, as before).
- No item / entity hover. `ItemStack` / `Entity` are platform types; use native components for them.
- No keybind / score / selector / nbt components, font, shadow color (1.21.4+), or `change_page` (books only).
- No insertion and no translatable fallback (see [Phases](#phases)).
- No JSON serializer, no mutable builder, no color class (colors are `int rgb`).

## API

### Value type `RichText` (common, `dev.marulabs.commandlib.util.text`)

```java
public abstract class RichText {   // package-private constructor: the kinds are the two below
    // Creation
    public static TextRichText text(@NotNull String content);
    public static TranslatableRichText translatable(@NotNull String key, @NotNull RichText... args);

    // Every change returns a new instance (immutable)
    public RichText color(int rgb);                 // 0xRRGGBB; the 16 legacy colors are sent as named colors
    public RichText bold();                         // = bold(true)
    public RichText bold(boolean bold);             // false cancels a decoration inherited from the parent
    public RichText italic();          public RichText italic(boolean italic);
    public RichText underlined();      public RichText underlined(boolean underlined);
    public RichText strikethrough();   public RichText strikethrough(boolean strikethrough);
    public RichText obfuscated();      public RichText obfuscated(boolean obfuscated);
    public RichText hover(@Nullable RichText hover); // null or an empty plain text removes it
    public RichText click(@Nullable ClickAction click); // null removes it
    public RichText append(@NotNull RichText... children);

    // Reading (platform converters and tests)
    public @NotNull List<RichText> children();
    public @Nullable Integer color();
    public @Nullable Boolean isBold();          // null = not set (inherited); same for the other four
    public @Nullable Boolean isItalic();
    public @Nullable Boolean isUnderlined();
    public @Nullable Boolean isStrikethrough();
    public @Nullable Boolean isObfuscated();
    public @Nullable RichText hover();
    public @Nullable ClickAction click();

    /** The texts joined. A translatable contributes its key and not its arguments (like Paper's plain serializer). */
    public @NotNull String plainText();

    // equals / hashCode / toString are value based
}

public final class TextRichText extends RichText {
    public @NotNull String content();
}

public final class TranslatableRichText extends RichText {
    public @NotNull String key();
    public @NotNull List<RichText> arguments();
}

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

Each kind of text is its own class, as Adventure's `TextComponent` and `TranslatableComponent`, so a literal has no
translation key or arguments to leave null and a converter picks the native component by type. The style methods return
`RichText`, so they need no self type, and the style lives in one package-private value (`TextStyle`) that every kind
shares. The constructor is package-private because each converter knows exactly these kinds.

Decorations use shortcut methods instead of a `Decoration` enum. `bold()` sets `true`, `bold(false)` keeps an explicit
`false` apart from "not set", so tri-state is representable. The getters are `isBold()` etc. returning `@Nullable Boolean`
because `bold()` is taken by the setter.

`hover(...)` drops a hover whose `plainText()` is empty, so `hover(RichText.text(description))` with an empty
description shows no tooltip (problem 4).

The name is not `Text` because that clashes with Fabric's (yarn mappings) `net.minecraft.text.Text` and Bungee's
`hover.content.Text`. `Message` clashes with Brigadier's `com.mojang.brigadier.Message`.

### Sending (`CommonCommandContext`)

```java
public abstract class CommonCommandContext<S, C> {
    public abstract void sendMessage(@NotNull RichText text);   // each platform converts and sends it

    public void sendSuccess(@NotNull RichText text);   // green unless the root has its own color
    public void sendWarn(@NotNull RichText text);      // yellow unless the root has its own color
    public void sendFailure(@NotNull RichText text);   // red unless the root has its own color
}
```

- The default color is applied to a copy only when the root has no color; the passed value is never changed.
- modded overrides `sendFailure(RichText)` and routes it through `CommandSourceStack#sendFailure`, like
  `sendFailure(String)`.
- These overloads sit next to the `sendMessage(Object)` / `sendMessage(String)` overloads. A call with a literal `null`
  is now ambiguous and fails to compile (none in this repository).

Native component overloads, all of which leave the passed component unchanged and apply the default color only when
it has none:

| Platform | Overloads |
|---|---|
| spigot | `sendMessage/sendSuccess/sendWarn/sendFailure(BaseComponent)` (existed; no longer overwrite or mutate the color) |
| paper | `sendMessage/sendSuccess/sendWarn/sendFailure(Component)` (new, Adventure) |
| modded | `sendMessage/sendSuccess/sendWarn/sendFailure(Component[, boolean allowLogging])` (new except `sendMessage`, vanilla) |

Example (ConfigLib's list):

```java
final class ConfigListCommand {
    static void sendEntry(CommonCommandContext<?, ?> ctx, ConfigSchemaEntry<?> entry, String value, String description) {
        ctx.sendMessage(RichText.text(entry.entryName() + ": " + value)
                                .color(ChatColorUtil.GREEN.getRGB())
                                .hover(RichText.text(description))
                                .click(ClickAction.suggestCommand("/config " + entry.entryName() + " set ")));
    }
}
```

### Platform conversion

Each platform has `util.text.RichTexts` (static utility):

- spigot: `BaseComponent toBungee(RichText)`
- paper: `Component toAdventure(RichText)`
- modded: `MutableComponent toVanilla(RichText)`

`CommandContext#sendMessage(RichText)` uses it. Users can convert with it too and add native features (item hover etc.)
afterwards. Each call returns a new component.

## Platform implementation

Each converter walks the `RichText` tree and builds native components. A color that exactly matches one of the 16
legacy colors becomes a named color (Bungee `ChatColor.GREEN`, `NamedTextColor.namedColor(rgb)`, `ChatFormatting`), so
console output and color comparisons in tests stay as before.

| Feature | Spigot (Bungee chat; compiled against API 1.16.5, runs on 1.16.4 to 1.21.1) | Paper (Adventure, 1.20.6+) | modded (vanilla: forge 1.16.5 / forge 1.20.1 / neoforge 1.21.1) |
|---|---|---|---|
| literal | `new TextComponent(s)` | `Component.text(s)` | <1.19 `new TextComponent(s)` / 1.19+ `Component.literal(s)` |
| translatable + args | `new TranslatableComponent(key, (Object[]) args)` | `Component.translatable(key, args)` | <1.19 `new TranslatableComponent(key, args)` / 1.19+ `Component.translatable(key, args)` |
| hex color | `ChatColor.of(new Color(rgb))` | `TextColor.color(rgb)` | `TextColor.fromRgb(rgb)` |
| decorations (tri-state) | `setBold(Boolean)` etc. | `decoration(d, TextDecoration.State.byBoolean(b))` | `Style.withBold(Boolean)` etc. On 1.16.5 underlined / strikethrough / obfuscated have no `Boolean` setter, so `true` uses `applyFormat(ChatFormatting.X)` and an explicit `false` is dropped |
| hover text | `new HoverEvent(SHOW_TEXT, new Text(new BaseComponent[]{c}))` | `HoverEvent.showText(c)` | `new HoverEvent(HoverEvent.Action.SHOW_TEXT, c)` (1.21.5+ changes to the `HoverEvent.ShowText` record) |
| click | `new ClickEvent(ClickEvent.Action.X, value)` | `ClickEvent.clickEvent(Action.X, value)` | `new ClickEvent(ClickEvent.Action.X, value)` (1.21.5+ changes to records such as `ClickEvent.RunCommand`, and `open_url` takes a `URI`) |
| children | `addExtra(c)` | `children(list)` | `append(c)` |
| plain text (tests) | `toPlainText()` | `PlainTextComponentSerializer` | `getString()` |

Notes:

- The oldest targets are 1.16.4 / 1.16.5, so hex colors, `copy_to_clipboard` (1.15+), and the hover content API
  (1.16+) are available everywhere. Nothing is downsampled.
- The network format changes of text components after 1.20.3 (NBT, snake_case in 1.21.5) are handled by the server and
  do not affect the API. Only modded's Java API changes in 1.21.5+ (table above); handle it with Stonecutter conditions
  when such a node is added. All current targets are below 1.21.5.
- `velocity` has a copy of paper's `RichTexts.toAdventure`; the modules share no source. Both build click events
  with the `ClickEvent` factory methods, which link against Adventure 4 and Adventure 5 (Paper 26.x, Velocity 4.x),
  where `ClickEvent.clickEvent(Action, String)` no longer exists.

## Removed APIs

API breaks were accepted, so the replaced APIs were removed instead of deprecated:

| Removed | Replacement |
|---|---|
| `ComponentBuilder`, `TextComponentBuilder`, `TranslatableComponentBuilder` (common) and every `*ComponentBuilderImpl` (spigot / paper / modded) | `RichText` + `RichTexts.toBungee/toAdventure/toVanilla` |
| `PlatformAdapter#createTextComponentBuilder` / `#createTranslatableComponentBuilder` | none needed (`ArgumentParseException` uses `RichText`) |
| `TestTextComponentBuilder` / `TestTranslatableComponentBuilder` (common-testing, common/src/test) | `TestCommandContext#sendMessage(RichText)` |
| `CommonCommandContext#sendMessageWithOption(String / Object, Consumer<MessageOption>)`, `MessageOption` | `sendMessage(RichText.text(msg).color(rgb).hover(RichText.text(hover)))` |

Kept: `sendComponent(C)`, Spigot's `send*(BaseComponent)`, modded's `sendMessage(Component[, boolean])`, as the escape
hatch for native features (item hover, MiniMessage, ...). `String` `send*` methods and the `§` strings of help and
errors are unchanged; rich help belongs to the formatter API (Pillar 6).

`ArgumentParseException` is rewritten on `RichText` without raw types. Its output is the same: two translatables
(`command.unknown.argument`, `command.context.here`) and a text with `§` codes. On Spigot the red is now the named
`ChatColor.RED` instead of the hex `#FF5555`.

### Migration (ConfigLib and other users)

```java
final class Migration {
    void send(CommonCommandContext<?, ?> ctx, String text, String description) {
        // Before (removed API)
        // ctx.sendMessageWithOption(text, o -> o.rgb(ChatColorUtil.GREEN.getRGB()).hoverText(description));
        // After (an empty description shows no tooltip)
        ctx.sendMessage(RichText.text(text).color(ChatColorUtil.GREEN.getRGB()).hover(RichText.text(description)));
    }
}
```

`sendMessageWithOption` defaulted to white (`0xFFFFFF`) without `rgb(...)`; `RichText` without `color(...)` inherits the
client default, which is white for chat. Code that used the builders through `PlatformAdapter` builds a `RichText` and
calls `sendMessage(RichText)`.

## Testing

- `common:test`: `RichTextTest` (immutability, tri-state decorations, `equals` / `hashCode` / `toString`, `plainText`,
  empty hover removal, `ClickAction` factories).
- Converter tests per platform: `spigot` and `paper` have `util.text.RichTextsTest`; modded's is in `modded-testing`
  (the `modded` module has no test setup). They cover colors and named-color mapping, tri-state decorations, hover,
  empty hover, the four click actions, translatable arguments, and children.
- `*-testing`: `CommandContextTest` (spigot / paper) and `CommandTesterTest` (modded) send `RichText` and assert the
  native components in `FakeSender#getSentMessages()`, including the default colors, the empty hover, and that the
  native `sendSuccess/Warn/Failure` overloads do not change the passed component.
- `common-testing`: `TestCommandContext#sendMessage(RichText)` renders a legacy string like Bungee's `toLegacyText`
  (named colors as `§a` etc., other colors as `§x§r§r§g§g§b§b`, decorations as `§l§o§n§m§k`, translatables as their
  key, hover and click dropped) into `messages()`. `List<RichText> richMessages()` records every message as a value
  (`String` messages as `RichText.text(message)` with the send method's color), so users can write
  `assertThat(ctx.richMessages()).containsExactly(RichText.text(...)...)`. The existing expectations of
  `ArgumentErrorMessageTest` and `CommandTesterTest#message_colors_are_kept_in_legacy_texts` are unchanged.
- No integration tests: unit tests cover building native components; delivering them to the client is the platform's
  job.

## Phases

1. **Phase 1 (done, inside CommandLib)**: `RichText` / `ClickAction`, `sendMessage/Success/Warn/Failure(RichText)`,
   the three platform converters, common-testing support, native `sendSuccess/Warn/Failure` on Paper and modded.
   `ArgumentParseException` migrated. Builders and `sendMessageWithOption` removed. Empty hover fixed.
2. **Phase 2 (ConfigLib)**: migrate ConfigLib's five call sites to `RichText` and, if useful, add click (copy a value,
   suggest `set`).
3. **Phase 3 (on demand)**: rich help (click on a usage line inserts it, hover shows the description) together with the
   formatter API (Pillar 6). Suggestion tooltips (currently Brigadier `Message`) written as `RichText`. `insertion` and
   translatable fallback (Paper and vanilla 1.19.4+ only; Bungee API 1.16.5 has none), item / entity hover,
   keybind / score / selector, MiniMessage, and a legacy parser only when users ask for them.

## Decisions

1. Name: `RichText`.
2. Builders (`ComponentBuilder` family, every `*Impl`, `PlatformAdapter#create*Builder`): removed without deprecation.
3. `sendMessageWithOption` / `MessageOption`: removed; `RichText` replaces them and ConfigLib migrates.
4. Decoration API: shortcut methods `bold()` / `bold(boolean)` etc. with `@Nullable Boolean isBold()` getters; no
   `Decoration` enum.
5. `insertion` and translatable fallback: not in Phase 1.
6. Paper / modded get native-component `sendSuccess/Warn/Failure` overloads for parity with Spigot. None of the three
   platforms mutates the passed component; Spigot's overwrite of the color was fixed.
