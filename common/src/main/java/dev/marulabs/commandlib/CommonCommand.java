package dev.marulabs.commandlib;

import dev.marulabs.commandlib.branch.*;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class CommonCommand<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    private final String name;
    private Function<C, String> description = ctx -> "";
    // The text of description(String), or null when a function gives the description.
    private @Nullable String staticDescription = "";
    private @Nullable String serverHelpShortText = null;
    private @Nullable String serverHelpFullText = null;
    private @Nullable String permissionNodeOverride = null;
    private @Nullable DefaultPermission defaultPermissionOverride = null;
    private String permissionDescription = "";
    // Null for a top-level command.
    private @Nullable T parent = null;
    // The parent's argument branch this command follows, or null when it follows the parent's literal.
    private @Nullable Arguments<C> parentArguments = null;
    private boolean inheritParentPrerequisite = true;
    private boolean inheritParentMiddleware = true;
    private final List<T> children = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();
    private final List<Arguments<C>> argumentsList = new ArrayList<>();
    private final List<CommandOption<?, C>> options = new ArrayList<>();
    private final List<Set<CommandOption<?, ?>>> exclusiveOptionGroups = new ArrayList<>();
    private Prerequisite<C> prerequisite = ctx -> {
    };
    private final List<CommandMiddleware<? super C>> middlewares = new ArrayList<>();
    private final Map<ContextKey<?>, Extractor<? super C, ?>> providers = new LinkedHashMap<>();
    private @Nullable Confirmation confirmation = null;
    private @Nullable Cooldown cooldown = null;
    private @Nullable Captions captions = null;
    // Null until execute is called; the command then runs its help action.
    private @Nullable AsyncCommandExecutor<C> executor;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers = new ArrayList<>();

    protected CommonCommand(@NotNull String name) {
        Objects.requireNonNull(name);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is not expected to empty.");
        }
        this.name = name;
    }

    public final String name() {
        return name;
    }

    public final String description(C ctx) {
        return description.apply(ctx);
    }

    public final void description(@NotNull String description) {
        Objects.requireNonNull(description);
        this.description = ctx -> description;
        this.staticDescription = description;
    }

    public final void description(@NotNull Function<C, String> description) {
        this.description = Objects.requireNonNull(description);
        this.staticDescription = null;
    }

    /**
     * The text set with {@link #description(String)}, or null when {@link #description(Function)} set a function,
     * whose text depends on the sender.
     */
    @Nullable
    final String staticDescription() {
        return staticDescription;
    }

    /**
     * Sets the text the server's {@code /help} shows for this top-level command in its command lists, which is also
     * the description of the command in the server's command map. Without it the server help shows a text naming the
     * plugin; {@link #description(String)} is not used there. Platforms without a server help, such as Forge, ignore
     * it.
     */
    public final void serverHelp(@NotNull String shortText) {
        this.serverHelpShortText = Objects.requireNonNull(shortText);
        this.serverHelpFullText = null;
    }

    /**
     * Same as {@link #serverHelp(String)}, and replaces the whole page that {@code /help <command>} shows, which is
     * otherwise generated from the short text, the usage and the aliases.
     */
    public final void serverHelp(@NotNull String shortText, @NotNull String fullText) {
        this.serverHelpShortText = Objects.requireNonNull(shortText);
        this.serverHelpFullText = Objects.requireNonNull(fullText);
    }

    /**
     * The text for the server help's command lists: the {@link #serverHelp(String)} text, or else a text naming the
     * plugin.
     */
    final String serverHelpShortText(String pluginName) {
        if (serverHelpShortText != null) {
            return serverHelpShortText;
        }
        return "A command by the " + pluginName + " plugin.";
    }

    /**
     * The page set with {@link #serverHelp(String, String)}, or null to keep the generated one.
     */
    @Nullable
    final String serverHelpFullText() {
        return serverHelpFullText;
    }

    @SafeVarargs
    public final void addChildren(@NotNull T child, @NotNull T... children) {
        List<T> list = new ArrayList<>();
        list.add(child);
        Collections.addAll(list, children);
        addChildren(list);
    }

    public final void addChildren(@NotNull Collection<? extends T> children) {
        Objects.requireNonNull(children);
        validateChildren(children);
        int size = this.children.size();
        this.children.addAll(children);
        try {
            CommandNameValidator.validateChildren(this);
        } catch (IllegalArgumentException e) {
            this.children.subList(size, this.children.size())
                         .clear();
            throw e;
        }
        setParentFor(children, null);
    }

    private void validateChildren(@NotNull Collection<? extends T> children) {
        for (T child : children) {
            Objects.requireNonNull(child);
        }
    }

    @SuppressWarnings("unchecked")
    private void setParentFor(@NotNull Collection<? extends T> children, @Nullable Arguments<C> parentArguments) {
        for (CommonCommand<C, T> child : children) {
            child.parent = ((T) this);
            child.parentArguments = parentArguments;
        }
    }

    public final void addAliases(@NotNull String alias, @NotNull String... aliases) {
        List<String> list = new ArrayList<>();
        list.add(alias);
        Collections.addAll(list, aliases);
        addAliases(list);
    }

    public final void addAliases(@NotNull Collection<String> aliases) {
        Objects.requireNonNull(aliases);
        for (String alias : aliases) {
            Objects.requireNonNull(alias);
        }
        int size = this.aliases.size();
        this.aliases.addAll(aliases);
        if (parent == null) {
            return;
        }
        try {
            CommandNameValidator.validateChildren(parent);
        } catch (IllegalArgumentException e) {
            this.aliases.subList(size, this.aliases.size())
                        .clear();
            throw e;
        }
    }

    /**
     * Registers {@code option} on this command and returns it typed for this command's context. Configure the
     * option, including its aliases, before registering it.
     *
     * @throws IllegalArgumentException if a long name or the short name of {@code option} is already used by
     *                                  another option of this command
     */
    @SuppressWarnings("unchecked")
    public final <E> CommandOption<E, C> option(@NotNull CommandOption<E, ? super C> option) {
        Objects.requireNonNull(option);
        for (String name : option.names()) {
            boolean duplicatedName = options.stream()
                                            .anyMatch(x -> x.names()
                                                            .contains(name));
            if (duplicatedName) {
                throw new IllegalArgumentException("Duplicate option name: " + name);
            }
        }

        Character shortName = option.shortName();
        if (shortName != null) {
            boolean duplicatedShortName = options.stream()
                                                 .map(CommandOption::shortName)
                                                 .filter(Objects::nonNull)
                                                 .anyMatch(x -> x.equals(shortName));
            if (duplicatedShortName) {
                throw new IllegalArgumentException("Duplicate option short name: " + shortName);
            }
        }

        CommandOption<E, C> typedOption = (CommandOption<E, C>) option;
        options.add(typedOption);
        return typedOption;
    }

    /**
     * Allows at most one of {@code options} in one input. Once one of them is given, the command tree no longer
     * offers the others, so they are neither suggested nor accepted.
     *
     * @throws IllegalArgumentException if fewer than two options are given or one of them is not registered on this
     *                                  command
     */
    @SafeVarargs
    public final void mutuallyExclusive(@NotNull CommandOption<?, ? super C>... options) {
        Set<CommandOption<?, ?>> group = new LinkedHashSet<>(Arrays.asList(options));
        if (group.size() < 2) {
            throw new IllegalArgumentException("A mutually exclusive group needs at least two options.");
        }
        for (CommandOption<?, ?> option : group) {
            if (!this.options.contains(Objects.requireNonNull(option))) {
                throw new IllegalArgumentException("Option " + option.formatName() + " is not registered on " + name
                        + ".");
            }
        }
        exclusiveOptionGroups.add(Set.copyOf(group));
    }

    public final void disableParentPrerequisite() {
        this.inheritParentPrerequisite = false;
    }

    /**
     * Keeps this command from inheriting the middlewares of its parent: those added with
     * {@link #addMiddleware(CommandMiddleware)} and {@link #addPreprocess(Predicate)}, the confirmation and the
     * cooldown.
     */
    public final void disableParentMiddleware() {
        this.inheritParentMiddleware = false;
    }

    /**
     * Adds a branch of any number of arguments, such as one of eight or more, which the typed overloads do not take.
     * Its executor reads each value typed with {@link CommonCommandContext#getArgument(CommonArgument)}:
     *
     * <pre>{@code
     * argument(x, y, z, yaw, pitch, world, speed, label).execute(ctx -> {
     *     double value = ctx.getArgument(x);
     * });
     * }</pre>
     *
     * @throws IllegalArgumentException if no argument is given or two of them have the same name
     */
    @SafeVarargs
    public final ArgumentBranch<C, T> argument(@NotNull CommonArgument<?, C, ?>... arguments) {
        return argument(Arrays.asList(arguments));
    }

    /**
     * Same as {@link #argument(CommonArgument[])} with the arguments in a list, for a branch built at runtime.
     */
    public final ArgumentBranch<C, T> argument(@NotNull List<? extends CommonArgument<?, C, ?>> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("An argument branch needs at least one argument.");
        }
        return new ArgumentBranch<>(delegateFor(addArguments(List.copyOf(arguments))));
    }

    public final <T1> UnaryArgumentBranch<T1, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument) {
        Arguments<C> arguments = addArguments(List.of(argument));
        return new UnaryArgumentBranch<>(delegateFor(arguments), argument);
    }

    public final <T1, T2> BiArgumentBranch<T1, T2, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                  @NotNull CommonArgument<T2, C, ?> argument2) {
        Arguments<C> arguments = addArguments(List.of(argument1, argument2));
        return new BiArgumentBranch<>(delegateFor(arguments), argument1, argument2);
    }

    public final <T1, T2, T3> TriArgumentBranch<T1, T2, T3, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                           @NotNull CommonArgument<T2, C, ?> argument2,
                                                                           @NotNull CommonArgument<T3, C, ?> argument3) {
        Arguments<C> arguments = addArguments(List.of(argument1, argument2, argument3));
        return new TriArgumentBranch<>(delegateFor(arguments), argument1, argument2, argument3);
    }

    public final <T1, T2, T3, T4> TetraArgumentBranch<T1, T2, T3, T4, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                                     @NotNull CommonArgument<T2, C, ?> argument2,
                                                                                     @NotNull CommonArgument<T3, C, ?> argument3,
                                                                                     @NotNull CommonArgument<T4, C, ?> argument4) {
        Arguments<C> arguments = addArguments(List.of(argument1, argument2, argument3, argument4));
        return new TetraArgumentBranch<>(delegateFor(arguments), argument1, argument2, argument3, argument4);
    }

    public final <T1, T2, T3, T4, T5> QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                                             @NotNull CommonArgument<T2, C, ?> argument2,
                                                                                             @NotNull CommonArgument<T3, C, ?> argument3,
                                                                                             @NotNull CommonArgument<T4, C, ?> argument4,
                                                                                             @NotNull CommonArgument<T5, C, ?> argument5) {
        Arguments<C> arguments = addArguments(List.of(argument1, argument2, argument3, argument4, argument5));
        return new QuintArgumentBranch<>(delegateFor(arguments), argument1, argument2, argument3, argument4, argument5);
    }

    public final <T1, T2, T3, T4, T5, T6> HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                                                    @NotNull CommonArgument<T2, C, ?> argument2,
                                                                                                    @NotNull CommonArgument<T3, C, ?> argument3,
                                                                                                    @NotNull CommonArgument<T4, C, ?> argument4,
                                                                                                    @NotNull CommonArgument<T5, C, ?> argument5,
                                                                                                    @NotNull CommonArgument<T6, C, ?> argument6) {
        Arguments<C> arguments = addArguments(List.of(argument1,
                                                      argument2,
                                                      argument3,
                                                      argument4,
                                                      argument5,
                                                      argument6));
        return new HexaArgumentBranch<>(delegateFor(arguments),
                                        argument1,
                                        argument2,
                                        argument3,
                                        argument4,
                                        argument5,
                                        argument6);
    }

    public final <T1, T2, T3, T4, T5, T6, T7> HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> argument(@NotNull CommonArgument<T1, C, ?> argument1,
                                                                                                            @NotNull CommonArgument<T2, C, ?> argument2,
                                                                                                            @NotNull CommonArgument<T3, C, ?> argument3,
                                                                                                            @NotNull CommonArgument<T4, C, ?> argument4,
                                                                                                            @NotNull CommonArgument<T5, C, ?> argument5,
                                                                                                            @NotNull CommonArgument<T6, C, ?> argument6,
                                                                                                            @NotNull CommonArgument<T7, C, ?> argument7) {
        Arguments<C> arguments = addArguments(List.of(argument1,
                                                      argument2,
                                                      argument3,
                                                      argument4,
                                                      argument5,
                                                      argument6,
                                                      argument7));
        return new HeptArgumentBranch<>(delegateFor(arguments),
                                        argument1,
                                        argument2,
                                        argument3,
                                        argument4,
                                        argument5,
                                        argument6,
                                        argument7);
    }

    private ArgumentBranchDelegate<C, T> delegateFor(Arguments<C> arguments) {
        return new ArgumentBranchDelegate<>() {
            @Override
            public void description(@NotNull String description) {
                arguments.description(description);
            }

            @Override
            public void description(@NotNull Function<C, String> description) {
                arguments.description(description);
            }

            @Override
            public void execute(@NotNull AsyncCommandExecutor<C> action) {
                arguments.executor(action);
            }

            @Override
            public void addChildren(@NotNull Collection<? extends T> children) {
                addArgumentChildren(arguments, children);
            }

            @Override
            public void permission(@NotNull String node) {
                arguments.permission(node);
            }

            @Override
            public void permission(@NotNull DefaultPermission defaultPermission) {
                arguments.permission(defaultPermission);
            }

            @Override
            public void permission(@NotNull DefaultPermission defaultPermission, @NotNull String description) {
                arguments.permission(defaultPermission, description);
            }

            @Override
            public void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
                arguments.permission(node, defaultPermission);
            }

            @Override
            public void permission(@NotNull String node,
                                   @NotNull DefaultPermission defaultPermission,
                                   @NotNull String description) {
                arguments.permission(node, defaultPermission, description);
            }

            @Override
            public void permissionDescription(@NotNull String description) {
                arguments.permissionDescription(description);
            }
        };
    }

    private Arguments<C> addArguments(@NotNull List<? extends CommonArgument<?, C, ?>> arguments) {
        return addArguments(arguments, List.of());
    }

    private Arguments<C> addArguments(@NotNull List<? extends CommonArgument<?, C, ?>> arguments,
                                      @NotNull Collection<? extends T> children) {
        validateArguments(arguments);
        validateChildren(children);
        Arguments<C> argumentBranch = new Arguments<>(this, arguments, children);
        argumentsList.add(argumentBranch);
        try {
            CommandNameValidator.validateChildren(this);
        } catch (IllegalArgumentException e) {
            argumentsList.remove(argumentsList.size() - 1);
            throw e;
        }
        setParentFor(children, argumentBranch);
        return argumentBranch;
    }

    final void addArgumentChildren(@NotNull Arguments<C> arguments, @NotNull Collection<? extends T> children) {
        Objects.requireNonNull(arguments);
        Objects.requireNonNull(children);
        validateChildren(children);
        int size = arguments.children()
                            .size();
        arguments.addChildren(children);
        try {
            CommandNameValidator.validateChildren(this);
        } catch (IllegalArgumentException e) {
            arguments.removeChildrenFrom(size);
            throw e;
        }
        setParentFor(children, arguments);
    }

    private void validateArguments(@NotNull List<? extends CommonArgument<?, C, ?>> arguments) {
        for (CommonArgument<?, C, ?> argument : arguments) {
            Objects.requireNonNull(argument);
        }
        Set<String> names = new HashSet<>();
        for (CommonArgument<?, C, ?> argument : arguments) {
            if (!names.add(argument.name())) {
                throw new IllegalArgumentException("Duplicate argument name: " + argument.name());
            }
        }
    }

    public final <S> RequiredArgumentBranch<S, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredArgumentBranch<>(Objects.requireNonNull(extractor), this::executeAsync, this::addChildren);
    }

    public final void addPrerequisite(@NotNull Prerequisite<C> prerequisite) {
        this.prerequisite = this.prerequisite.and(Objects.requireNonNull(prerequisite));
    }

    public final void addPreprocess(@NotNull Consumer<C> preprocess) {
        Objects.requireNonNull(preprocess);

        addPreprocess(ctx -> {
            preprocess.accept(ctx);
            return true;
        });
    }

    /**
     * Adds a check that runs before this command executes, as a middleware. Returning false fails the command with the
     * messages the check sent, or silently when it sent none; returning true sends them and continues.
     *
     * @param preprocess return false if cancel executing command.
     */
    public final void addPreprocess(@NotNull Predicate<C> preprocess) {
        Objects.requireNonNull(preprocess);
        addMiddleware(preprocessMiddleware(preprocess));
    }

    /**
     * Adds a middleware that wraps the execution of this command, its argument branches and, unless they call
     * {@link #disableParentMiddleware()}, its child commands. Middlewares run in the order they are added, the
     * parent's before the child's. See {@link CommandMiddleware}.
     */
    public final void addMiddleware(@NotNull CommandMiddleware<? super C> middleware) {
        this.middlewares.add(Objects.requireNonNull(middleware));
    }

    /**
     * Provides the value of {@code key} to the executions of this command, its argument branches and its child
     * commands: {@link CommonCommandContext#get(ContextKey)} calls {@code provider} the first time it reads the key in
     * an execution and keeps the value for the rest of it. A child's provider of the same key replaces the parent's,
     * and a value a middleware puts with {@link CommonCommandContext#put(ContextKey, Object)} replaces both.
     * <p>
     * Use it to map the sender to a type of the plugin once ({@code provide(GamePlayer.class, ctx -> ...)}), or to
     * give the commands a service ({@code provide(Economy.class, ctx -> economy)}). A
     * {@link dev.marulabs.commandlib.exception.CommandPrerequisiteException} thrown by {@code provider} fails the
     * command with its message. Unlike middlewares, providers are inherited also by a child that calls
     * {@link #disableParentMiddleware()}.
     */
    public final <V> void provide(@NotNull ContextKey<V> key, @NotNull Extractor<? super C, ? extends V> provider) {
        this.providers.put(Objects.requireNonNull(key), Objects.requireNonNull(provider));
    }

    /**
     * Same as {@link #provide(ContextKey, Extractor)} with the key of {@code type}, which
     * {@link CommonCommandContext#get(Class)} reads.
     */
    public final <V> void provide(@NotNull Class<V> type, @NotNull Extractor<? super C, ? extends V> provider) {
        provide(ContextKey.of(type), provider);
    }

    /**
     * The providers of an execution of this command, the parent's replaced by this command's for the same key.
     */
    final Map<ContextKey<?>, Extractor<? super C, ?>> providers() {
        if (parent == null) {
            return Map.copyOf(providers);
        }
        Map<ContextKey<?>, Extractor<? super C, ?>> result = new HashMap<>(parent.providers());
        result.putAll(providers);
        return Map.copyOf(result);
    }

    /**
     * A middleware that collects the messages {@code check} sends until it tells whether the command fails, and then
     * fails with them or sends them.
     */
    private static <C extends CommonCommandContext<?, ?>> CommandMiddleware<C> preprocessMiddleware(Predicate<? super C> check) {
        return (ctx, next) -> {
            boolean[] passed = new boolean[1];
            CommandFailure failure = ctx.captureFailure(() -> passed[0] = check.test(ctx));
            if (!passed[0]) {
                return CompletableFuture.failedFuture(failure);
            }
            failure.sendMessages();
            return next.proceed();
        };
    }

    /**
     * Asks the sender to run the command again within 10 seconds before it executes. See {@link Confirmation}.
     */
    public final void confirm() {
        confirm(Confirmation.within(Duration.ofSeconds(10)));
    }

    /**
     * Asks the sender to confirm before the command executes, as {@code confirmation} describes. Like a middleware,
     * it applies to the argument branches and, unless they call {@link #disableParentMiddleware()}, to the child
     * commands that set none of their own. It replaces the confirmation set before.
     */
    public final void confirm(@NotNull Confirmation confirmation) {
        this.confirmation = Objects.requireNonNull(confirmation);
    }

    /**
     * Keeps a sender from executing the command again for {@code duration}. See {@link Cooldown}.
     */
    public final void cooldown(@NotNull Duration duration) {
        cooldown(Cooldown.of(duration));
    }

    /**
     * Keeps a sender from executing the command again until {@code cooldown} ends. Like a middleware, it applies to the
     * argument branches and, unless they call {@link #disableParentMiddleware()}, to the child commands that set none
     * of their own, which then share it. It replaces the cooldown set before.
     * <p>
     * The cooldown is checked before the confirmation and the other middlewares, and starts only when they all pass,
     * right before the executor runs.
     */
    public final void cooldown(@NotNull Cooldown cooldown) {
        this.cooldown = Objects.requireNonNull(cooldown);
    }

    /**
     * Sets the texts of CommandLib's messages and of {@code ctx.sendFailure(caption)} for this command, its argument
     * branches and the child commands that set none of their own. Without it, the commands use
     * {@link Captions#create()}. See {@link Captions}.
     */
    public final void captions(@NotNull Captions captions) {
        this.captions = Objects.requireNonNull(captions);
    }

    final Captions captions() {
        if (captions != null) {
            return captions;
        }
        return parent != null ? parent.captions() : Captions.DEFAULT;
    }

    public final void execute(@NotNull CommandExecutor<C> executor) {
        executeAsync(AsyncCommandExecutor.of(executor));
    }

    /**
     * Sets an executor whose work finishes later. It runs on the sync thread and returns the stage that completes when
     * the command is done, such as one made with {@link CommonCommandContext#supplyAsync}. See
     * {@link AsyncCommandExecutor}.
     */
    public final void executeAsync(@NotNull AsyncCommandExecutor<C> executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public final void addUncaughtExceptionHandler(UncaughtExceptionHandler<C> executor) {
        this.uncaughtExceptionHandlers.add(executor);
    }

    @Nullable
    final T parent() {
        return parent;
    }

    @Nullable
    final Arguments<C> parentArguments() {
        return parentArguments;
    }

    final List<T> children() {
        return List.copyOf(children);
    }

    @SuppressWarnings("unchecked")
    final List<T> argumentChildren() {
        return argumentsList.stream()
                            .flatMap(x -> x.children()
                                           .stream())
                            .map(x -> (T) x)
                            .collect(java.util.stream.Collectors.toList());
    }

    final Prerequisite<C> prerequisite() {
        if (!inheritParentPrerequisite || parent == null) {
            return prerequisite;
        }

        return parent.prerequisite()
                     .and(prerequisite);
    }

    /**
     * The middlewares an execution of this command runs, outermost first: the cooldown check, the confirmation, the
     * inherited and own middlewares, and the start of the cooldown.
     */
    final List<CommandMiddleware<? super C>> middlewares() {
        List<CommandMiddleware<? super C>> result = new ArrayList<>();
        Cooldown cooldown = effectiveCooldown();
        Confirmation confirmation = effectiveConfirmation();
        if (cooldown != null) {
            result.add(preprocessMiddleware(cooldown::check));
        }
        if (confirmation != null) {
            result.add(preprocessMiddleware(confirmation::confirm));
        }
        result.addAll(inheritedMiddlewares());
        if (cooldown != null) {
            result.add((ctx, next) -> {
                cooldown.start(ctx);
                return next.proceed();
            });
        }
        return result;
    }

    final List<CommandMiddleware<? super C>> inheritedMiddlewares() {
        if (!inheritParentMiddleware || parent == null) {
            return middlewares;
        }
        List<CommandMiddleware<? super C>> result = new ArrayList<>(parent.inheritedMiddlewares());
        result.addAll(middlewares);
        return result;
    }

    @Nullable
    final Cooldown effectiveCooldown() {
        if (cooldown != null || !inheritParentMiddleware || parent == null) {
            return cooldown;
        }
        return parent.effectiveCooldown();
    }

    @Nullable
    final Confirmation effectiveConfirmation() {
        if (confirmation != null || !inheritParentMiddleware || parent == null) {
            return confirmation;
        }
        return parent.effectiveConfirmation();
    }

    final List<Arguments<C>> argumentsList() {
        return List.copyOf(argumentsList);
    }

    final List<CommandOption<?, C>> options() {
        return List.copyOf(options);
    }

    /**
     * Returns whether {@code a} and {@code b} are different options in one mutually exclusive group.
     */
    final boolean excludes(CommandOption<?, ?> a, CommandOption<?, ?> b) {
        return !a.equals(b) && exclusiveOptionGroups.stream()
                                                    .anyMatch(x -> x.contains(a) && x.contains(b));
    }

    final List<String> aliases() {
        return List.copyOf(aliases);
    }

    @Nullable
    final AsyncCommandExecutor<C> executor() {
        return executor;
    }

    final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers() {
        return List.copyOf(uncaughtExceptionHandlers);
    }

    public final void permission(@NotNull String node) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
    }

    public final void permission(@NotNull DefaultPermission defaultPermission) {
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
    }

    public final void permission(@NotNull DefaultPermission defaultPermission, @NotNull String description) {
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
        this.permissionDescription = Objects.requireNonNull(description);
    }

    public final void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
    }

    public final void permission(@NotNull String node,
                                 @NotNull DefaultPermission defaultPermission,
                                 @NotNull String description) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
        this.permissionDescription = Objects.requireNonNull(description);
    }

    public final void permissionDescription(@NotNull String description) {
        this.permissionDescription = Objects.requireNonNull(description);
    }

    public final String permissionName(@NotNull String prefix) {
        if (permissionNodeOverride != null) {
            return permissionNodeOverride;
        }
        return prefix + "." + permissionNameWithoutPrefix();
    }

    public final PermissionConfig permissionConfig(@NotNull String prefix) {
        return new PermissionConfig(permissionName(prefix), effectiveDefaultPermission(), permissionDescription);
    }

    final List<PermissionConfig> permissionConfigs(@NotNull String prefix) {
        List<PermissionConfig> configs = new ArrayList<>();
        configs.add(permissionConfig(prefix));
        argumentsList.stream()
                     .map(x -> x.permissionConfig(this, prefix))
                     .forEach(configs::add);
        children().forEach(x -> configs.addAll(x.permissionConfigs(prefix)));
        argumentChildren().forEach(x -> configs.addAll(x.permissionConfigs(prefix)));
        return configs;
    }

    String permissionNameWithoutPrefix() {
        if (parent == null) {
            return name;
        }
        return parent.permissionNameWithoutPrefix() + "." + name;
    }

    DefaultPermission effectiveDefaultPermission() {
        if (defaultPermissionOverride != null) {
            return defaultPermissionOverride;
        }
        if (parentArguments != null) {
            return parentArguments.effectiveDefaultPermission();
        }
        if (parent != null) {
            return parent.effectiveDefaultPermission();
        }
        return DefaultPermission.OP;
    }
}
