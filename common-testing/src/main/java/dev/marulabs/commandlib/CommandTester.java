package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CommandTester implements AutoCloseable {
    private final CommandDispatcher<FakeSender> dispatcher = new CommandDispatcher<>();
    private final Collection<? extends TestCommand> commands;
    private final String permissionPrefix;

    public static Builder builder() {
        return new Builder();
    }

    public CommandTester(@NotNull TestCommand command) {
        this(command, "test.command");
    }

    public CommandTester(@NotNull TestCommand command, @NotNull String permissionPrefix) {
        this(builder().command(command)
                      .permissionPrefix(permissionPrefix));
    }

    public CommandTester(@NotNull Supplier<? extends TestCommand> commandSupplier, @NotNull String permissionPrefix) {
        this(builder().command(commandSupplier)
                      .permissionPrefix(permissionPrefix));
    }

    public CommandTester(@NotNull Collection<? extends TestCommand> commands, @NotNull String permissionPrefix) {
        this(builder().commands(commands)
                      .permissionPrefix(permissionPrefix));
    }

    private CommandTester(Builder builder) {
        this.permissionPrefix = Objects.requireNonNull(builder.permissionPrefix, "permissionPrefix");
        if (builder.commandSuppliers.isEmpty()) {
            throw new IllegalStateException("At least one command must be registered.");
        }

        commands = builder.createCommands();
        new CommandNodeCreator<>(commands, permissionPrefix).build()
                                                            .forEach(dispatcher.getRoot()::addChild);
    }

    /**
     * Executes the input as the sender and returns the command's result, which {@code /execute store result} would
     * store. A command that fails sends its failure messages to the sender, as the server does, and returns 0.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull FakeSender sender) {
        TestCommandContext.clearLatest();
        try {
            return dispatcher.execute(input, sender);
        } catch (CommandFailure e) {
            e.sendMessages();
            return 0;
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        }
    }

    @NotNull
    public TestCommandContext executeAndGetContext(@NotNull String input, @NotNull FakeSender sender) {
        execute(input, sender);
        // Every node CommandLib builds creates a context when it runs, so a successful execute always leaves one.
        TestCommandContext ctx = TestCommandContext.takeLatestOnCurrentThread();
        if (ctx == null) {
            throw new IllegalStateException("No command context was created for '" + input + "'.");
        }
        return ctx;
    }

    public CompletableFuture<Suggestions> suggestions(@NotNull String input, @NotNull FakeSender sender) {
        ParseResults<FakeSender> parseResults = dispatcher.parse(input, sender);
        return dispatcher.getCompletionSuggestions(parseResults);
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    @NotNull
    public List<String> suggest(@NotNull String input, @NotNull FakeSender sender) {
        return suggestions(input, sender).join()
                                         .getList()
                                         .stream()
                                         .map(Suggestion::getText)
                                         .collect(Collectors.toList());
    }

    /**
     * Renders the registered commands as indented text: each command with its aliases and permission node, then its
     * options, argument branches and child commands.
     */
    @NotNull
    public String commandTree() {
        return CommandTreeRenderer.render(commands, permissionPrefix);
    }

    /**
     * Compares {@link #commandTree()} with {@code src/test/resources/snapshots/<snapshotName>}. A missing snapshot
     * is written and fails the test; the environment variable {@code COMMANDLIB_UPDATE_SNAPSHOTS=true} rewrites it.
     *
     * @throws AssertionError if the tree differs from the snapshot
     */
    public void assertCommandTreeMatchesSnapshot(@NotNull String snapshotName) {
        Snapshots.assertMatches(snapshotName, commandTree());
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private final List<Supplier<? extends TestCommand>> commandSuppliers = new ArrayList<>();
        private @Nullable String permissionPrefix;

        public Builder command(@NotNull TestCommand command) {
            Objects.requireNonNull(command, "command");
            return command(() -> command);
        }

        public Builder command(@NotNull Supplier<? extends TestCommand> commandSupplier) {
            commandSuppliers.add(Objects.requireNonNull(commandSupplier, "commandSupplier"));
            return this;
        }

        public Builder commands(@NotNull Collection<? extends TestCommand> commands) {
            Objects.requireNonNull(commands, "commands");
            commands.forEach(this::command);
            return this;
        }

        public Builder permissionPrefix(@NotNull String permissionPrefix) {
            this.permissionPrefix = Objects.requireNonNull(permissionPrefix, "permissionPrefix");
            return this;
        }

        public CommandTester build() {
            return new CommandTester(this);
        }

        private Collection<? extends TestCommand> createCommands() {
            List<TestCommand> commands = new ArrayList<>();
            for (Supplier<? extends TestCommand> commandSupplier : commandSuppliers) {
                commands.add(commandSupplier.get());
            }
            return commands;
        }
    }
}
