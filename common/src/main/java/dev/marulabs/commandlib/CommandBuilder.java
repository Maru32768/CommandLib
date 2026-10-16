package dev.marulabs.commandlib;

import dev.marulabs.commandlib.branch.*;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configures a command in one chain, without a subclass or an initializer block. The result is the same command the
 * fluent API makes, with the same tree, help and permissions:
 *
 * <pre>{@code
 * Command heal = Command.builder("heal")
 *                       .description("Heal a player")
 *                       .permission(DefaultPermission.OP)
 *                       .argument(new PlayerArgument("target"), branch -> branch.execute((target, ctx) -> {
 *                           target.setHealth(20);
 *                       }))
 *                       .build();
 * }</pre>
 *
 * <p>Each method of {@link CommonCommand} that configures the command has a method of the same name here that returns
 * the builder. The methods that return a branch there, {@code argument(...)} and {@code require(...)}, take a callback
 * that configures the branch here. {@link #configure(Consumer)} calls any other method of the platform's command, such
 * as {@code requirePlayer()}.
 *
 * <p>The builder configures its command as it goes: {@link #build()} returns that command, and the builder is not used
 * after it.
 *
 * @param <C> the platform's command context
 * @param <T> the platform's command
 */
public final class CommandBuilder<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    private final T command;

    private CommandBuilder(@NotNull T command) {
        this.command = Objects.requireNonNull(command);
    }

    /**
     * A builder that configures {@code command}. The platforms' {@code Command.builder(name)} call it with a new
     * command.
     */
    @NotNull
    public static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> CommandBuilder<C, T> of(@NotNull T command) {
        return new CommandBuilder<>(command);
    }

    /**
     * Calls {@code configuration} with the command, for a method this builder does not have, such as a platform's
     * {@code requirePlayer()}: {@code .configure(Command::requirePlayer)}.
     */
    public CommandBuilder<C, T> configure(@NotNull Consumer<? super T> configuration) {
        configuration.accept(command);
        return this;
    }

    /**
     * Returns the configured command.
     */
    @NotNull
    public T build() {
        return command;
    }

    public CommandBuilder<C, T> description(@NotNull String description) {
        command.description(description);
        return this;
    }

    public CommandBuilder<C, T> description(@NotNull Function<C, String> description) {
        command.description(description);
        return this;
    }

    /**
     * See {@link CommonCommand#serverHelp(String)}.
     */
    public CommandBuilder<C, T> serverHelp(@NotNull String shortText) {
        command.serverHelp(shortText);
        return this;
    }

    /**
     * See {@link CommonCommand#serverHelp(String, String)}.
     */
    public CommandBuilder<C, T> serverHelp(@NotNull String shortText, @NotNull String fullText) {
        command.serverHelp(shortText, fullText);
        return this;
    }

    @SafeVarargs
    public final CommandBuilder<C, T> addChildren(@NotNull T child, @NotNull T... children) {
        command.addChildren(child, children);
        return this;
    }

    public CommandBuilder<C, T> addChildren(@NotNull Collection<? extends T> children) {
        command.addChildren(children);
        return this;
    }

    /**
     * Adds the command that {@code child} builds as a child command.
     */
    public CommandBuilder<C, T> addChild(@NotNull CommandBuilder<C, T> child) {
        command.addChildren(List.of(child.build()));
        return this;
    }

    public CommandBuilder<C, T> addAliases(@NotNull String alias, @NotNull String... aliases) {
        command.addAliases(alias, aliases);
        return this;
    }

    public CommandBuilder<C, T> addAliases(@NotNull Collection<String> aliases) {
        command.addAliases(aliases);
        return this;
    }

    /**
     * Registers {@code option}, like {@link CommonCommand#option(CommandOption)}. Keep the option to read it with
     * {@link CommonCommandContext#getOption(CommandOption)}.
     */
    public CommandBuilder<C, T> option(@NotNull CommandOption<?, ? super C> option) {
        command.option(option);
        return this;
    }

    /**
     * See {@link CommonCommand#mutuallyExclusive(CommandOption[])}.
     */
    @SafeVarargs
    public final CommandBuilder<C, T> mutuallyExclusive(@NotNull CommandOption<?, ? super C>... options) {
        command.mutuallyExclusive(options);
        return this;
    }

    /**
     * See {@link CommonCommand#visibleWhen(java.util.function.Predicate)}.
     */
    public CommandBuilder<C, T> visibleWhen(@NotNull java.util.function.Predicate<? super C> predicate) {
        command.visibleWhen(predicate);
        return this;
    }

    /**
     * See {@link CommonCommand#optionsAnywhere()}.
     */
    public CommandBuilder<C, T> optionsAnywhere() {
        command.optionsAnywhere();
        return this;
    }

    public CommandBuilder<C, T> disableParentPrerequisite() {
        command.disableParentPrerequisite();
        return this;
    }

    /**
     * See {@link CommonCommand#disableParentMiddleware()}.
     */
    public CommandBuilder<C, T> disableParentMiddleware() {
        command.disableParentMiddleware();
        return this;
    }

    /**
     * Adds an argument branch, like {@link CommonCommand#argument(CommonArgument)}, and configures it in
     * {@code branch}: its executor, description, permission and child commands.
     */
    public <T1> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                              @NotNull Consumer<? super UnaryArgumentBranch<T1, C, T>> branch) {
        branch.accept(command.argument(argument1));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with two arguments.
     */
    public <T1, T2> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                  @NotNull CommonArgument<T2, C, ?> argument2,
                                                  @NotNull Consumer<? super BiArgumentBranch<T1, T2, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with three arguments.
     */
    public <T1, T2, T3> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                      @NotNull CommonArgument<T2, C, ?> argument2,
                                                      @NotNull CommonArgument<T3, C, ?> argument3,
                                                      @NotNull Consumer<? super TriArgumentBranch<T1, T2, T3, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2, argument3));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with four arguments.
     */
    public <T1, T2, T3, T4> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                          @NotNull CommonArgument<T2, C, ?> argument2,
                                                          @NotNull CommonArgument<T3, C, ?> argument3,
                                                          @NotNull CommonArgument<T4, C, ?> argument4,
                                                          @NotNull Consumer<? super TetraArgumentBranch<T1, T2, T3, T4, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2, argument3, argument4));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with five arguments.
     */
    public <T1, T2, T3, T4, T5> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                              @NotNull CommonArgument<T2, C, ?> argument2,
                                                              @NotNull CommonArgument<T3, C, ?> argument3,
                                                              @NotNull CommonArgument<T4, C, ?> argument4,
                                                              @NotNull CommonArgument<T5, C, ?> argument5,
                                                              @NotNull Consumer<? super QuintArgumentBranch<T1, T2, T3, T4, T5, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2, argument3, argument4, argument5));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with six arguments.
     */
    public <T1, T2, T3, T4, T5, T6> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                  @NotNull CommonArgument<T2, C, ?> argument2,
                                                                  @NotNull CommonArgument<T3, C, ?> argument3,
                                                                  @NotNull CommonArgument<T4, C, ?> argument4,
                                                                  @NotNull CommonArgument<T5, C, ?> argument5,
                                                                  @NotNull CommonArgument<T6, C, ?> argument6,
                                                                  @NotNull Consumer<? super HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2, argument3, argument4, argument5, argument6));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with seven arguments.
     */
    public <T1, T2, T3, T4, T5, T6, T7> CommandBuilder<C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                      @NotNull CommonArgument<T2, C, ?> argument2,
                                                                      @NotNull CommonArgument<T3, C, ?> argument3,
                                                                      @NotNull CommonArgument<T4, C, ?> argument4,
                                                                      @NotNull CommonArgument<T5, C, ?> argument5,
                                                                      @NotNull CommonArgument<T6, C, ?> argument6,
                                                                      @NotNull CommonArgument<T7, C, ?> argument7,
                                                                      @NotNull Consumer<? super HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T>> branch) {
        branch.accept(command.argument(argument1, argument2, argument3, argument4, argument5, argument6, argument7));
        return this;
    }

    /**
     * Same as {@link #argument(CommonArgument, Consumer)} with any number of arguments, such as eight or more. The
     * executor reads each value typed with {@link CommonCommandContext#getArgument(CommonArgument)}.
     */
    public CommandBuilder<C, T> argument(@NotNull List<? extends CommonArgument<?, C, ?>> arguments,
                                         @NotNull Consumer<? super ArgumentBranch<C, T>> branch) {
        branch.accept(command.argument(arguments));
        return this;
    }

    /**
     * Requires what {@code extractor} gets from the context, like {@link CommonCommand#require(Extractor)}, and
     * configures the executor that receives it in {@code branch}.
     */
    public <S> CommandBuilder<C, T> require(@NotNull Extractor<C, S> extractor,
                                            @NotNull Consumer<? super RequiredArgumentBranch<S, C, T>> branch) {
        branch.accept(command.require(extractor));
        return this;
    }

    public CommandBuilder<C, T> addPrerequisite(@NotNull Prerequisite<C> prerequisite) {
        command.addPrerequisite(prerequisite);
        return this;
    }

    /**
     * See {@link CommonCommand#addPreprocess(Predicate)}.
     */
    public CommandBuilder<C, T> addPreprocess(@NotNull Predicate<C> preprocess) {
        command.addPreprocess(preprocess);
        return this;
    }

    /**
     * See {@link CommonCommand#addMiddleware(CommandMiddleware)}.
     */
    public CommandBuilder<C, T> addMiddleware(@NotNull CommandMiddleware<? super C> middleware) {
        command.addMiddleware(middleware);
        return this;
    }

    /**
     * See {@link CommonCommand#provide(ContextKey, Extractor)}.
     */
    public <V> CommandBuilder<C, T> provide(@NotNull ContextKey<V> key,
                                            @NotNull Extractor<? super C, ? extends V> provider) {
        command.provide(key, provider);
        return this;
    }

    /**
     * See {@link CommonCommand#provide(Class, Extractor)}.
     */
    public <V> CommandBuilder<C, T> provide(@NotNull Class<V> type,
                                            @NotNull Extractor<? super C, ? extends V> provider) {
        command.provide(type, provider);
        return this;
    }

    /**
     * See {@link CommonCommand#confirm()}.
     */
    public CommandBuilder<C, T> confirm() {
        command.confirm();
        return this;
    }

    /**
     * See {@link CommonCommand#confirm(Confirmation)}.
     */
    public CommandBuilder<C, T> confirm(@NotNull Confirmation confirmation) {
        command.confirm(confirmation);
        return this;
    }

    /**
     * See {@link CommonCommand#cooldown(Duration)}.
     */
    public CommandBuilder<C, T> cooldown(@NotNull Duration duration) {
        command.cooldown(duration);
        return this;
    }

    /**
     * See {@link CommonCommand#cooldown(Cooldown)}.
     */
    public CommandBuilder<C, T> cooldown(@NotNull Cooldown cooldown) {
        command.cooldown(cooldown);
        return this;
    }

    /**
     * See {@link CommonCommand#captions(Captions)}.
     */
    public CommandBuilder<C, T> captions(@NotNull Captions captions) {
        command.captions(captions);
        return this;
    }

    /**
     * See {@link CommonCommand#helpFormatter(HelpFormatter)}.
     */
    public CommandBuilder<C, T> helpFormatter(@NotNull HelpFormatter helpFormatter) {
        command.helpFormatter(helpFormatter);
        return this;
    }

    /**
     * See {@link CommonCommand#usageFormatter(UsageFormatter)}.
     */
    public CommandBuilder<C, T> usageFormatter(@NotNull UsageFormatter usageFormatter) {
        command.usageFormatter(usageFormatter);
        return this;
    }

    /**
     * See {@link CommonCommand#errorFormatter(ErrorFormatter)}.
     */
    public CommandBuilder<C, T> errorFormatter(@NotNull ErrorFormatter errorFormatter) {
        command.errorFormatter(errorFormatter);
        return this;
    }

    /**
     * See {@link CommonCommand#examples(String...)}.
     */
    public CommandBuilder<C, T> examples(@NotNull String... examples) {
        command.examples(examples);
        return this;
    }

    public CommandBuilder<C, T> execute(@NotNull CommandExecutor<C> executor) {
        command.execute(executor);
        return this;
    }

    /**
     * See {@link CommonCommand#executeAsync(AsyncCommandExecutor)}.
     */
    public CommandBuilder<C, T> executeAsync(@NotNull AsyncCommandExecutor<C> executor) {
        command.executeAsync(executor);
        return this;
    }

    public CommandBuilder<C, T> addUncaughtExceptionHandler(@NotNull UncaughtExceptionHandler<C> handler) {
        command.addUncaughtExceptionHandler(handler);
        return this;
    }

    public CommandBuilder<C, T> permission(@NotNull String node) {
        command.permission(node);
        return this;
    }

    public CommandBuilder<C, T> permission(@NotNull DefaultPermission defaultPermission) {
        command.permission(defaultPermission);
        return this;
    }

    public CommandBuilder<C, T> permission(@NotNull DefaultPermission defaultPermission, @NotNull String description) {
        command.permission(defaultPermission, description);
        return this;
    }

    public CommandBuilder<C, T> permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
        command.permission(node, defaultPermission);
        return this;
    }

    public CommandBuilder<C, T> permission(@NotNull String node,
                                           @NotNull DefaultPermission defaultPermission,
                                           @NotNull String description) {
        command.permission(node, defaultPermission, description);
        return this;
    }

    public CommandBuilder<C, T> permissionDescription(@NotNull String description) {
        command.permissionDescription(description);
        return this;
    }
}
