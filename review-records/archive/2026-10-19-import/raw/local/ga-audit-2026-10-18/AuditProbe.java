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
