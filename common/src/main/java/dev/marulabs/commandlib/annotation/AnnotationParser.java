package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.Confirmation;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.branch.ArgumentBranch;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
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
                                                                                           // Last, so that a @Sender parameter can allow it.
                                                                                           Sender.class);

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
    private final Map<Class<?>, Extractor<C, ?>> contextValues = new HashMap<>();
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
     * Resolves parameters of {@code type} from the context with {@code extractor}: those annotated {@link Sender},
     * those whose type has no argument factory, and the value of an omitted {@link Optional} argument of the type. A
     * {@link CommandPrerequisiteException} thrown by the extractor fails the command with its message.
     */
    public <V> AnnotationParser<C, T> registerContextValue(@NotNull Class<V> type,
                                                           @NotNull Extractor<C, ? extends V> extractor) {
        contextValues.put(box(type), Objects.requireNonNull(extractor));
        return this;
    }

    /**
     * Applies {@code handler} to every argument parameter annotated with {@code annotationType} whose value type is a
     * {@code valueType}. Handlers apply in registration order, after the factory and {@link Suggest}, and before the
     * argument is made variadic or optional, so a handler of a {@code List} parameter configures each value.
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
        private final IdentityHashMap<T, CommandState> states = new IdentityHashMap<>();

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
            commands.put("", root);
            configure(root, type, "class " + type.getSimpleName(), new String[0]);

            List<Method> methods = Arrays.stream(type.getDeclaredMethods())
                                         .filter(x -> !x.isSynthetic())
                                         .filter(this::isCommandMethod)
                                         .sorted(Comparator.comparing((Method x) -> pathOf(x))
                                                           .thenComparing(Method::getName)
                                                           .thenComparingInt(Method::getParameterCount)
                                                           .thenComparing(Method::toGenericString))
                                         .collect(Collectors.toList());
            methods.forEach(this::addMethod);
            return root;
        }

        private boolean isCommandMethod(Method method) {
            if (method.isAnnotationPresent(Execute.class) || method.isAnnotationPresent(Subcommand.class)) {
                return true;
            }
            for (Class<? extends Annotation> x : List.of(Description.class,
                                                         Permission.class,
                                                         Confirm.class,
                                                         Cooldown.class)) {
                if (method.isAnnotationPresent(x)) {
                    throw error(method, "has @" + x.getSimpleName() + " but neither @Execute nor @Subcommand");
                }
            }
            return false;
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
            T command;
            if (subcommand != null) {
                command = commandAt(method, subcommand);
                configure(command, method, describe(method), subcommand.aliases());
            } else {
                command = root;
                for (Class<? extends Annotation> x : List.of(Confirm.class, Cooldown.class)) {
                    if (method.isAnnotationPresent(x)) {
                        throw error(method, "has @" + x.getSimpleName() + "; put it on the class");
                    }
                }
            }
            try {
                method.setAccessible(true);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(describe(method) + " is not accessible", e);
            }

            List<CommonArgument<?, C, ?>> arguments = new ArrayList<>();
            List<Extractor<C, ?>> resolvers = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                resolvers.add(resolve(method, parameter, command, arguments));
            }
            AsyncCommandExecutor<C> executor = executor(method, resolvers);

            if (arguments.isEmpty()) {
                if (execute != null && (method.isAnnotationPresent(Description.class)
                        || method.isAnnotationPresent(Permission.class))) {
                    throw error(method,
                                "has no arguments, so its @Description and @Permission would describe the "
                                        + "command; put them on the class");
                }
                CommandState state = state(command);
                if (state.hasExecutor) {
                    throw error(method, "executes '" + pathOf(method) + "' without arguments, as another method does");
                }
                state.hasExecutor = true;
                command.executeAsync(executor);
                return;
            }
            ArgumentBranch<C, T> branch = command.argument(builder -> {
                arguments.forEach(builder::argument);
                builder.executeAsync(executor);
            });
            if (execute != null) {
                Description description = method.getAnnotation(Description.class);
                if (description != null) {
                    branch.description(description.value());
                }
                Permission permission = method.getAnnotation(Permission.class);
                if (permission != null) {
                    applyPermission(method,
                                    permission,
                                    branch::permission,
                                    branch::permission,
                                    branch::permissionDescription);
                }
            }
        }

        private T commandAt(Method method, Subcommand subcommand) {
            List<String> segments = segments(subcommand);
            if (segments.isEmpty()) {
                throw error(method, "has an empty @Subcommand; use @Execute for the command itself");
            }
            T current = root;
            List<String> path = new ArrayList<>();
            for (String segment : segments) {
                path.add(segment);
                T parent = current;
                current = commands.computeIfAbsent(String.join(" ", path), x -> {
                    T child = commandFactory.apply(segment);
                    parent.addChildren(List.of(child));
                    return child;
                });
            }
            return current;
        }

        /**
         * Applies the command annotations of {@code element}, which only one element of a command may give.
         */
        private void configure(T command, AnnotatedElement element, String source, String[] aliases) {
            Description description = element.getAnnotation(Description.class);
            Permission permission = element.getAnnotation(Permission.class);
            Confirm confirm = element.getAnnotation(Confirm.class);
            Cooldown cooldown = element.getAnnotation(Cooldown.class);
            if (description == null && permission == null && confirm == null && cooldown == null
                    && aliases.length == 0) {
                return;
            }
            CommandState state = state(command);
            if (state.configuredBy != null) {
                throw new IllegalArgumentException(source + " configures '" + command.name() + "', which "
                        + state.configuredBy + " already configures; give the command annotations on one method");
            }
            state.configuredBy = source;
            if (aliases.length > 0) {
                command.addAliases(Arrays.asList(aliases));
            }
            if (description != null) {
                command.description(description.value());
            }
            if (permission != null) {
                applyPermission(element,
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
        }

        private void applyPermission(AnnotatedElement element,
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
                throw new IllegalArgumentException(element + " gives more than one defaultPermission");
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
                return ctx -> ctx;
            }
            if (parameter.isAnnotationPresent(Sender.class)) {
                reject(method,
                       parameter,
                       "a @Sender parameter",
                       PARAMETER_ANNOTATIONS.subList(0, PARAMETER_ANNOTATIONS.size() - 1));
                Extractor<C, ?> value = contextValues.get(box(rawType));
                if (value == null) {
                    throw error(method,
                                parameter,
                                "has no context value for " + rawType.getName()
                                        + "; register one with registerContextValue");
                }
                return value;
            }
            if (parameter.isAnnotationPresent(Flag.class) || parameter.isAnnotationPresent(Option.class)) {
                CommandOption<?, C> option = option(method, parameter, command);
                return ctx -> ctx.getOption(option);
            }

            boolean variadic = rawType == List.class;
            Class<?> valueType = variadic ? elementType(method, parameter) : box(rawType);
            ArgumentFactory<C> factory = argumentFactories.get(valueType);
            if (factory == null && valueType.isEnum()) {
                factory = argumentFactories.get(Enum.class);
            }
            if (factory == null) {
                Extractor<C, ?> value = variadic ? null : contextValues.get(valueType);
                if (value == null) {
                    throw error(method,
                                parameter,
                                "has no argument type for " + valueType.getName()
                                        + "; register one with registerArgument");
                }
                reject(method, parameter, "a context value", PARAMETER_ANNOTATIONS);
                return value;
            }
            CommonArgument<?, C, ?> argument = argument(method, parameter, factory, valueType, variadic);
            arguments.add(argument);
            return ctx -> ctx.getArgument(argument);
        }

        private CommonArgument<?, C, ?> argument(Method method,
                                                 Parameter parameter,
                                                 ArgumentFactory<C> factory,
                                                 Class<?> valueType,
                                                 boolean variadic) {
            reject(method, parameter, "an argument", List.of(Description.class));
            if (parameter.isAnnotationPresent(Range.class) && !Number.class.isAssignableFrom(valueType)) {
                throw error(method, parameter, "has @Range, but " + valueType.getName() + " is not a number");
            }
            if (parameter.isAnnotationPresent(Greedy.class) && valueType != String.class) {
                throw error(method, parameter, "has @Greedy, but " + valueType.getName() + " is not a String");
            }
            String name = argumentName(method, parameter);
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
                argument = argument.variadic();
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
                Extractor<C, ?> fromContext = variadic ? null : contextValues.get(valueType);
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

        private SuggestionProvider<C> suggestions(Method method, Parameter parameter, Suggest suggest) {
            List<String> values = List.of(suggest.value());
            if (suggest.method()
                       .isEmpty()) {
                if (values.isEmpty()) {
                    throw error(method, parameter, "has @Suggest without values or a method");
                }
                return builder -> values.forEach(builder::suggest);
            }
            List<Method> candidates = Arrays.stream(type.getDeclaredMethods())
                                            .filter(x -> x.getName()
                                                          .equals(suggest.method()))
                                            .filter(x -> isSuggestionMethod(x))
                                            .collect(Collectors.toList());
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
                return source.invoke(instance, arguments);
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
                throw new IllegalStateException(cause);
            }
        }

        private CommandOption<?, C> option(Method method, Parameter parameter, T command) {
            Flag flag = parameter.getAnnotation(Flag.class);
            Option option = parameter.getAnnotation(Option.class);
            reject(method, parameter, "an option", List.of(Optional.class, Suggest.class, Name.class, Greedy.class));
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
            Set<Object> key = new HashSet<>(Arrays.asList(parameter.getAnnotations()));
            key.add(parameter.getParameterizedType());
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
            state.options.put(name, new RegisteredOption(key, added));
            return added;
        }

        private CommandOption<?, C> flagOption(Method method, Parameter parameter, String name, Flag flag) {
            reject(method, parameter, "a @Flag parameter", List.of(Default.class, Range.class));
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
            return finishOption(option, flag.aliases(), parameter);
        }

        private CommandOption<?, C> valueOption(Method method, Parameter parameter, String name, Option annotation) {
            boolean repeatable = parameter.getType() == List.class;
            if (repeatable != (annotation.repeatable() > 0)) {
                throw error(method,
                            parameter,
                            repeatable ? "is a List option and needs repeatable of at least 1"
                                    : "has repeatable, which needs a List parameter");
            }
            Class<?> valueType = repeatable ? elementType(method, parameter) : box(parameter.getType());
            Object empty = emptyValue(valueType);
            if (empty == null) {
                throw error(method,
                            parameter,
                            "has @Option, which needs a boolean, int, long, float, double or " + "String parameter");
            }
            Range range = parameter.getAnnotation(Range.class);
            if (range != null && valueType != Integer.class) {
                throw error(method, parameter, "has @Range, which applies to int options only");
            }
            Default defaultValue = parameter.getAnnotation(Default.class);
            if (defaultValue != null && repeatable) {
                throw error(method, parameter, "is repeatable and cannot take @Default; it gives an empty list");
            }
            Object value = defaultValue == null ? empty
                    : parseDefault(method, parameter, valueType, defaultValue.value());

            CommandOption<?, C> option = finishOption(createValueOption(name,
                                                                        annotation.shortName(),
                                                                        valueType,
                                                                        value,
                                                                        range),
                                                      annotation.aliases(),
                                                      parameter);
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

        private <V> CommandOption<V, C> finishOption(CommandOption<V, C> option,
                                                     String[] aliases,
                                                     Parameter parameter) {
            if (aliases.length > 0) {
                option.aliases(aliases);
            }
            Description description = parameter.getAnnotation(Description.class);
            if (description != null) {
                option.description(description.value());
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
                    returned = method.invoke(instance, values);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CommandPrerequisiteException) {
                        throw (CommandPrerequisiteException) cause;
                    }
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
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
                if (element instanceof Class) {
                    return box((Class<?>) element);
                }
            }
            throw error(method, parameter, "needs a List of a class, such as List<String>");
        }

        private Object parseDefault(Method method, Parameter parameter, Class<?> valueType, String text) {
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
                throw error(method,
                            parameter,
                            "has @Default(\"" + text + "\"), which is not a valid " + valueType.getSimpleName());
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
                        "has @Default(\"" + text + "\"), which is not a valid " + valueType.getName()
                                + "; @Default takes strings, numbers, booleans and enum constants");
        }

        private void reject(Method method,
                            Parameter parameter,
                            String kind,
                            List<Class<? extends Annotation>> annotationTypes) {
            for (Class<? extends Annotation> x : annotationTypes) {
                if (parameter.isAnnotationPresent(x)) {
                    throw error(method, parameter, "has @" + x.getSimpleName() + ", which does not apply to " + kind);
                }
            }
        }

        private IllegalArgumentException error(Method method, String message) {
            return new IllegalArgumentException(describe(method) + " " + message);
        }

        private IllegalArgumentException error(Method method, Parameter parameter, String message) {
            return new IllegalArgumentException(describe(method) + ": parameter " + parameter.getName() + " "
                    + message);
        }

        private String describe(Method method) {
            return type.getSimpleName() + "#" + method.getName() + Arrays.stream(method.getParameterTypes())
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

        RegisteredOption(Set<Object> key, CommandOption<?, C> option) {
            this.key = key;
            this.option = option;
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
