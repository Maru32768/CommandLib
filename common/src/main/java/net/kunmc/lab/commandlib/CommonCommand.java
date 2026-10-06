package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.branch.*;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.command.Prerequisite;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class CommonCommand<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    private final String name;
    private Function<C, String> description = ctx -> "";
    private String permissionNodeOverride = null;
    private DefaultPermission defaultPermissionOverride = null;
    private String permissionDescription = "";
    private T parent = null;
    private Arguments<C> parentArguments = null;
    private boolean inheritParentPrerequisite = true;
    private boolean inheritParentPreprocess = true;
    private final List<T> children = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();
    private final List<Arguments<C>> argumentsList = new ArrayList<>();
    private final List<CommandOption<?, C>> options = new ArrayList<>();
    private Prerequisite<C> prerequisite = ctx -> {
    };
    private Predicate<C> preprocess = ctx -> true;
    private CommandExecutor<C> executor;
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
        this.description = ctx -> Objects.requireNonNull(description);
    }

    public final void description(@NotNull Function<C, String> description) {
        this.description = Objects.requireNonNull(description);
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
    private void setParentFor(@NotNull Collection<? extends T> children, Arguments<C> parentArguments) {
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

    @SuppressWarnings("unchecked")
    public final <E> CommandOption<E, C> option(@NotNull CommandOption<E, ? super C> option) {
        Objects.requireNonNull(option);
        boolean duplicatedName = options.stream()
                                        .anyMatch(x -> x.name()
                                                        .equals(option.name()));
        if (duplicatedName) {
            throw new IllegalArgumentException("Duplicate option name: " + option.name());
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

    public final void disableParentPrerequisite() {
        this.inheritParentPrerequisite = false;
    }

    public final void disableParentPreprocess() {
        this.inheritParentPreprocess = false;
    }

    public final ArgumentBranch<C, T> argument(@NotNull Consumer<ArgumentBuilder<C>> buildArguments) {
        ArgumentBuilder<C> builder = new ArgumentBuilder<>();
        buildArguments.accept(builder);
        List<CommonArgument<?, C, ?>> builtArguments = builder.build();
        if (builtArguments.isEmpty()) {
            throw new IllegalArgumentException("ArgumentBuilder must add at least one argument.");
        }
        Arguments<C> arguments = addArguments(builtArguments, List.of());
        if (builder.executor() != null && builtArguments.get(builtArguments.size() - 1)
                                                        .executor() == null) {
            arguments.executor(builder.executor());
        }
        return new ArgumentBranch<>(delegateFor(arguments));
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
            public void execute(@Nullable CommandExecutor<C> action) {
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
        return new RequiredArgumentBranch<>(Objects.requireNonNull(extractor), this::execute, this::addChildren);
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
     * @param preprocess return false if cancel executing command.
     */
    public final void addPreprocess(@NotNull Predicate<C> preprocess) {
        this.preprocess = this.preprocess.and(Objects.requireNonNull(preprocess));
    }

    public final void execute(@Nullable CommandExecutor<C> executor) {
        this.executor = executor;
    }

    public final void addUncaughtExceptionHandler(UncaughtExceptionHandler<C> executor) {
        this.uncaughtExceptionHandlers.add(executor);
    }

    final T parent() {
        return parent;
    }

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

    final Predicate<C> preprocess() {
        if (!inheritParentPreprocess || parent == null) {
            return preprocess;
        }

        return parent.preprocess()
                     .and(preprocess);
    }

    final List<Arguments<C>> argumentsList() {
        return List.copyOf(argumentsList);
    }

    final List<CommandOption<?, C>> options() {
        return List.copyOf(options);
    }

    final List<String> aliases() {
        return List.copyOf(aliases);
    }

    final CommandExecutor<C> executor() {
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
            return parentArguments.effectiveDefaultPermission(parent);
        }
        if (parent != null) {
            return parent.effectiveDefaultPermission();
        }
        return DefaultPermission.OP;
    }
}
