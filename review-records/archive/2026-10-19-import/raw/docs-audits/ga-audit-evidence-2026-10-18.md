# GA 監査の再現資料

[監査レポート](ga-architecture-api-audit-2026-10-18.md)の補足。対象は同レポートに記載した作業ツリー。以下は実際に実行した監査専用プログラムと採取した出力であり、製品コードや既存テストには追加していない。

## 実行環境と再現方法

Windows / PowerShell / Java 21。既存 Gradle cache を使用。各プログラムは同じ package の既存 test fixture を利用する。platform の同名 class が衝突するため、各 probe は別 JVM で実行する。Mockito の実行に必要な JVM attachment は実行環境に依存する。

下記の各 Java コードと init script をファイル名どおり `.local/ga-audit-2026-10-18` に保存する。ディレクトリは `New-Item -ItemType Directory -Force .local/ga-audit-2026-10-18` で作成できる。Gradle cache パスは環境に合わせて変更する。依存が cache にない環境では `--offline` を外して依存を解決する必要がある。

```powershell
.\gradlew.bat -g C:/Users/maru3/.gradle '-Pcommandlib.typedNms=false' --offline -I .local/ga-audit-2026-10-18/classpaths.init.gradle :common:test :paper-testing:test :velocity-testing:test :spigot-testing:test :common-testing:test :common:gaAuditClasspath :paper-testing:gaAuditClasspath :velocity-testing:gaAuditClasspath :spigot-testing:gaAuditClasspath

$auditDir = Join-Path (Get-Location) '.local/ga-audit-2026-10-18'
New-Item -ItemType Directory -Force "$auditDir/common-classes", "$auditDir/paper-classes", "$auditDir/velocity-classes"

$cp = Get-Content -LiteralPath "$auditDir/common-classpath.txt"
javac -proc:none -cp $cp -d "$auditDir/common-classes" "$auditDir/AuditProbe.java"
java -cp "$auditDir/common-classes;$cp" dev.marulabs.commandlib.AuditProbe

$cp = Get-Content -LiteralPath "$auditDir/paper-testing-classpath.txt"
javac -proc:none -cp $cp -d "$auditDir/paper-classes" "$auditDir/PaperAuditProbe.java" "$auditDir/PermissionAuditProbe.java"
java '-XX:+EnableDynamicAgentLoading' -cp "$auditDir/paper-classes;$cp" dev.marulabs.commandlib.PaperAuditProbe

$spigotCp = Get-Content -LiteralPath "$auditDir/spigot-testing-classpath.txt"
java '-XX:+EnableDynamicAgentLoading' -cp "$auditDir/paper-classes;paper/build/classes/java/main;$spigotCp" dev.marulabs.commandlib.PermissionAuditProbe

$cp = Get-Content -LiteralPath "$auditDir/velocity-testing-classpath.txt"
javac -proc:none -cp $cp -d "$auditDir/velocity-classes" "$auditDir/VelocityAuditProbe.java"
java '-XX:+EnableDynamicAgentLoading' -cp "$auditDir/velocity-classes;$cp" dev.marulabs.commandlib.VelocityAuditProbe
```

common の採取時は上記 Gradle 出力を利用する代わりに、common 本体全 Java ソースと TestCommand / TestCommandContext / TestCommandSource / TestCommandRunner / PlatformAdapterImpl を監査用ディレクトリへ fresh compile して実行した。上の手順は同じコードを Gradle test runtime classpath で実行できるようにまとめたもの。

PermissionAuditProbe は Paper の `CommandPermissions` helper と Spigot API の実 `SimplePluginManager` を同じ JVM に載せ、Bukkit の permission registry の名前による削除を検証している。実サーバーでの plugin lifecycle 全体を再現したものではない。PaperAuditProbe / VelocityAuditProbe も既存 mock fixture を使っており、この結果を実サーバー integration の成功として扱っていない。

終了コードのみで判定しないこと。AuditProbe は例外と StackOverflowError を意図的に捕捉し、各シナリオを継続する。判定に使う行と期待値は監査レポートの各 F 項目に記載した。出力には JVM warning、時刻、オブジェクトの identity hash、標準出力と標準エラーの順序など環境依存の部分がある。

## classpaths.init.gradle

```groovy
allprojects {
    afterEvaluate {
        if (project.path in [':paper-testing', ':velocity-testing', ':common', ':spigot-testing']) {
            tasks.register('gaAuditClasspath') {
                doLast {
                    rootProject.file('.local/ga-audit-2026-10-18/' + project.name + '-classpath.txt').text = sourceSets.test.runtimeClasspath.asPath
                }
            }
        }
    }
}

```

## AuditProbe.java

```java
package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import java.util.List;

public class AuditProbe {
    static class Str extends CommonStringArgument<TestCommandContext, Str> {
        Str(String name) { super(name); }
    }
    static class Int extends CommonIntegerArgument<TestCommandContext, Int> {
        Int(String name) { super(name); }
    }
    interface Probe { void run() throws Exception; }
    static void probe(String name, Probe probe) {
        try { probe.run(); }
        catch (Throwable e) { System.out.println(name + " => " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
    }
    public static void main(String[] args) {
        probe("nested same name", () -> {
            TestCommand root = new TestCommand("root");
            root.argument(new Str("value")).child(parent -> {
                TestCommand child = new TestCommand("set");
                child.argument(new Str("value")).execute((value, ctx) -> ctx.sendMessage("parent=" + parent.get() + ",child=" + value));
                return child;
            });
            System.out.println("nested same name => " + new TestCommandRunner(root).execute("root A set B").messages());
        });
        probe("optional matches command", () -> {
            TestCommand root = new TestCommand("count");
            root.argument(new Int("count").optional(7)).execute((count, ctx) -> ctx.sendMessage(count));
            System.out.println("optional matches command => " + new TestCommandRunner(root).execute("count").messages());
        });
        probe("cycle", () -> {
            TestCommand root = new TestCommand("loop");
            root.addChildren(root);
            System.out.println("cycle accepted by addChildren");
            new TestCommandRunner(root);
        });
        probe("shared child", () -> {
            TestCommand root = new TestCommand("root");
            TestCommand child = new TestCommand("show");
            child.execute(ctx -> ctx.sendMessage("ok"));
            root.argument(new Str("first")).permission(DefaultPermission.ALL).child(ref -> child);
            root.argument(new Str("second")).permission(DefaultPermission.NONE).child(ref -> child);
            System.out.println("shared child parent path => " + child.parentArguments().first().name());
            System.out.println("shared child default => " + child.permissionConfig("test.command"));
            System.out.println("shared child docs => " + CommandDocs.of("test.command", List.of(root)).permissionsJson());
        });
        probe("literal permission suggestion", () -> {
            TestCommand root = new TestCommand("root");
            root.addChildren(new TestCommand("secret"));
            TestCommandRunner runner = new TestCommandRunner(root, new TestCommandSource("test.command.root"));
            System.out.println("literal permission suggestion => " + runner.suggest("root "));
            System.out.println("literal permitted children => " + runner.visibleChildren("root"));
        });
        probe("late mutation", () -> {
            TestCommand root = new TestCommand("root");
            root.execute(ctx -> ctx.sendMessage("old"));
            TestCommandRunner runner = new TestCommandRunner(root);
            root.execute(ctx -> ctx.sendMessage("new"));
            root.addChildren(new TestCommand("added"));
            System.out.println("late mutation execute => " + runner.execute("root").messages());
            System.out.println("late mutation help => " + runner.execute("root added").messages());
        });
        probe("cached permission", () -> {
            boolean[] rootAllowed = {true};
            TestCommandSource source = new TestCommandSource() {
                public boolean hasPermission(String permission) {
                    return !permission.equals("test.command.root") || rootAllowed[0];
                }
            };
            TestCommand root = new TestCommand("root");
            TestCommand child = new TestCommand("child");
            child.execute(ctx -> ctx.sendMessage("EXECUTED"));
            root.addChildren(child);
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<TestCommandSource>();
            new CommandNodeCreator<>(List.of(root), "test.command", TestCommandRunner.INLINE).build().forEach(dispatcher.getRoot()::addChild);
            var parsed = dispatcher.parse("root child", source);
            rootAllowed[0] = false;
            System.out.println("cached permission => " + dispatcher.execute(parsed) + ", " + TestCommandContext.latest().messages());
        });
        probe("cached confirmation", () -> {
            TestCommand root = new TestCommand("confirm");
            root.confirm();
            root.execute(ctx -> ctx.sendMessage("EXECUTED"));
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<TestCommandSource>();
            new CommandNodeCreator<>(List.of(root), "test.command", TestCommandRunner.INLINE).build().forEach(dispatcher.getRoot()::addChild);
            var source = new TestCommandSource() { public boolean hasPermission(String p) { return true; } };
            var parsed = dispatcher.parse("confirm", source);
            for (int i = 0; i < 3; i++) {
                try { dispatcher.execute(parsed); } catch (CommandFailure e) { e.sendMessages(); }
                System.out.println("cached confirmation " + i + " => " + TestCommandContext.latest().messages());
            }
        });
        probe("suggestion options", () -> {
            TestCommand root = new TestCommand("root");
            var flag = root.option(Options.flag("flag", 'f'));
            root.argument(new Str("value").setSuggestionProvider(sb -> sb.suggest("flag=" + sb.getContext().getOption(flag)))).execute((v, ctx) -> {});
            System.out.println("suggestion options => " + new TestCommandRunner(root).suggest("root -f "));
        });
    }
}

```

### 採取した出力

```text
nested same name => [parent=B,child=B]
java.lang.IllegalArgumentException: No such argument 'count' exists on this command
	at com.mojang.brigadier.context.CommandContext.getArgument(CommandContext.java:85)
	at com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(IntegerArgumentType.java:37)
	at dev.marulabs.commandlib.argument.CommonIntegerArgument.parseImpl(CommonIntegerArgument.java:23)
	at dev.marulabs.commandlib.argument.CommonIntegerArgument.parseImpl(CommonIntegerArgument.java:7)
	at dev.marulabs.commandlib.CommonArgument.parse(CommonArgument.java:286)
	at dev.marulabs.commandlib.Arguments.parse(Arguments.java:53)
	at dev.marulabs.commandlib.CommandRunner.runInContext(CommandRunner.java:145)
	at dev.marulabs.commandlib.CommandRunner.lambda$run$0(CommandRunner.java:88)
	at dev.marulabs.commandlib.CurrentCommandContext.call(CurrentCommandContext.java:26)
	at dev.marulabs.commandlib.CommandRunner.run(CommandRunner.java:88)
	at com.mojang.brigadier.CommandDispatcher.execute(CommandDispatcher.java:263)
	at com.mojang.brigadier.CommandDispatcher.execute(CommandDispatcher.java:177)
	at com.mojang.brigadier.CommandDispatcher.execute(CommandDispatcher.java:142)
	at dev.marulabs.commandlib.TestCommandRunner.dispatch(TestCommandRunner.java:92)
	at dev.marulabs.commandlib.TestCommandRunner.executeAndGetResult(TestCommandRunner.java:80)
	at dev.marulabs.commandlib.TestCommandRunner.execute(TestCommandRunner.java:55)
	at dev.marulabs.commandlib.AuditProbe.lambda$main$4(AuditProbe.java:32)
	at dev.marulabs.commandlib.AuditProbe.probe(AuditProbe.java:16)
	at dev.marulabs.commandlib.AuditProbe.main(AuditProbe.java:29)
optional matches command => IllegalArgumentException: No such argument 'count' exists on this command
cycle accepted by addChildren
cycle => StackOverflowError: null
shared child parent path => second
shared child default => dev.marulabs.commandlib.PermissionConfig@470e2030
shared child docs => {
  "permissionPrefix": "test.command",
  "permissions": [
    {
      "node": "test.command.root",
      "default": "OP",
      "description": "",
      "route": "/root",
      "kind": "command"
    },
    {
      "node": "test.command.root.first",
      "default": "ALL",
      "description": "",
      "route": "/root <first>",
      "kind": "argument"
    },
    {
      "node": "test.command.root.show",
      "default": "NONE",
      "description": "",
      "route": "/root <first> show",
      "kind": "command"
    },
    {
      "node": "test.command.root.second",
      "default": "NONE",
      "description": "",
      "route": "/root <second>",
      "kind": "argument"
    },
    {
      "node": "test.command.root.show",
      "default": "NONE",
      "description": "",
      "route": "/root <second> show",
      "kind": "command"
    }
  ]
}

literal permission suggestion => [help, secret]
literal permitted children => [help]
late mutation execute => [old]
late mutation => CommandSyntaxException: Incorrect argument for command at position 5: root <--[HERE]
cached permission => 1, [EXECUTED]
cached confirmation 0 => [Run the command again within 10s to confirm. [Confirm]]
cached confirmation 1 => []
cached confirmation 2 => []
suggestion options => [flag=false]

```

## PaperAuditProbe.java

```java
package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.Plugin;
import org.mockito.Mockito;

public class PaperAuditProbe {
    public static void main(String[] args) throws Exception {
        var fixture = new CommandLibLifecycleTest();
        fixture.setUp();
        try {
            var field = CommandLibLifecycleTest.class.getDeclaredField("plugin");
            field.setAccessible(true);
            Plugin plugin = (Plugin) field.get(fixture);
            Command broken = new Command("broken");
            broken.argument(new dev.marulabs.commandlib.argument.IntegerArgument("optional").optional(1),
                            new dev.marulabs.commandlib.argument.IntegerArgument("required")).execute((a, b, ctx) -> {});
            CommandLib lib = CommandLib.register(plugin, "audit", broken);
            System.out.println("invalid definition register returned => " + (lib != null));
            var fire = CommandLibLifecycleTest.class.getDeclaredMethod("fireCommandsEvent");
            fire.setAccessible(true);
            var dispatcher = (com.mojang.brigadier.CommandDispatcher<?>) fire.invoke(fixture);
            System.out.println("invalid definition label after event => " + dispatcher.getRoot().getChild("broken"));
            lib.unregister();
        } finally {
            fixture.tearDown();
        }
    }
}

```

### 採取した出力

```text
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
invalid definition register returned => true
Oct 18, 2026 3:11:59 PM dev.marulabs.commandlib.CommandLib logRegistrationFailure
SEVERE: Failed to register commands
java.lang.IllegalStateException: Argument 'required' of command 'broken' follows optional argument 'optional'; optional arguments must be the last arguments of their branch
	at dev.marulabs.commandlib.Arguments.validateTrailingArguments(Arguments.java:85)
	at dev.marulabs.commandlib.CommandNodeCreator.lambda$toCommandNode$6(CommandNodeCreator.java:131)
	at java.base/java.lang.Iterable.forEach(Iterable.java:75)
	at dev.marulabs.commandlib.CommandNodeCreator.toCommandNode(CommandNodeCreator.java:131)
	at dev.marulabs.commandlib.CommandNodeCreator.toCommandNodes(CommandNodeCreator.java:33)
	at dev.marulabs.commandlib.CommandNodeCreator.lambda$build$0(CommandNodeCreator.java:25)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.AbstractList$RandomAccessSpliterator.forEachRemaining(AbstractList.java:722)
invalid definition label after event => null
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
	at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:682)
	at dev.marulabs.commandlib.CommandNodeCreator.build(CommandNodeCreator.java:27)
	at dev.marulabs.commandlib.CommandSet.buildNodes(CommandSet.java:40)
	at dev.marulabs.commandlib.CommandLib.registerWithRegistrar(CommandLib.java:667)
	at dev.marulabs.commandlib.CommandLib.lambda$registerLifecycleHandler$5(CommandLib.java:616)
	at dev.marulabs.commandlib.CommandLibLifecycleTest.fireCommandsEvent(CommandLibLifecycleTest.java:602)
	at dev.marulabs.commandlib.CommandLibLifecycleTest.fireCommandsEvent(CommandLibLifecycleTest.java:579)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at dev.marulabs.commandlib.PaperAuditProbe.main(PaperAuditProbe.java:27)


```

## PermissionAuditProbe.java

```java
package dev.marulabs.commandlib;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;
import org.mockito.Mockito;
public class PermissionAuditProbe {
    public static void main(String[] args) throws Exception {
        Server server = Mockito.mock(Server.class);
        SimplePluginManager manager = new SimplePluginManager(server, Mockito.mock(SimpleCommandMap.class));
        Mockito.when(server.getPluginManager()).thenReturn(manager);
        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            Permission old = new Permission("audit.shared", PermissionDefault.TRUE);
            Permission replacement = new Permission("audit.shared", PermissionDefault.FALSE);
            CommandPermissions.add(old);
            CommandPermissions.add(replacement);
            System.out.println("replacement permission is live => " + (manager.getPermission("audit.shared") == replacement));
            CommandPermissions.remove(old);
            System.out.println("after old permission removal => " + manager.getPermission("audit.shared"));
        }
    }
}

```

### 採取した出力

```text
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
replacement permission is live => true
after old permission removal => null

```

## VelocityAuditProbe.java

```java
package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.permission.Tristate;
import org.mockito.Mockito;

public class VelocityAuditProbe {
    public static void main(String[] args) throws Exception {
        var fixture = new CommandLibTest();
        fixture.setUp();
        var p = CommandLibTest.class.getDeclaredField("plugin");
        p.setAccessible(true);
        var s = CommandLibTest.class.getDeclaredField("proxy");
        s.setAccessible(true);
        Object plugin = p.get(fixture);
        ProxyServer proxy = (ProxyServer) s.get(fixture);
        Command old = new Command("audit");
        old.permission(DefaultPermission.ALL);
        CommandLib first = CommandLib.register(plugin, proxy, "audit", old);
        first.unregister();
        Command replacement = new Command("audit");
        replacement.permission(DefaultPermission.NONE);
        CommandLib second = CommandLib.register(plugin, proxy, "audit", replacement);
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getPermissionValue(Mockito.anyString())).thenReturn(Tristate.UNDEFINED);
        System.out.println("after unregister ALL and register NONE => allowed=" + VelocityPermissions.hasPermission(player, "audit.audit"));
        System.out.println("declared default => " + replacement.effectiveDefaultPermission());
        second.unregister();
    }
}

```

### 採取した出力

```text
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
after unregister ALL and register NONE => allowed=true
declared default => NONE

```