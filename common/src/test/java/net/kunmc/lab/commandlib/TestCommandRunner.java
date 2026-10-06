package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TestCommandRunner {
    private final CommandDispatcher<TestCommandSource> dispatcher = new CommandDispatcher<>();
    private final TestCommandSource source;

    TestCommandRunner(TestCommand command) {
        this(command, new TestCommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        });
    }

    TestCommandRunner(TestCommand command, TestCommandSource source) {
        this.source = source;
        new CommandNodeCreator<>(List.of(command), "test.command").build()
                                                                  .forEach(dispatcher.getRoot()::addChild);
    }

    TestCommandContext execute(String input) throws CommandSyntaxException {
        TestCommandContext.clearLatest();
        dispatcher.execute(input, source);
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
        dispatcher.execute(reader, source);
        return TestCommandContext.latest();
    }

    int executeAndGetResult(String input) throws CommandSyntaxException {
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
