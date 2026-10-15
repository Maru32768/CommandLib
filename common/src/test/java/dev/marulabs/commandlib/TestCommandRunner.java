package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TestCommandRunner {
    static final CommandScheduler INLINE = CommandScheduler.of(Runnable::run, Runnable::run);
    private final CommandDispatcher<TestCommandSource> dispatcher = new CommandDispatcher<>();
    private final TestCommandSource source;

    TestCommandRunner(TestCommand command) {
        this(command, INLINE);
    }

    TestCommandRunner(TestCommand command, CommandScheduler scheduler) {
        this(command, new TestCommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        }, scheduler);
    }

    TestCommandRunner(TestCommand command, TestCommandSource source) {
        this(command, source, INLINE);
    }

    TestCommandRunner(TestCommand command, TestCommandSource source, CommandScheduler scheduler) {
        this.source = source;
        new CommandNodeCreator<>(List.of(command), "test.command", scheduler).build()
                                                                             .forEach(dispatcher.getRoot()::addChild);
    }

    com.mojang.brigadier.tree.RootCommandNode<TestCommandSource> root() {
        return dispatcher.getRoot();
    }

    /**
     * Adds a literal that redirects to the root, as vanilla's "execute run" does.
     */
    void redirectToRoot(String literal) {
        dispatcher.register(LiteralArgumentBuilder.<TestCommandSource>literal(literal)
                                                  .redirect(dispatcher.getRoot()));
    }

    TestCommandContext execute(String input) throws CommandSyntaxException {
        executeAndGetResult(input);
        return TestCommandContext.latest();
    }

    /**
     * Executes input the way a server handles player chat commands: the leading slash stays in the input string while
     * the reader starts after it.
     */
    TestCommandContext executeWithLeadingSlash(String input) throws CommandSyntaxException {
        TestCommandContext.clearLatest();
        StringReader reader = new StringReader(input);
        reader.skip();
        try {
            dispatcher.execute(reader, source);
        } catch (CommandFailure e) {
            e.sendMessages();
        }
        return TestCommandContext.latest();
    }

    /**
     * Executes input like the testers: a failure sends its messages to the context and returns 0.
     */
    int executeAndGetResult(String input) throws CommandSyntaxException {
        try {
            return dispatch(input);
        } catch (CommandFailure e) {
            e.sendMessages();
            return 0;
        }
    }

    /**
     * Executes input like a server, which gets the failure of a command as the exception.
     */
    int dispatch(String input) throws CommandSyntaxException {
        TestCommandContext.clearLatest();
        return dispatcher.execute(input, source);
    }

    Map<String, String> suggestTooltips(String input) {
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source))
                                            .join();
        Map<String, String> result = new LinkedHashMap<>();
        suggestions.getList()
                   .forEach(x -> result.put(x.getText(),
                                            x.getTooltip() == null ? null : x.getTooltip()
                                                                             .getString()));
        return result;
    }

    /**
     * Returns the children of the literal path that the source can use, like the command tree a server sends to a
     * client. Brigadier's own completion does not check requirements, so permission visibility is checked here.
     */
    List<String> visibleChildren(String... path) {
        com.mojang.brigadier.tree.CommandNode<TestCommandSource> node = dispatcher.getRoot();
        for (String name : path) {
            node = node.getChild(name);
        }
        return node.getChildren()
                   .stream()
                   .filter(x -> x.canUse(source))
                   .map(com.mojang.brigadier.tree.CommandNode::getName)
                   .collect(Collectors.toList());
    }

    List<String> suggest(String input) {
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source))
                                            .join();
        return suggestions.getList()
                          .stream()
                          .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                          .collect(Collectors.toList());
    }
}
