package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CommandTester implements AutoCloseable {
    private final CommandDispatcher<FakeSender> dispatcher = new CommandDispatcher<>();

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
        Objects.requireNonNull(builder.permissionPrefix, "permissionPrefix");
        if (builder.commandSuppliers.isEmpty()) {
            throw new IllegalStateException("At least one command must be registered.");
        }

        new CommandNodeCreator<>(builder.createCommands(), builder.permissionPrefix).build()
                                                                                    .forEach(dispatcher.getRoot()::addChild);
    }

    public void execute(@NotNull String input, @NotNull FakeSender sender) {
        TestCommandContext.clearLatest();
        try {
            dispatcher.execute(input, sender);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        }
    }

    public TestCommandContext executeAndGetContext(String input, FakeSender sender) {
        execute(input, sender);
        return TestCommandContext.takeLatestOnCurrentThread();
    }

    public CompletableFuture<Suggestions> suggestions(@NotNull String input, @NotNull FakeSender sender) {
        ParseResults<FakeSender> parseResults = dispatcher.parse(input, sender);
        return dispatcher.getCompletionSuggestions(parseResults);
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private final List<Supplier<? extends TestCommand>> commandSuppliers = new ArrayList<>();
        private String permissionPrefix;

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
