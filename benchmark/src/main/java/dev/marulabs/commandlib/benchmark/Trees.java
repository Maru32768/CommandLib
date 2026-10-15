package dev.marulabs.commandlib.benchmark;

import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.TestCommand;
import dev.marulabs.commandlib.TestCommandContext;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * The command trees the benchmarks register. Every command has the same shape, described in docs/benchmarks.md.
 */
final class Trees {
    static final String PERMISSION_PREFIX = "bench";
    static final String[] PLAYERS = {"Alex", "Alice", "Amelia", "Andrew", "Anna", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy", "Mallory", "Niaj", "Olivia", "Peggy", "Rupert", "Sybil"};
    // Executors write here so that the JIT cannot drop their work, and never send messages, which FakeSender keeps.
    static volatile Object sink;

    private Trees() {
    }

    static List<TestCommand> commands(int count) {
        List<TestCommand> commands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            commands.add(command("cmd" + i));
        }
        return commands;
    }

    static CommandTester tester(int count) {
        return CommandTester.builder()
                            .commands(commands(count))
                            .permissionPrefix(PERMISSION_PREFIX)
                            .build();
    }

    // /<name> [-f] [-v] [-n <limit>] <target> [count], /<name> list <words> (1 to 4), /<name> admin (own permission)
    static TestCommand command(String name) {
        TestCommand command = new TestCommand(name);
        CommandOption<Boolean, TestCommandContext> force = command.option(Options.flag("force", 'f'));
        CommandOption<Boolean, TestCommandContext> verbose = command.option(Options.flag("verbose", 'v'));
        CommandOption<Integer, TestCommandContext> limit = command.option(Options.integer("limit", 'n', 10, 1, 100));
        command.argument(new WordArg("target").addSuggestionProvider(sb -> {
            for (String player : PLAYERS) {
                sb.suggest(player);
            }
        }), new IntArg("count").optional(1))
               .execute((target, count, ctx) -> sink = ctx.getOption(force) || ctx.getOption(verbose) ? target
                       : count + ctx.getOption(limit));

        TestCommand list = new TestCommand("list");
        list.argument(new WordArg("words").variadic(4))
            .execute((words, ctx) -> sink = words);
        TestCommand admin = new TestCommand("admin");
        admin.permission(PERMISSION_PREFIX + ".admin");
        admin.execute(ctx -> sink = ctx);
        command.addChildren(list, admin);
        return command;
    }

    // One command with the given number of short flags, for the combined-flag permutations of the option tree.
    static TestCommand flags(int count) {
        TestCommand command = new TestCommand("flags");
        for (int i = 0; i < count; i++) {
            command.option(Options.flag("flag" + i, (char) ('a' + i)));
        }
        command.argument(new WordArg("target"))
               .execute((target, ctx) -> sink = target);
        return command;
    }

    static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    static final class WordArg extends CommonStringArgument<TestCommandContext, WordArg> {
        WordArg(String name) {
            super(name, Type.WORD);
        }
    }
}
