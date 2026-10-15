# Localization

CommandLib's own messages, such as the permission error, the option errors, the cooldown and confirmation messages and
the headings of the generated help, are **captions**: each has a stable key and an English default, and is sent in the
sender's locale. A plugin can translate them, replace them per locale, load them from properties files, and change how
they are sent. It can use the same mechanism for its own messages.

## Captions

A `Caption` is a key with an English default. CommandLib's own captions are the constants of `Caption`; make your own
with `Caption.of`:

```java
public static final Caption HOME_SET = Caption.of("myplugin.home.set", "Home {name} set.");
```

A text may hold placeholders, `{name}`, which are replaced by name. A brace that does not name a given placeholder stays
as it is; there is no escaping and no other syntax.

Send a caption from any context; it is resolved in the sender's locale:

```java
execute(ctx -> ctx.sendSuccess(HOME_SET, Map.of("name", "base")));
```

| Method | Sends like |
|---|---|
| `ctx.sendMessage(caption[, placeholders])` | `sendMessage(RichText)` |
| `ctx.sendSuccess(caption[, placeholders])` | `sendSuccess(RichText)`, green |
| `ctx.sendWarn(caption[, placeholders])` | `sendWarn(RichText)`, yellow |
| `ctx.sendFailure(caption[, placeholders])` | `sendFailure(RichText)`, red |
| `ctx.caption(caption[, placeholders])` | Returns the text instead of sending it |
| `new CommandPrerequisiteException(caption[, placeholders])` | Fails the command with the caption |
| `new ArgumentParseException(caption, placeholders)`, `new ArgumentValidationException(caption, placeholders)` | Rejects the argument with the caption |
| `new OptionParseException(caption, placeholders)` | Rejects the options with the caption |

## The sender's locale

`ctx.getLocale()` decides the language: the client language of a player (Spigot / Paper `Player#getLocale`, Velocity
`Player#getEffectiveLocale`, Forge / NeoForge `ServerPlayer#getLanguage`), and the JVM's default locale for the console,
command blocks and other senders. A player whose client has not sent its settings yet gets the JVM default too.

For a locale such as `ja_JP`, the text of a caption is looked up for `ja_JP`, then `ja`, then the root locale. At each
step the texts given to `set` and `load` come first, then the bundles added with `bundle(...)` from the last added, then
CommandLib's own translations. Without any, the caption's English default is used. A more specific locale wins over a
less specific one, so a text set for `Locale.ROOT` replaces the English default but not CommandLib's Japanese text for a
Japanese player; set it for `Locale.JAPANESE` too to replace that.

## Configuring captions

`Captions` holds the texts and how they are sent. Give one to a top-level command; its argument branches and child
commands use it unless they set their own. A command without one uses `Captions.create()`.

```java
Captions captions = Captions.create()
                            // Replaces a text for every locale without a text of its own.
                            .set(Caption.PERMISSION_DENIED, "You can't do that.")
                            // Replaces it for Japanese senders.
                            .set(Locale.JAPANESE, Caption.PERMISSION_DENIED, "それはできません。")
                            // lang/messages.properties, lang/messages_ja.properties, ... in the plugin jar.
                            .bundle("lang/messages", getClass().getClassLoader());

public final class HomeCommand extends Command {
    public HomeCommand(Captions captions) {
        super("home");
        captions(captions);
        // ...
    }
}
```

- `create()` starts with CommandLib's English and Japanese texts.
- `set(caption, text)` sets the text for the root locale, `set(locale, caption, text)` for one locale.
- `load(locale, resourceBundle)` sets every text of a `ResourceBundle`, keyed by caption key. For a file the server
  owner edits, such as one in the plugin's data folder:
  `load(Locale.JAPANESE, new PropertyResourceBundle(Files.newBufferedReader(dataFolder.resolve("messages_ja.properties"))))`.
- `bundle(baseName, classLoader)` adds the properties files `baseName.properties`, `baseName_ja.properties`,
  `baseName_ja_JP.properties` and so on that the class loader finds. They are read as UTF-8, each the first time a
  sender of its locale needs it. Unlike `ResourceBundle.getBundle`, it never falls back to the server's default locale
  and works inside Forge / NeoForge's module layer.
- `text(locale, caption)` and `format(locale, caption, placeholders)` resolve a text without a context.

Set the captions up before the commands are registered; lookups may then run on any thread.

A bundle is a properties file of caption keys:

```properties
# lang/messages_ja.properties
commandlib.permission.denied=権限がありません。
myplugin.home.set=ホーム {name} を設定しました。
```

## Formatting captions

`Captions#sender(CaptionSender)` replaces how each caption is sent. It gets the context, the caption, the message as a
`RichText` (the text with its placeholders replaced, and for the confirmation request the `[Confirm]` button after it)
and `send`, which sends a message the way CommandLib does for that caption, such as in red for a failure:

```java
// A prefix before every caption.
captions.sender((ctx, caption, message, send) -> send.accept(RichText.text("[Homes] ")
                                                                     .color(0xFFAA00)
                                                                     .append(message)));

// MiniMessage texts on Paper, except the confirmation, which carries a button.
captions.sender((ctx, caption, message, send) -> {
    if (caption.equals(Caption.CONFIRMATION_REQUEST)) {
        send.accept(message);
        return;
    }
    ((CommandContext) ctx).sendFailure(MiniMessage.miniMessage()
                                                  .deserialize(message.plainText()));
});
```

What the sender sends while a command fails is part of the failure, as with `ctx.sendFailure`: a silent source does not
get it and `/execute store success` stores 0.

The generated help is not sent through the `CaptionSender`: its headings and notes (`Usage:`, `Options:`, `options`,
`required`, `up to {count} times`) are captions, but each line combines several of them with the command's usage.

## Vanilla translation keys

Where vanilla has a translation key that means the same thing, CommandLib sends a translatable text, which every client
translates into its own language, instead of a caption:

| Message | Vanilla key |
|---|---|
| No player matched an `@a` / `@r` of `OfflinePlayersArgument`, `UUIDsArgument` | `argument.entity.notfound.player` ("No player was found") |
| Unknown advancement of `AdvancementArgument` | `advancement.advancementNotFound` ("Unknown advancement: %s") |
| Unknown recipe of `RecipeArgument` | `recipe.notFound` ("Unknown recipe: %s") |
| Unknown objective of `ObjectiveArgument` | `arguments.objective.notFound` ("Unknown scoreboard objective '%s'") |
| "Incorrect argument for command" and "<--[HERE]" of `ArgumentParseException.ofIncorrectInput` | `command.unknown.argument`, `command.context.here` |

These are not captions, so `Captions` cannot change them; reject the value in a validator with your own caption to
replace one. `Caption.UNEXPECTED_ERROR` is both: unless a text is set for it, from `set`, `load` or `bundle`, it is
sent as vanilla's `command.failed` ("An unexpected error occurred trying to execute that command"), so it is translated
for every language. CommandLib's Japanese bundle leaves it out for that reason.

The rest of CommandLib's messages have no vanilla equivalent, such as the permission message (Bukkit's own permission
message is not translatable either) and the option errors, so they are captions.

## Built-in captions

| Constant | Key | English |
|---|---|---|
| `PERMISSION_DENIED` | `commandlib.permission.denied` | You do not have permission to execute this command. |
| `REQUIRE_PLAYER` | `commandlib.prerequisite.player` | This command can only be executed by a player. |
| `REQUIRE_CONSOLE` | `commandlib.prerequisite.console` | This command can only be executed from the console. |
| `INVALID_VALUE` | `commandlib.argument.invalid_value` | Invalid value. |
| `INVALID_KEY` | `commandlib.argument.invalid_key` | {value} is not a valid namespaced key |
| `UNKNOWN_PLAYER_OR_UUID` | `commandlib.argument.unknown_player_or_uuid` | {value} is neither a known player nor a valid UUID |
| `INVALID_SELECTOR` | `commandlib.argument.invalid_selector` | {value} is not a valid selector |
| `UNKNOWN_LOOT_TABLE` | `commandlib.argument.unknown_loot_table` | {value} is not a known loot table |
| `UNKNOWN_ATTRIBUTE` | `commandlib.argument.unknown_attribute` | {value} is not a known attribute |
| `UNKNOWN_BIOME` | `commandlib.argument.unknown_biome` | {value} is not a known biome |
| `UNKNOWN_ENTITY_TYPE` | `commandlib.argument.unknown_entity_type` | {value} is not a known entity type |
| `UNKNOWN_SOUND` | `commandlib.argument.unknown_sound` | {value} is not a known sound |
| `OPTION_MISSING` | `commandlib.option.missing` | Missing required option {option}. |
| `OPTION_REQUIRES` | `commandlib.option.requires` | {option} requires {required}. |
| `OPTION_REQUIRES_VALUE` | `commandlib.option.requires_value` | {option} requires {required} to be {value}. |
| `COOLDOWN` | `commandlib.cooldown` | You must wait {remaining} before using this command again. |
| `CONFIRMATION_REQUEST` | `commandlib.confirmation.request` | Run the command again within {timeout} to confirm. |
| `CONFIRMATION_BUTTON` | `commandlib.confirmation.button` | [Confirm] |
| `DURATION_HOURS` | `commandlib.duration.hours` | {value}h |
| `DURATION_MINUTES` | `commandlib.duration.minutes` | {value}m |
| `DURATION_SECONDS` | `commandlib.duration.seconds` | {value}s |
| `TIMEOUT` | `commandlib.execution.timeout` | The command timed out. |
| `UNEXPECTED_ERROR` | `commandlib.execution.unexpected_error` | An unexpected error occurred trying to execute that command. (vanilla `command.failed` unless set) |
| `SEE_CONSOLE` | `commandlib.execution.see_console` | Check the console for details. |
| `HELP_USAGE` | `commandlib.help.usage` | Usage: |
| `HELP_OPTIONS` | `commandlib.help.options` | Options: |
| `HELP_OPTIONS_TAG` | `commandlib.help.options_tag` | options |
| `HELP_REQUIRED` | `commandlib.help.required` | required |
| `HELP_MAX_OCCURRENCES` | `commandlib.help.max_occurrences` | up to {count} times |

The durations join their parts with a space, such as `1h 2m 3s` or `1時間 2分 3秒`. The English and Japanese files are
`dev/marulabs/commandlib/lang/commandlib_en.properties` and `commandlib_ja.properties` in the `common` jar; copy the
English one to start a translation, and add it to your plugin with `bundle(...)`. `Caption.builtIns()` lists the
constants.

`Cooldown#message` and `Confirmation#message` still replace their whole message for one cooldown or confirmation.

## In tests

The testers resolve captions like a server: set a player's locale with `FakeSender#locale(...)` (`"ja_jp"` in
`spigot-testing`, `paper-testing` and `modded-testing`, a `Locale` in `velocity-testing`). A console sender uses the JVM's
default locale, so pin it, for example with `systemProperty("user.language", "en")` on the Gradle `Test` task, when the
tests assert English texts on machines with another language.

```java
try (CommandTester tester = new CommandTester(new HomeCommand(captions), "myplugin.command")) {
    FakeSender sender = FakeSender.player("Steve")
                                  .locale("ja_jp");
    tester.execute("home set base", sender);
    assertThat(sender.getSentMessageTexts()).containsExactly("ホーム base を設定しました。");
}
```
