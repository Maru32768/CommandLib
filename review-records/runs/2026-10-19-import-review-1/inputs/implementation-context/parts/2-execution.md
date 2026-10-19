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
