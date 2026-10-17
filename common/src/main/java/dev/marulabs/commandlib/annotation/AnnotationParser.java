package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.Captions;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.Confirmation;
import dev.marulabs.commandlib.ContextKey;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.branch.ArgumentBranch;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds a command from a class annotated with {@link Command}, on top of the fluent API: the result is an ordinary
 * command, with the same help, permissions, options and behavior as the same command written fluently. Each platform
 * has an {@code AnnotatedCommands} class that returns a parser with its argument types registered.
 *
 * <pre>
 * &#64;Command("ban")
 * &#64;Description("Ban a player")
 * public final class BanCommand {
 *     &#64;Execute
 *     void ban(CommandContext ctx, &#64;Flag(shortName = 's') boolean silent, Player target, &#64;Greedy &#64;Default("Banned") String reason) {
 *         target.kickPlayer(reason);
 *     }
 * }
 *
 * Command ban = AnnotatedCommands.parse(new BanCommand());
 * </pre>
 *
 * Each parameter of an {@link Execute} or {@link Subcommand} method is, in this order:
 * <ol>
 * <li>the context, when its type is a {@link CommonCommandContext} the platform's context is assignable to;</li>
 * <li>a context value registered with {@link #registerContextValue}, when annotated {@link Sender};</li>
 * <li>an option, when annotated {@link Flag} or {@link Option};</li>
 * <li>an argument made by the factory registered for its type with {@link #registerArgument}; a {@code List} last
 * parameter is a variadic argument of its element type;</li>
 * <li>a context value registered for its type, such as the sender.</li>
 * </ol>
 * A context value is read with {@link CommonCommandContext#get(Class)}, so the method gets what the command's
 * provider gives or what a middleware put for the type, the same value fluent code of the command reads. The parsed
 * command provides the context values registered with an extractor, and its fields annotated {@link Dependency} are
 * set to the instances registered with {@link #registerDependency}.
 * Methods are added in the order of their subcommand path, then their name, since reflection does not report the
 * order of the source, so the generated help lists subcommands and branches in that order.
 * <p>
 * Parsing throws {@link IllegalArgumentException} naming the method and the parameter for a signature it cannot map.
 * The arguments' own constraints, such as optional arguments being last, are checked when the command is registered,
 * as for fluent commands.
 *
 * @param <C> the platform's context
 * @param <T> the platform's command
 */
public final class AnnotationParser<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    private static final List<Class<? extends Annotation>> PARAMETER_ANNOTATIONS = List.of(Optional.class,
                                                                                           Default.class,
                                                                                           Range.class,
                                                                                           Suggest.class,
                                                                                           Name.class,
                                                                                           Greedy.class,
                                                                                           Description.class,
                                                                                           Flag.class,
                                                                                           Option.class,
                                                                                           Permission.class,
                                                                                           Requires.class,
                                                                                           Variadic.class,
                                                                                           // Last, so that a @Sender parameter can allow it.
                                                                                           Sender.class);
    // The annotations that configure a command, and those that describe an argument branch on an @Execute method.
    private static final List<Class<? extends Annotation>> COMMAND_ANNOTATIONS = List.of(Confirm.class,
                                                                                         Cooldown.class,
                                                                                         Condition.class,
                                                                                         Middleware.class,
                                                                                         OptionsAnywhere.class,
                                                                                         MutuallyExclusive.class,
                                                                                         Examples.class,
                                                                                         CaptionBundle.class);
    private static final List<Class<? extends Annotation>> BRANCH_ANNOTATIONS = List.of(Description.class,
                                                                                        Permission.class,
                                                                                        VisibleWhen.class);

    // The values of an omitted @Optional argument and of an @Option without @Default.
    private static final Map<Class<?>, Object> EMPTY_VALUES = Map.of(Boolean.class,
                                                                     false,
                                                                     Integer.class,
                                                                     0,
                                                                     Long.class,
                                                                     0L,
                                                                     Float.class,
                                                                     0F,
                                                                     Double.class,
                                                                     0D,
                                                                     String.class,
                                                                     "");

    private final Class<C> contextType;
    private final Function<String, ? extends T> commandFactory;
    private final Map<Class<?>, ArgumentFactory<C>> argumentFactories = new HashMap<>();
    // Every type registered as a context value, and the extractors of those the parsed command provides.
    private final Set<Class<?>> contextTypes = new HashSet<>();
    private final Map<Class<?>, Extractor<C, ?>> contextValues = new LinkedHashMap<>();
    private final Map<Class<?>, Object> dependencies = new HashMap<>();
    private final Map<String, Prerequisite<C>> conditions = new HashMap<>();
    private final Map<String, Predicate<? super C>> visibilities = new HashMap<>();
    private final Map<String, CommandMiddleware<? super C>> middlewares = new HashMap<>();
    // In registration order, which is the order the handlers apply in.
    private final Map<Class<? extends Annotation>, RegisteredHandler<?, ?, C>> annotationHandlers = new LinkedHashMap<>();

    /**
     * Creates a parser without argument types; prefer the platform's {@code AnnotatedCommands.parser()}.
     *
     * @param contextType    the platform's context, given to parameters of a type it is assignable to
     * @param commandFactory creates a command with the given name, such as {@code Command::new}
     */
    public AnnotationParser(@NotNull Class<C> contextType, @NotNull Function<String, ? extends T> commandFactory) {
        this.contextType = Objects.requireNonNull(contextType);
        this.commandFactory = Objects.requireNonNull(commandFactory);
    }

    /**
     * Makes parameters of {@code type} arguments created by {@code factory}, replacing the factory registered for it
     * before. A primitive type stands for its wrapper, and {@code Enum.class} for every enum without its own factory.
     */
    public AnnotationParser<C, T> registerArgument(@NotNull Class<?> type, @NotNull ArgumentFactory<C> factory) {
        argumentFactories.put(box(type), Objects.requireNonNull(factory));
        return this;
    }

    /**
     * Maps the value types every platform has to the platform's arguments made by the given constructors:
     * {@code String} ({@code greedyString} when annotated {@link Greedy}), {@code int}, {@code long}, {@code float} and
     * {@code double} (bounded by {@link Range}, or by the limits of the type without it), {@code boolean}, enums and
     * {@link Duration}.
     */
    public AnnotationParser<C, T> registerStandardArguments(@NotNull Function<String, ? extends CommonArgument<String, C, ?>> string,
                                                            @NotNull Function<String, ? extends CommonArgument<String, C, ?>> greedyString,
                                                            @NotNull NumberArgumentFactory<Integer, C> integer,
                                                            @NotNull NumberArgumentFactory<Long, C> longArgument,
                                                            @NotNull NumberArgumentFactory<Float, C> floatArgument,
                                                            @NotNull NumberArgumentFactory<Double, C> doubleArgument,
                                                            @NotNull Function<String, ? extends CommonArgument<Boolean, C, ?>> bool,
                                                            @NotNull EnumArgumentFactory<C> enumArgument,
                                                            @NotNull Function<String, ? extends CommonArgument<Duration, C, ?>> duration) {
        registerArgument(String.class,
                         p -> (p.annotation(Greedy.class) == null ? string : greedyString).apply(p.name()));
        registerArgument(Integer.class, p -> integer.create(p.name(), (int) min(p), (int) max(p)));
        registerArgument(Long.class, p -> longArgument.create(p.name(), (long) min(p), (long) max(p)));
        registerArgument(Float.class,
                         p -> floatArgument.create(p.name(),
                                                   (float) Math.max(min(p), -Float.MAX_VALUE),
                                                   (float) Math.min(max(p), Float.MAX_VALUE)));
        registerArgument(Double.class, p -> doubleArgument.create(p.name(), min(p), max(p)));
        registerArgument(Boolean.class, p -> bool.apply(p.name()));
        registerArgument(Enum.class, p -> createEnumArgument(enumArgument, p));
        return registerArgument(Duration.class, p -> duration.apply(p.name()));
    }

    // Without @Range the bounds are its defaults, which the casts above clamp to the limits of each type.
    private static double min(AnnotatedParameter parameter) {
        Range range = parameter.annotation(Range.class);
        return range == null ? -Double.MAX_VALUE : range.min();
    }

    private static double max(AnnotatedParameter parameter) {
        Range range = parameter.annotation(Range.class);
        return range == null ? Double.MAX_VALUE : range.max();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <C extends CommonCommandContext<?, ?>> CommonArgument<?, C, ?> createEnumArgument(EnumArgumentFactory<C> factory,
                                                                                                     AnnotatedParameter parameter) {
        return factory.create(parameter.name(), (Class) parameter.type());
    }

    /**
     * Creates a numeric argument with inclusive bounds, such as {@code IntegerArgument::new}.
     */
    @FunctionalInterface
    public interface NumberArgumentFactory<N extends Number, C extends CommonCommandContext<?, ?>> {
        @NotNull
        CommonArgument<N, C, ?> create(@NotNull String name, @NotNull N min, @NotNull N max);
    }

    /**
     * Creates an argument of the constants of an enum, such as {@code EnumArgument::new}.
     */
    @FunctionalInterface
    public interface EnumArgumentFactory<C extends CommonCommandContext<?, ?>> {
        @NotNull
        <E extends Enum<E>> CommonArgument<E, C, ?> create(@NotNull String name, @NotNull Class<E> type);
    }

    /**
     * Resolves parameters of {@code type} from the context with {@code extractor}: those annotated {@link Sender},
     * those whose type has no argument factory, and the value of an omitted {@link Optional} argument of the type. A
     * {@link CommandPrerequisiteException} thrown by the extractor fails the command with its message.
     * <p>
     * The parsed command provides the value with {@link CommonCommand#provide(Class, Extractor)}, so the extractor
     * runs at most once per execution, a middleware can replace the value with
     * {@link CommonCommandContext#put(Class, Object)}, and fluent code of the command reads it with
     * {@link CommonCommandContext#get(Class)}. This is how to map the sender to a type of your own:
     * {@code registerContextValue(GamePlayer.class, ctx -> games.of(ctx.getActor()))}.
     */
    public <V> AnnotationParser<C, T> registerContextValue(@NotNull Class<V> type,
                                                           @NotNull Extractor<C, ? extends V> extractor) {
        contextTypes.add(box(type));
        contextValues.put(box(type), Objects.requireNonNull(extractor));
        return this;
    }

    /**
     * Resolves parameters of {@code type} as {@link #registerContextValue(Class, Extractor)} does, from a value
     * something else gives the context: a middleware that puts it, or the provider of a command the parsed command is
     * added to. An execution that reaches the method without the value fails with an {@link IllegalStateException}.
     * It replaces an extractor registered for the type.
     */
    public AnnotationParser<C, T> registerContextValue(@NotNull Class<?> type) {
        contextTypes.add(box(type));
        contextValues.remove(box(type));
        return this;
    }

    /**
     * Registers {@code instance} as the dependency of {@code type}: fields of the type annotated {@link Dependency} are
     * set to it when a class is parsed, and it is a context value of the type
     * ({@link #registerContextValue(Class, Extractor)}), so methods can take it as a parameter and fluent code of the
     * command can read it with {@link CommonCommandContext#get(Class)}.
     */
    public <V> AnnotationParser<C, T> registerDependency(@NotNull Class<V> type, @NotNull V instance) {
        Objects.requireNonNull(instance);
        dependencies.put(box(type), instance);
        return registerContextValue(type, ctx -> instance);
    }

    /**
     * Registers {@code prerequisite} under {@code name}, so that {@link Condition @Condition("name")} on a class or a
     * subcommand method adds it to the command, replacing the one registered before under the name. A fluent command
     * adds the same {@code Prerequisite} constant with {@code addPrerequisite(...)}.
     */
    public AnnotationParser<C, T> registerCondition(@NotNull String name, @NotNull Prerequisite<C> prerequisite) {
        conditions.put(Objects.requireNonNull(name), Objects.requireNonNull(prerequisite));
        return this;
    }

    /**
     * Registers {@code predicate} under {@code name}, so that {@link VisibleWhen @VisibleWhen("name")} shows the
     * command or the branch only to senders for whom it holds, as {@code visibleWhen(predicate)} does. It replaces the
     * predicate registered before under the name.
     */
    public AnnotationParser<C, T> registerVisibility(@NotNull String name, @NotNull Predicate<? super C> predicate) {
        visibilities.put(Objects.requireNonNull(name), Objects.requireNonNull(predicate));
        return this;
    }

    /**
     * Registers {@code middleware} under {@code name}, so that {@link Middleware @Middleware("name")} on a class or a
     * subcommand method adds it to the command, as {@code addMiddleware(middleware)} does. It replaces the middleware
     * registered before under the name.
     */
    public AnnotationParser<C, T> registerMiddleware(@NotNull String name,
                                                     @NotNull CommandMiddleware<? super C> middleware) {
        middlewares.put(Objects.requireNonNull(name), Objects.requireNonNull(middleware));
        return this;
    }

    /**
     * Applies {@code handler} to every argument parameter annotated with {@code annotationType} whose value type is a
     * {@code valueType}. Handlers apply in registration order, after the factory and {@link Suggest}, and before the
     * argument is made variadic or optional, so a handler of a {@code List} parameter configures each value. Parsing
     * fails for the annotation on a parameter that is no argument: the context, a {@link Sender} or context value, a
     * {@link Flag}, or an {@link Option} of a type that needs no argument factory.
     */
    public <A extends Annotation, V> AnnotationParser<C, T> registerAnnotation(@NotNull Class<A> annotationType,
                                                                               @NotNull Class<V> valueType,
                                                                               @NotNull ArgumentAnnotationHandler<A, V, C> handler) {
        annotationHandlers.put(Objects.requireNonNull(annotationType),
                               new RegisteredHandler<>(annotationType,
                                                       box(valueType),
                                                       Objects.requireNonNull(handler)));
        return this;
    }

    /**
     * Builds the command that {@code instance}'s class declares. The methods are called on {@code instance}.
     *
     * @throws IllegalArgumentException if the class is not annotated {@link Command} or a method cannot be mapped
     */
    @NotNull
    public T parse(@NotNull Object instance) {
        return new Parse(Objects.requireNonNull(instance)).run();
    }

    private final class Parse {
        private final Object instance;
        private final Class<?> type;
        private final T root;
        // The commands by their path below the root, separated by spaces; the root is "".
        private final Map<String, T> commands = new HashMap<>();
        // The argument branches of path placeholders by their path, such as "<team>".
        private final Map<String, PathBranch> branches = new HashMap<>();
        private final IdentityHashMap<T, CommandState> states = new IdentityHashMap<>();
        // What needs every option of the commands: @Requires and @MutuallyExclusive.
        private final List<Runnable> deferred = new ArrayList<>();

        Parse(Object instance) {
            this.instance = instance;
            this.type = instance.getClass();
            Command command = type.getAnnotation(Command.class);
            if (command == null) {
                throw new IllegalArgumentException(type.getName() + " is not annotated with @Command");
            }
            this.root = commandFactory.apply(command.value());
            if (command.aliases().length > 0) {
                root.addAliases(Arrays.asList(command.aliases()));
            }
        }

        T run() {
            injectDependencies();
            contextValues.forEach((x, extractor) -> provide(root, x, extractor));
            commands.put("", root);
            configure(root, type, "class " + type.getSimpleName(), new String[0]);

            List<Method> methods = methods(this::isCommandMethod).stream()
                                                                 .sorted(Comparator.comparing((Method x) -> pathOf(x))
                                                                                   .thenComparing(Method::getName)
                                                                                   .thenComparingInt(Method::getParameterCount)
                                                                                   .thenComparing(Method::toGenericString))
                                                                 .collect(Collectors.toList());
            methods.forEach(this::addMethod);
            deferred.forEach(Runnable::run);
            return root;
        }

        /**
         * The methods of the class and its superclasses that pass {@code filter}, an overridden method once: its most
         * derived declaration that passes, whose annotations apply. A call runs the override in any case.
         */
        private List<Method> methods(Predicate<Method> filter) {
            Map<List<Object>, Method> methods = new LinkedHashMap<>();
            for (Class<?> x = type; x != null && x != Object.class; x = x.getSuperclass()) {
                for (Method method : x.getDeclaredMethods()) {
                    if (method.isSynthetic() || !filter.test(method)) {
                        continue;
                    }
                    int modifiers = method.getModifiers();
                    // A private or static method does not override, and nothing overrides it.
                    List<Object> key = Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers) ? List.of(method)
                            : List.of(method.getName(), Arrays.asList(method.getParameterTypes()));
                    methods.putIfAbsent(key, method);
                }
            }
            return new ArrayList<>(methods.values());
        }

        private void injectDependencies() {
            for (Class<?> x = type; x != null && x != Object.class; x = x.getSuperclass()) {
                for (Field field : x.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(Dependency.class)) {
                        continue;
                    }
                    String name = x.getSimpleName() + "." + field.getName();
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                        throw new IllegalArgumentException(name + " has @Dependency, but is static or final");
                    }
                    Object dependency = dependencies.get(box(field.getType()));
                    if (dependency == null) {
                        throw new IllegalArgumentException(name
                                + " has @Dependency, but no dependency of type " + field.getType()
                                                                                        .getName()
                                + " is registered; register one with registerDependency");
                    }
                    try {
                        field.setAccessible(true);
                        field.set(instance, dependency);
                    } catch (IllegalAccessException | RuntimeException e) {
                        throw new IllegalArgumentException(name + " is not accessible", e);
                    }
                }
            }
        }

        private boolean isCommandMethod(Method method) {
            if (method.isAnnotationPresent(Execute.class) || method.isAnnotationPresent(Subcommand.class)) {
                return true;
            }
            for (Class<? extends Annotation> x : allOf(BRANCH_ANNOTATIONS, COMMAND_ANNOTATIONS)) {
                if (has(method, x)) {
                    throw error(method, "has @" + x.getSimpleName() + " but neither @Execute nor @Subcommand");
                }
            }
            return false;
        }

        private void rejectCommandAnnotations(Method method, String reason) {
            for (Class<? extends Annotation> x : COMMAND_ANNOTATIONS) {
                if (has(method, x)) {
                    throw error(method, "has @" + x.getSimpleName() + "; " + reason);
                }
            }
        }

        private void addMethod(Method method) {
            Execute execute = method.getAnnotation(Execute.class);
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            if (execute != null && subcommand != null) {
                throw error(method, "has both @Execute and @Subcommand");
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && returnType != int.class
                    && !CompletionStage.class.isAssignableFrom(returnType)) {
                throw error(method, "must return void, int or a CompletionStage, not " + returnType.getName());
            }
            Target target;
            if (subcommand == null) {
                target = new Target(root, null, Map.of());
                rejectCommandAnnotations(method, "put it on the class");
            } else {
                target = commandAt(method, subcommand);
                if (target.branch == null) {
                    configure(target.command, method, describe(method), subcommand.aliases());
                } else {
                    rejectCommandAnnotations(method,
                                             "its path ends in a placeholder, so it describes an argument branch");
                    if (subcommand.aliases().length > 0) {
                        throw error(method, "has aliases, but its path ends in a placeholder");
                    }
                }
            }
            T command = target.command;
            try {
                method.setAccessible(true);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(describe(method) + " is not accessible", e);
            }

            List<CommonArgument<?, C, ?>> arguments = new ArrayList<>();
            List<Extractor<C, ?>> resolvers = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                CommonArgument<?, C, ?> placeholder = target.placeholders.get(parameter);
                resolvers.add(placeholder != null ? ctx -> ctx.getArgument(placeholder)
                        : resolve(method, parameter, command, arguments));
            }
            AsyncCommandExecutor<C> executor = executor(method, resolvers);

            PathBranch pathBranch = target.branch;
            if (pathBranch != null) {
                if (!arguments.isEmpty()) {
                    throw error(method,
                                "takes arguments after the placeholders of its path; put them in the path or in a "
                                        + "subcommand");
                }
                if (pathBranch.hasExecutor) {
                    throw error(method, "executes '" + pathOf(method) + "', as another method does");
                }
                pathBranch.hasExecutor = true;
                pathBranch.branch.executeAsync(executor);
                describeBranch(method, pathBranch.branch);
                return;
            }
            if (arguments.isEmpty()) {
                if (execute != null && BRANCH_ANNOTATIONS.stream()
                                                         .anyMatch(x -> has(method, x))) {
                    throw error(method,
                                "has no arguments, so its @Description, @Permission and @VisibleWhen would describe "
                                        + "the command; put them on the class");
                }
                CommandState state = state(command);
                if (state.hasExecutor) {
                    throw error(method, "executes '" + pathOf(method) + "' without arguments, as another method does");
                }
                state.hasExecutor = true;
                command.executeAsync(executor);
                return;
            }
            ArgumentBranch<C, T> branch = command.argument(arguments)
                                                 .executeAsync(executor);
            if (execute != null) {
                describeBranch(method, branch);
            }
        }

        private void describeBranch(Method method, ArgumentBranch<C, T> branch) {
            Description description = method.getAnnotation(Description.class);
            if (description != null) {
                Function<C, String> localized = localized(description);
                if (localized != null) {
                    branch.description(localized);
                } else {
                    branch.description(description.value());
                }
            }
            Permission permission = method.getAnnotation(Permission.class);
            if (permission != null) {
                applyPermission(describe(method),
                                permission,
                                branch::permission,
                                branch::permission,
                                branch::permissionDescription);
            }
            VisibleWhen visibleWhen = method.getAnnotation(VisibleWhen.class);
            if (visibleWhen != null) {
                branch.visibleWhen(named(visibilities,
                                         visibleWhen.value(),
                                         describe(method),
                                         "@VisibleWhen",
                                         "visibility predicate",
                                         "registerVisibility"));
            }
        }

        /**
         * The command of a {@code @Subcommand} path, creating the commands and the argument branches of its
         * placeholders that are missing, and the branch when the path ends in placeholders.
         */
        private Target commandAt(Method method, Subcommand subcommand) {
            List<String> segments = segments(subcommand);
            if (segments.isEmpty()) {
                throw error(method, "has an empty @Subcommand; use @Execute for the command itself");
            }
            for (String segment : segments) {
                if (!isPlaceholder(segment) && (segment.contains("<") || segment.contains(">"))) {
                    throw error(method,
                                "has '" + segment + "' in its @Subcommand path; a placeholder is one name in angle "
                                        + "brackets, such as <team>");
                }
            }
            T current = root;
            PathBranch branch = null;
            Map<Parameter, CommonArgument<?, C, ?>> placeholders = new HashMap<>();
            List<String> path = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                String segment = segments.get(i);
                if (isPlaceholder(segment)) {
                    // Adjacent placeholders form one branch, as argument(a, b).child(...) does.
                    List<Parameter> parameters = new ArrayList<>();
                    for (; i < segments.size() && isPlaceholder(segments.get(i)); i++) {
                        String name = segments.get(i);
                        parameters.add(placeholderParameter(method, name.substring(1, name.length() - 1)));
                        path.add(name);
                    }
                    i--;
                    branch = branchAt(method, current, String.join(" ", path), parameters);
                    for (int j = 0; j < parameters.size(); j++) {
                        placeholders.put(parameters.get(j), branch.arguments.get(j));
                    }
                    continue;
                }
                path.add(segment);
                T parent = current;
                PathBranch parentBranch = branch;
                current = commands.computeIfAbsent(String.join(" ", path), x -> {
                    T child = commandFactory.apply(segment);
                    if (parentBranch != null) {
                        parentBranch.branch.child(child);
                    } else {
                        parent.addChildren(List.of(child));
                    }
                    return child;
                });
                branch = null;
            }
            return new Target(current, branch, placeholders);
        }

        private PathBranch branchAt(Method method, T command, String path, List<Parameter> parameters) {
            List<Set<Object>> keys = parameters.stream()
                                               .map(AnnotationParser::declaration)
                                               .collect(Collectors.toList());
            PathBranch branch = branches.get(path);
            if (branch != null) {
                if (!branch.keys.equals(keys)) {
                    throw error(method, "declares the arguments of '" + path + "' differently from another method");
                }
                return branch;
            }
            List<CommonArgument<?, C, ?>> arguments = new ArrayList<>();
            for (Parameter parameter : parameters) {
                boolean variadic = parameter.getType() == List.class;
                Class<?> valueType = variadic ? elementType(method, parameter) : box(parameter.getType());
                ArgumentFactory<C> factory = factory(valueType);
                if (factory == null) {
                    throw error(method,
                                parameter,
                                "has no argument type for " + valueType.getName()
                                        + "; register one with registerArgument");
                }
                arguments.add(argument(method, parameter, factory, valueType, variadic));
            }
            branch = new PathBranch(command.argument(arguments), arguments, keys);
            branches.put(path, branch);
            return branch;
        }

        private Parameter placeholderParameter(Method method, String name) {
            for (Parameter parameter : method.getParameters()) {
                Class<?> rawType = parameter.getType();
                boolean isContext = CommonCommandContext.class.isAssignableFrom(rawType)
                        && rawType.isAssignableFrom(contextType);
                if (isContext || has(parameter, Sender.class) || has(parameter, Flag.class)
                        || has(parameter, Option.class)) {
                    continue;
                }
                Name declared = parameter.getAnnotation(Name.class);
                String parameterName = declared != null ? declared.value()
                        : parameter.isNamePresent() ? parameter.getName() : null;
                if (name.equals(parameterName)) {
                    return parameter;
                }
            }
            throw error(method,
                        "has <" + name + "> in its @Subcommand path, but no argument parameter named " + name
                                + " (compile with -parameters or annotate it with @Name)");
        }

        /**
         * Applies the command annotations of {@code element}, which only one element of a command may give.
         */
        private void configure(T command, AnnotatedElement element, String source, String[] aliases) {
            if (aliases.length == 0 && allOf(BRANCH_ANNOTATIONS, COMMAND_ANNOTATIONS).stream()
                                                                                     .noneMatch(x -> has(element, x))) {
                return;
            }
            Description description = element.getAnnotation(Description.class);
            Permission permission = element.getAnnotation(Permission.class);
            Confirm confirm = element.getAnnotation(Confirm.class);
            Cooldown cooldown = element.getAnnotation(Cooldown.class);
            Condition condition = element.getAnnotation(Condition.class);
            VisibleWhen visibleWhen = element.getAnnotation(VisibleWhen.class);
            Middleware middleware = element.getAnnotation(Middleware.class);
            Examples examples = element.getAnnotation(Examples.class);
            CaptionBundle captionBundle = element.getAnnotation(CaptionBundle.class);
            CommandState state = state(command);
            if (state.configuredBy != null) {
                throw new IllegalArgumentException(source + " configures '" + command.name() + "', which "
                        + state.configuredBy + " already configures; give the command annotations on one method");
            }
            state.configuredBy = source;
            if (aliases.length > 0) {
                command.addAliases(Arrays.asList(aliases));
            }
            if (captionBundle != null) {
                command.captions(Captions.create()
                                         .bundle(captionBundle.value(), type.getClassLoader()));
            }
            if (description != null) {
                Function<C, String> localized = localized(description);
                if (localized != null) {
                    command.description(localized);
                } else {
                    command.description(description.value());
                }
            }
            if (examples != null) {
                command.examples(examples.value());
            }
            if (element.isAnnotationPresent(OptionsAnywhere.class)) {
                command.optionsAnywhere();
            }
            if (visibleWhen != null) {
                command.visibleWhen(named(visibilities,
                                          visibleWhen.value(),
                                          source,
                                          "@VisibleWhen",
                                          "visibility predicate",
                                          "registerVisibility"));
            }
            if (middleware != null) {
                for (String name : middleware.value()) {
                    command.addMiddleware(named(middlewares,
                                                name,
                                                source,
                                                "@Middleware",
                                                "middleware",
                                                "registerMiddleware"));
                }
            }
            for (MutuallyExclusive group : element.getAnnotationsByType(MutuallyExclusive.class)) {
                deferred.add(() -> mutuallyExclusive(command, source, group.value()));
            }
            if (permission != null) {
                applyPermission(source,
                                permission,
                                command::permission,
                                command::permission,
                                command::permissionDescription);
            }
            if (confirm != null) {
                command.confirm(Confirmation.within(Duration.ofSeconds(confirm.seconds())));
            }
            if (cooldown != null) {
                command.cooldown(Duration.of(cooldown.value(),
                                             cooldown.unit()
                                                     .toChronoUnit()));
            }
            if (condition != null) {
                for (String name : condition.value()) {
                    command.addPrerequisite(named(conditions,
                                                  name,
                                                  source,
                                                  "@Condition",
                                                  "condition",
                                                  "registerCondition"));
                }
            }
        }

        private <V> V named(Map<String, V> registry,
                            String name,
                            String source,
                            String annotation,
                            String kind,
                            String register) {
            V value = registry.get(name);
            if (value == null) {
                throw new IllegalArgumentException(source + " has " + annotation + "(\"" + name + "\"), but no " + kind
                        + " of that name is registered; register one with " + register);
            }
            return value;
        }

        @Nullable
        private Function<C, String> localized(Description description) {
            if (description.caption()
                           .isEmpty()) {
                return null;
            }
            Caption caption = Caption.of(description.caption(), description.value());
            return ctx -> ctx.caption(caption);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void mutuallyExclusive(T command, String source, String[] names) {
            List<CommandOption<?, C>> group = new ArrayList<>();
            for (String name : names) {
                RegisteredOption option = state(command).options.get(name);
                if (option == null) {
                    throw new IllegalArgumentException(source + " has @MutuallyExclusive with --" + name + ", but '"
                            + command.name() + "' has no option of that name");
                }
                group.add(option.option);
            }
            command.mutuallyExclusive(group.toArray(new CommandOption[0]));
        }

        private void applyPermission(String source,
                                     Permission permission,
                                     Consumer<String> node,
                                     Consumer<DefaultPermission> defaultPermission,
                                     Consumer<String> description) {
            if (!permission.value()
                           .isEmpty()) {
                node.accept(permission.value());
            }
            DefaultPermission[] defaults = permission.defaultPermission();
            if (defaults.length > 1) {
                throw new IllegalArgumentException(source + " gives more than one defaultPermission");
            }
            if (defaults.length == 1) {
                defaultPermission.accept(defaults[0]);
            }
            if (!permission.description()
                           .isEmpty()) {
                description.accept(permission.description());
            }
        }

        private Extractor<C, ?> resolve(Method method,
                                        Parameter parameter,
                                        T command,
                                        List<CommonArgument<?, C, ?>> arguments) {
            Class<?> rawType = parameter.getType();
            if (CommonCommandContext.class.isAssignableFrom(rawType) && rawType.isAssignableFrom(contextType)) {
                reject(method, parameter, "the context", PARAMETER_ANNOTATIONS);
                rejectRegisteredAnnotations(method, parameter, "the context");
                return ctx -> ctx;
            }
            if (parameter.isAnnotationPresent(Sender.class)) {
                reject(method,
                       parameter,
                       "a @Sender parameter",
                       PARAMETER_ANNOTATIONS.subList(0, PARAMETER_ANNOTATIONS.size() - 1));
                rejectRegisteredAnnotations(method, parameter, "a @Sender parameter");
                if (!contextTypes.contains(box(rawType))) {
                    throw error(method,
                                parameter,
                                "has no context value for " + rawType.getName()
                                        + "; register one with registerContextValue");
                }
                return contextValue(box(rawType));
            }
            if (parameter.isAnnotationPresent(Flag.class) || parameter.isAnnotationPresent(Option.class)) {
                CommandOption<?, C> option = option(method, parameter, command);
                return ctx -> ctx.getOption(option);
            }

            boolean variadic = rawType == List.class;
            Class<?> valueType = variadic ? elementType(method, parameter) : box(rawType);
            ArgumentFactory<C> factory = factory(valueType);
            if (factory == null) {
                if (variadic || !contextTypes.contains(valueType)) {
                    throw error(method,
                                parameter,
                                "has no argument type for " + valueType.getName()
                                        + "; register one with registerArgument");
                }
                reject(method, parameter, "a context value", PARAMETER_ANNOTATIONS);
                rejectRegisteredAnnotations(method, parameter, "a context value");
                return contextValue(valueType);
            }
            CommonArgument<?, C, ?> argument = argument(method, parameter, factory, valueType, variadic);
            arguments.add(argument);
            return ctx -> ctx.getArgument(argument);
        }

        @Nullable
        private ArgumentFactory<C> factory(Class<?> valueType) {
            ArgumentFactory<C> factory = argumentFactories.get(valueType);
            return factory == null && valueType.isEnum() ? argumentFactories.get(Enum.class) : factory;
        }

        private CommonArgument<?, C, ?> argument(Method method,
                                                 Parameter parameter,
                                                 ArgumentFactory<C> factory,
                                                 Class<?> valueType,
                                                 boolean variadic) {
            reject(method, parameter, "an argument", List.of(Description.class, Permission.class, Requires.class));
            return argument(method, parameter, factory, valueType, variadic, argumentName(method, parameter));
        }

        /**
         * The argument of a parameter, or of the value of an option when {@code name} is the option's.
         */
        private CommonArgument<?, C, ?> argument(Method method,
                                                 Parameter parameter,
                                                 ArgumentFactory<C> factory,
                                                 Class<?> valueType,
                                                 boolean variadic,
                                                 String name) {
            if (parameter.isAnnotationPresent(Range.class) && !Number.class.isAssignableFrom(valueType)) {
                throw error(method, parameter, "has @Range, but " + valueType.getName() + " is not a number");
            }
            checkRange(method, parameter, valueType);
            if (parameter.isAnnotationPresent(Greedy.class) && valueType != String.class) {
                throw error(method, parameter, "has @Greedy, but " + valueType.getName() + " is not a String");
            }
            Variadic maximum = parameter.getAnnotation(Variadic.class);
            if (maximum != null && !variadic) {
                throw error(method, parameter, "has @Variadic, which applies to a List argument");
            }
            CommonArgument<?, C, ?> argument = factory.create(new AnnotatedParameter(name, valueType, parameter));
            Suggest suggest = parameter.getAnnotation(Suggest.class);
            if (suggest != null) {
                argument.setSuggestionProvider(suggestions(method, parameter, suggest));
            }
            for (RegisteredHandler<?, ?, C> handler : annotationHandlers.values()) {
                Annotation annotation = parameter.getAnnotation(handler.annotationType);
                if (annotation == null) {
                    continue;
                }
                if (!handler.valueType.isAssignableFrom(valueType)) {
                    throw error(method,
                                parameter,
                                "has @" + handler.annotationType.getSimpleName() + ", which applies to "
                                        + handler.valueType.getName() + ", not " + valueType.getName());
                }
                argument = handler.apply(annotation, argument);
                if (!argument.name()
                             .equals(name)) {
                    throw error(method,
                                parameter,
                                "the handler of @" + handler.annotationType.getSimpleName()
                                        + " returned an argument named '" + argument.name() + "' instead of '" + name
                                        + "'");
                }
            }
            if (variadic) {
                argument = maximum == null ? argument.variadic() : argument.variadic(maximum.max());
            }

            Default defaultValue = parameter.getAnnotation(Default.class);
            if (defaultValue != null) {
                if (variadic) {
                    throw error(method,
                                parameter,
                                "is variadic and cannot take @Default; @Optional gives an empty list");
                }
                Object value = parseDefault(method, parameter, valueType, defaultValue.value());
                makeOptional(argument, ctx -> value);
            } else if (parameter.isAnnotationPresent(Optional.class)) {
                Extractor<C, ?> fromContext = !variadic && contextTypes.contains(valueType) ? contextValue(valueType)
                        : null;
                Object empty = variadic ? List.of() : emptyValue(valueType);
                if (fromContext != null) {
                    makeOptional(argument, fromContext);
                } else if (empty != null) {
                    makeOptional(argument, ctx -> empty);
                } else {
                    throw error(method,
                                parameter,
                                "has no value for an omitted " + valueType.getName()
                                        + "; give @Default or register a context value for the type");
                }
            }
            return argument;
        }

        // Integral types would truncate a fractional bound, so @Range(min = 1.5) int would accept 1.
        private void checkRange(Method method, Parameter parameter, Class<?> valueType) {
            Range range = parameter.getAnnotation(Range.class);
            if (range == null) {
                return;
            }
            if (range.min() > range.max()) {
                throw error(method, parameter, "has @Range with min " + range.min() + " above max " + range.max());
            }
            boolean integral = valueType == Integer.class || valueType == Long.class || valueType == Short.class
                    || valueType == Byte.class;
            if (integral && (range.min() != Math.rint(range.min()) || range.max() != Math.rint(range.max()))) {
                throw error(method,
                            parameter,
                            "has @Range with a fractional bound, but " + valueType.getSimpleName()
                                    + " takes whole numbers");
            }
        }

        private SuggestionProvider<C> suggestions(Method method, Parameter parameter, Suggest suggest) {
            List<String> values = List.of(suggest.value());
            if (suggest.method()
                       .isEmpty()) {
                if (values.isEmpty()) {
                    throw error(method, parameter, "has @Suggest without values or a method");
                }
                return builder -> values.forEach(builder::suggest);
            }
            List<Method> candidates = methods(x -> x.getName()
                                                    .equals(suggest.method())
                    && isSuggestionMethod(x));
            if (candidates.size() != 1) {
                throw error(method,
                            parameter,
                            "names the suggestion method '" + suggest.method() + "', but " + type.getSimpleName()
                                    + " has " + candidates.size() + " methods of that name that take no "
                                    + "parameters and return an Iterable<String>, or take the SuggestionBuilder");
            }
            Method source = candidates.get(0);
            source.setAccessible(true);
            boolean takesBuilder = source.getParameterCount() == 1;
            return builder -> {
                values.forEach(builder::suggest);
                if (takesBuilder) {
                    invokeSuggestions(source, builder);
                    return;
                }
                for (Object x : (Iterable<?>) invokeSuggestions(source)) {
                    builder.suggest(String.valueOf(x));
                }
            };
        }

        private boolean isSuggestionMethod(Method method) {
            if (method.getParameterCount() == 0) {
                return Iterable.class.isAssignableFrom(method.getReturnType());
            }
            return method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(SuggestionBuilder.class)
                    && method.getReturnType() == void.class;
        }

        private Object invokeSuggestions(Method source, Object... arguments) {
            try {
                return invoke(source, arguments);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e.getCause());
            }
        }

        // An unchecked exception the method throws propagates unchanged; a checked one stays wrapped for the caller.
        private Object invoke(Method method, Object... arguments) throws InvocationTargetException {
            try {
                return method.invoke(instance, arguments);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw e;
            }
        }

        private CommandOption<?, C> option(Method method, Parameter parameter, T command) {
            Flag flag = parameter.getAnnotation(Flag.class);
            Option option = parameter.getAnnotation(Option.class);
            reject(method, parameter, "an option", List.of(Name.class, Variadic.class));
            String declaredName;
            if (flag != null) {
                if (option != null) {
                    throw error(method, parameter, "has both @Flag and @Option");
                }
                declaredName = flag.value();
            } else {
                declaredName = Objects.requireNonNull(option)
                                      .value();
            }
            String name = declaredName.isEmpty() ? parameterName(method, parameter) : declaredName;

            // Methods of one command share its options, so a second declaration must match the first.
            CommandState state = state(command);
            Set<Object> key = declaration(parameter);
            RegisteredOption registered = state.options.get(name);
            if (registered != null) {
                if (!registered.key.equals(key)) {
                    throw error(method,
                                parameter,
                                "declares the option --" + name + " differently from another method");
                }
                return registered.option;
            }
            CommandOption<?, C> created = flag != null ? flagOption(method, parameter, name, flag)
                    : valueOption(method, parameter, name, Objects.requireNonNull(option));
            CommandOption<?, C> added = command.option(created);
            // A repeatable option has no single value for @Requires(equalTo) to compare.
            Class<?> valueType = parameter.getType() == List.class ? null : box(parameter.getType());
            state.options.put(name, new RegisteredOption(key, added, valueType));
            for (Requires requires : parameter.getAnnotationsByType(Requires.class)) {
                deferred.add(() -> require(method, parameter, command, added, requires));
            }
            return added;
        }

        private void require(Method method,
                             Parameter parameter,
                             T command,
                             CommandOption<?, C> option,
                             Requires requires) {
            RegisteredOption other = state(command).options.get(requires.value());
            if (other == null) {
                throw error(method,
                            parameter,
                            "has @Requires(\"" + requires.value() + "\"), but '" + command.name()
                                    + "' has no option of that name");
            }
            String[] equalTo = requires.equalTo();
            if (equalTo.length > 1) {
                throw error(method, parameter, "has @Requires with more than one equalTo value");
            }
            if (equalTo.length == 0) {
                option.requires(other.option);
                return;
            }
            Class<?> valueType = other.valueType;
            if (valueType == null) {
                throw error(method,
                            parameter,
                            "has @Requires(\"" + requires.value() + "\") with equalTo, but --" + requires.value()
                                    + " is repeatable");
            }
            String what = "@Requires(value = \"" + requires.value() + "\", equalTo = \"" + equalTo[0] + "\")";
            requireValue(option, other.option, parseValue(method, parameter, valueType, equalTo[0], what));
        }

        private CommandOption<?, C> flagOption(Method method, Parameter parameter, String name, Flag flag) {
            reject(method,
                   parameter,
                   "a @Flag parameter",
                   List.of(Default.class, Range.class, Optional.class, Suggest.class, Greedy.class));
            rejectRegisteredAnnotations(method, parameter, "a @Flag parameter");
            Class<?> valueType = box(parameter.getType());
            char shortName = flag.shortName();
            CommandOption<?, C> option;
            if (valueType == Boolean.class) {
                if (flag.max() != 0) {
                    throw error(method, parameter, "is a boolean flag; max applies to an int flag");
                }
                option = shortName == 0 ? Options.<C>flag(name) : Options.<C>flag(name, shortName);
            } else if (valueType == Integer.class) {
                if (flag.max() < 1) {
                    throw error(method, parameter, "is a counted flag and needs max of at least 1");
                }
                option = shortName == 0 ? Options.<C>count(name, flag.max())
                        : Options.<C>count(name, shortName, flag.max());
            } else {
                throw error(method, parameter, "has @Flag, which needs a boolean or int parameter");
            }
            return finishOption(method, option, flag.aliases(), parameter);
        }

        private CommandOption<?, C> valueOption(Method method, Parameter parameter, String name, Option annotation) {
            if (annotation.required() && (parameter.isAnnotationPresent(Default.class)
                    || parameter.isAnnotationPresent(Optional.class))) {
                throw error(method, parameter, "is a required option and cannot take @Default or @Optional");
            }
            boolean repeatable = parameter.getType() == List.class;
            if (repeatable != (annotation.repeatable() > 0)) {
                throw error(method,
                            parameter,
                            repeatable ? "is a List option and needs repeatable of at least 1"
                                    : "has repeatable, which needs a List parameter");
            }
            Class<?> valueType = repeatable ? elementType(method, parameter) : box(parameter.getType());
            Default defaultValue = parameter.getAnnotation(Default.class);
            if (repeatable && (defaultValue != null || parameter.isAnnotationPresent(Optional.class))) {
                throw error(method,
                            parameter,
                            "is repeatable and cannot take @Default or @Optional; it gives an empty list");
            }
            Object empty = emptyValue(valueType);
            CommandOption<?, C> created;
            if (empty == null) {
                ArgumentFactory<C> factory = factory(valueType);
                if (factory == null) {
                    throw error(method,
                                parameter,
                                "has @Option, but there is no argument type for " + valueType.getName()
                                        + "; register one with registerArgument");
                }
                created = argumentOption(name,
                                         annotation.shortName(),
                                         argument(method, parameter, factory, valueType, false, name));
            } else {
                reject(method,
                       parameter,
                       "an option of " + valueType.getSimpleName(),
                       List.of(Optional.class, Suggest.class, Greedy.class));
                rejectRegisteredAnnotations(method, parameter, "an option of " + valueType.getSimpleName());
                Range range = parameter.getAnnotation(Range.class);
                if (range != null && valueType != Integer.class) {
                    throw error(method, parameter, "has @Range, which applies to int options only");
                }
                checkRange(method, parameter, valueType);
                Object value = defaultValue == null ? empty
                        : parseDefault(method, parameter, valueType, defaultValue.value());
                created = createValueOption(name, annotation.shortName(), valueType, value, range);
            }

            CommandOption<?, C> option = finishOption(method, created, annotation.aliases(), parameter);
            if (repeatable) {
                option = option.repeatable(annotation.repeatable());
            }
            if (annotation.required()) {
                option = option.required();
            }
            return option;
        }

        private CommandOption<?, C> createValueOption(String name,
                                                      char shortName,
                                                      Class<?> valueType,
                                                      Object value,
                                                      @Nullable Range range) {
            boolean hasShortName = shortName != 0;
            if (valueType == Boolean.class) {
                return hasShortName ? Options.<C>bool(name, shortName, (Boolean) value)
                        : Options.<C>bool(name, (Boolean) value);
            }
            if (valueType == Integer.class) {
                int min = range == null ? Integer.MIN_VALUE : (int) range.min();
                int max = range == null ? Integer.MAX_VALUE : (int) range.max();
                return hasShortName ? Options.<C>integer(name, shortName, (Integer) value, min, max)
                        : Options.<C>integer(name, (Integer) value, min, max);
            }
            if (valueType == Long.class) {
                return hasShortName ? Options.<C>longValue(name, shortName, (Long) value)
                        : Options.<C>longValue(name, (Long) value);
            }
            if (valueType == Float.class) {
                return hasShortName ? Options.<C>floatValue(name, shortName, (Float) value)
                        : Options.<C>floatValue(name, (Float) value);
            }
            if (valueType == Double.class) {
                return hasShortName ? Options.<C>doubleValue(name, shortName, (Double) value)
                        : Options.<C>doubleValue(name, (Double) value);
            }
            return hasShortName ? Options.<C>string(name, shortName, (String) value)
                    : Options.<C>string(name, (String) value);
        }

        private <V> CommandOption<V, C> argumentOption(String name, char shortName, CommonArgument<V, C, ?> argument) {
            return shortName == 0 ? Options.of(name, argument) : Options.of(name, shortName, argument);
        }

        private <V> CommandOption<V, C> finishOption(Method method,
                                                     CommandOption<V, C> option,
                                                     String[] aliases,
                                                     Parameter parameter) {
            if (aliases.length > 0) {
                option.aliases(aliases);
            }
            Description description = parameter.getAnnotation(Description.class);
            if (description != null) {
                if (!description.caption()
                                .isEmpty()) {
                    throw error(method,
                                parameter,
                                "has @Description with a caption; an option has a fixed description");
                }
                option.description(description.value());
            }
            Permission permission = parameter.getAnnotation(Permission.class);
            if (permission != null) {
                DefaultPermission[] defaults = permission.defaultPermission();
                String node = permission.value();
                if (!permission.description()
                               .isEmpty()
                        || defaults.length > 1 || (node.isEmpty() && defaults.length == 0)) {
                    throw error(method,
                                parameter,
                                "has @Permission, which on an option needs a node or one defaultPermission, and "
                                        + "takes no description");
                }
                if (node.isEmpty()) {
                    option.permission(defaults[0]);
                } else if (defaults.length == 0) {
                    option.permission(node);
                } else {
                    option.permission(node, defaults[0]);
                }
            }
            return option;
        }

        private AsyncCommandExecutor<C> executor(Method method, List<Extractor<C, ?>> resolvers) {
            boolean async = CompletionStage.class.isAssignableFrom(method.getReturnType());
            boolean result = method.getReturnType() == int.class;
            return ctx -> {
                Object[] values = new Object[resolvers.size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvers.get(i)
                                         .extract(ctx);
                }
                Object returned;
                try {
                    returned = invoke(method, values);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CommandPrerequisiteException) {
                        throw (CommandPrerequisiteException) cause;
                    }
                    // A checked exception fails the command like the stage of an async executor.
                    return CompletableFuture.failedFuture(cause);
                }
                if (async) {
                    return Objects.requireNonNull((CompletionStage<?>) returned,
                                                  () -> describe(method) + " returned null instead of a stage");
                }
                if (result) {
                    ctx.setResult((Integer) returned);
                }
                return CompletableFuture.completedFuture(null);
            };
        }

        private CommandState state(T command) {
            return states.computeIfAbsent(command, x -> new CommandState());
        }

        private String argumentName(Method method, Parameter parameter) {
            Name name = parameter.getAnnotation(Name.class);
            return name != null ? name.value() : parameterName(method, parameter);
        }

        private String parameterName(Method method, Parameter parameter) {
            if (!parameter.isNamePresent()) {
                throw error(method, parameter, "has no name; compile with -parameters or annotate it with @Name");
            }
            return parameter.getName();
        }

        private Class<?> elementType(Method method, Parameter parameter) {
            Type type = parameter.getParameterizedType();
            if (type instanceof ParameterizedType) {
                Type element = ((ParameterizedType) type).getActualTypeArguments()[0];
                // A generic element such as Holder<Enchantment> resolves by its raw class; the factory reads the
                // full type from AnnotatedParameter.parameter().
                if (element instanceof ParameterizedType) {
                    element = ((ParameterizedType) element).getRawType();
                }
                if (element instanceof Class) {
                    return box((Class<?>) element);
                }
            }
            throw error(method, parameter, "needs a List of a class, such as List<String>");
        }

        private Object parseDefault(Method method, Parameter parameter, Class<?> valueType, String text) {
            return parseValue(method, parameter, valueType, text, "@Default(\"" + text + "\")");
        }

        /**
         * Converts {@code text}, which {@code what} names in errors, to a value of {@code valueType}.
         */
        private Object parseValue(Method method, Parameter parameter, Class<?> valueType, String text, String what) {
            try {
                if (valueType == String.class) {
                    return text;
                }
                if (valueType == Integer.class) {
                    return Integer.valueOf(text);
                }
                if (valueType == Long.class) {
                    return Long.valueOf(text);
                }
                if (valueType == Float.class) {
                    return Float.valueOf(text);
                }
                if (valueType == Double.class) {
                    return Double.valueOf(text);
                }
            } catch (NumberFormatException e) {
                throw error(method, parameter, "has " + what + ", which is not a valid " + valueType.getSimpleName());
            }
            if (valueType == Boolean.class && (text.equals("true") || text.equals("false"))) {
                return Boolean.valueOf(text);
            }
            Object[] constants = valueType.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    if (((Enum<?>) constant).name()
                                            .equalsIgnoreCase(text)) {
                        return constant;
                    }
                }
            }
            throw error(method,
                        parameter,
                        "has " + what + ", which is not a valid " + valueType.getName()
                                + "; it takes strings, numbers, booleans and enum constants");
        }

        private void reject(Method method,
                            Parameter parameter,
                            String kind,
                            List<Class<? extends Annotation>> annotationTypes) {
            for (Class<? extends Annotation> x : annotationTypes) {
                if (has(parameter, x)) {
                    throw error(method, parameter, "has @" + x.getSimpleName() + ", which does not apply to " + kind);
                }
            }
        }

        // The handlers of registerAnnotation apply to arguments only, so elsewhere they would be ignored silently.
        private void rejectRegisteredAnnotations(Method method, Parameter parameter, String kind) {
            reject(method, parameter, kind, new ArrayList<>(annotationHandlers.keySet()));
        }

        private IllegalArgumentException error(Method method, String message) {
            return new IllegalArgumentException(describe(method) + " " + message);
        }

        private IllegalArgumentException error(Method method, Parameter parameter, String message) {
            Name name = parameter.getAnnotation(Name.class);
            return new IllegalArgumentException(describe(method) + ": parameter "
                    + (name != null ? name.value() : parameter.getName()) + " " + message);
        }

        private String describe(Method method) {
            return method.getDeclaringClass()
                         .getSimpleName()
                    + "#" + method.getName() + Arrays.stream(method.getParameterTypes())
                                                     .map(Class::getSimpleName)
                                                     .collect(Collectors.joining(", ", "(", ")"));
        }
    }

    private static String pathOf(Method method) {
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        return subcommand == null ? "" : String.join(" ", segments(subcommand));
    }

    private static List<String> segments(Subcommand subcommand) {
        String value = subcommand.value()
                                 .trim();
        return value.isEmpty() ? List.of() : Arrays.asList(value.split("\\s+"));
    }

    private static boolean isPlaceholder(String segment) {
        return segment.length() > 2 && segment.startsWith("<") && segment.endsWith(">");
    }

    // Repeatable annotations are found only through getAnnotationsByType.
    private static boolean has(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return element.getAnnotationsByType(annotationType).length > 0;
    }

    private static List<Class<? extends Annotation>> allOf(List<Class<? extends Annotation>> a,
                                                           List<Class<? extends Annotation>> b) {
        List<Class<? extends Annotation>> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }

    /**
     * What makes two parameters declare the same option or placeholder argument: their annotations and type.
     */
    private static Set<Object> declaration(Parameter parameter) {
        Set<Object> key = new HashSet<>(Arrays.asList(parameter.getAnnotations()));
        key.add(parameter.getParameterizedType());
        return key;
    }

    // The value was converted to the type of the other option.
    @SuppressWarnings("unchecked")
    private static void requireValue(CommandOption<?, ?> option, CommandOption<?, ?> other, Object value) {
        option.requires((CommandOption<Object, ?>) other, value);
    }

    private static <D extends CommonCommandContext<?, ?>> Extractor<D, ?> contextValue(Class<?> type) {
        ContextKey<?> key = ContextKey.of(type);
        return ctx -> ctx.get(key);
    }

    // The extractor was registered for the type with an extractor of a subtype.
    @SuppressWarnings("unchecked")
    private static <V, D extends CommonCommandContext<?, ?>, U extends CommonCommand<D, U>> void provide(U command,
                                                                                                         Class<V> type,
                                                                                                         Extractor<D, ?> extractor) {
        command.provide(type, (Extractor<D, ? extends V>) extractor);
    }

    @SuppressWarnings("unchecked")
    private static <V, D extends CommonCommandContext<?, ?>> void makeOptional(CommonArgument<V, D, ?> argument,
                                                                               Extractor<D, ?> defaultValue) {
        argument.optional((Extractor<D, ? extends V>) defaultValue);
    }

    @Nullable
    private static Object emptyValue(Class<?> valueType) {
        return EMPTY_VALUES.get(valueType);
    }

    private static Class<?> box(Class<?> type) {
        return MethodType.methodType(type)
                         .wrap()
                         .returnType();
    }

    private final class CommandState {
        // The element that gave the command annotations, or null.
        private @Nullable String configuredBy;
        private boolean hasExecutor;
        private final Map<String, RegisteredOption> options = new HashMap<>();
    }

    private final class RegisteredOption {
        private final Set<Object> key;
        private final CommandOption<?, C> option;
        // The type of the value, or null for a repeatable option.
        private final @Nullable Class<?> valueType;

        RegisteredOption(Set<Object> key, CommandOption<?, C> option, @Nullable Class<?> valueType) {
            this.key = key;
            this.option = option;
            this.valueType = valueType;
        }
    }

    /**
     * The argument branch of the placeholders of a {@code @Subcommand} path, shared by the methods below it.
     */
    private final class PathBranch {
        private final ArgumentBranch<C, T> branch;
        private final List<CommonArgument<?, C, ?>> arguments;
        private final List<Set<Object>> keys;
        private boolean hasExecutor;

        PathBranch(ArgumentBranch<C, T> branch, List<CommonArgument<?, C, ?>> arguments, List<Set<Object>> keys) {
            this.branch = branch;
            this.arguments = arguments;
            this.keys = keys;
        }
    }

    /**
     * Where a method runs: its command, the branch when its path ends in placeholders, and the parameters that take
     * the placeholders' values.
     */
    private final class Target {
        private final T command;
        private final @Nullable PathBranch branch;
        private final Map<Parameter, CommonArgument<?, C, ?>> placeholders;

        Target(T command, @Nullable PathBranch branch, Map<Parameter, CommonArgument<?, C, ?>> placeholders) {
            this.command = command;
            this.branch = branch;
            this.placeholders = placeholders;
        }
    }

    private static final class RegisteredHandler<A extends Annotation, V, C extends CommonCommandContext<?, ?>> {
        private final Class<A> annotationType;
        private final Class<?> valueType;
        private final ArgumentAnnotationHandler<A, V, C> handler;

        RegisteredHandler(Class<A> annotationType, Class<?> valueType, ArgumentAnnotationHandler<A, V, C> handler) {
            this.annotationType = annotationType;
            this.valueType = valueType;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        CommonArgument<?, C, ?> apply(Annotation annotation, CommonArgument<?, C, ?> argument) {
            return handler.apply(annotationType.cast(annotation), (CommonArgument<V, C, ?>) argument);
        }
    }
}
