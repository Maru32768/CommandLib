package dev.marulabs.commandlib;

import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.util.ChatColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Arguments<C extends CommonCommandContext<?, ?>> {
    private final CommonCommand<C, ?> owner;
    private final List<? extends CommonArgument<?, C, ?>> arguments;
    private final List<CommonCommand<C, ?>> children;
    private Function<C, String> description = ctx -> "";
    // The text of description(String), or null when a function gives the description.
    private @Nullable String staticDescription = "";
    private @Nullable String permissionNodeOverride = null;
    private @Nullable DefaultPermission defaultPermissionOverride = null;
    private String permissionDescription = "";
    // Null until the branch sets one; the last argument's own executor applies then.
    private @Nullable CommandExecutor<C> executor = null;

    Arguments(CommonCommand<C, ?> owner,
              List<? extends CommonArgument<?, C, ?>> arguments,
              Collection<? extends CommonCommand<C, ?>> children) {
        this.owner = owner;
        this.arguments = arguments;
        this.children = new ArrayList<>(children);
    }

    void parse(C ctx) throws ArgumentParseException, CommandPrerequisiteException {
        for (CommonArgument<?, C, ?> argument : arguments) {
            if (!ctx.hasInput(argument.name())) {
                if (argument.isOptional()) {
                    ctx.setParsedArgument(argument.name(), argument.defaultValue(ctx));
                    continue;
                }
                // Help actions can run before the whole branch is typed; stop at the first missing token so partial
                // input still produces help instead of an unrelated parse failure.
                return;
            }
            Object parsedArg = argument.parse(ctx);
            ctx.setParsedArgument(argument.name(), parsedArg);
        }
    }

    String concatTagNames() {
        if (arguments.isEmpty()) {
            return "";
        }

        // Examples: "<target>", "[count]", "<targets...>"
        return arguments.stream()
                        .map(x -> {
                            String name = x.name() + (x instanceof VariadicArgument ? "..." : "");
                            String open = x.isOptional() ? "[" : "<";
                            String close = x.isOptional() ? "]" : ">";
                            return ChatColorUtil.GRAY + open + ChatColorUtil.YELLOW + name + ChatColorUtil.GRAY + close;
                        })
                        .collect(Collectors.joining(" "));
    }

    /**
     * Checks that optional and variadic arguments end the branch, so that each input has one meaning.
     *
     * @throws IllegalStateException if they do not
     */
    void validateTrailingArguments(CommonCommand<C, ?> parent) {
        for (int i = 0; i < arguments.size(); i++) {
            CommonArgument<?, C, ?> argument = arguments.get(i);
            if (argument instanceof VariadicArgument && i != arguments.size() - 1) {
                throw new IllegalStateException("Variadic argument '" + argument.name() + "' of command '"
                        + parent.name() + "' must be the last argument of its branch");
            }
            if (i > 0 && arguments.get(i - 1)
                                  .isOptional()
                    && !argument.isOptional()) {
                throw new IllegalStateException("Argument '" + argument.name() + "' of command '" + parent.name()
                        + "' follows optional argument '" + arguments.get(i - 1)
                                                                     .name()
                        + "'; optional arguments must be the last arguments of their branch");
            }
        }
        CommonArgument<?, C, ?> last = arguments.get(arguments.size() - 1);
        if (!children.isEmpty() && (last.isOptional() || last instanceof VariadicArgument)) {
            throw new IllegalStateException("The branch of command '" + parent.name() + "' ending with argument '"
                    + last.name() + "' cannot have child commands, since the argument is optional or variadic");
        }
    }

    /**
     * Whether the branch runs without any of its arguments typed.
     */
    boolean isAllOptional() {
        return arguments.stream()
                        .allMatch(CommonArgument::isOptional);
    }

    CommonArgument<?, C, ?> first() {
        return arguments.get(0);
    }

    int size() {
        return arguments.size();
    }

    Stream<? extends CommonArgument<?, C, ?>> stream() {
        return arguments.stream();
    }

    List<? extends CommonCommand<C, ?>> children() {
        return List.copyOf(children);
    }

    void addChildren(Collection<? extends CommonCommand<C, ?>> children) {
        this.children.addAll(children);
    }

    void removeChildrenFrom(int index) {
        this.children.subList(index, this.children.size())
                     .clear();
    }

    void executor(CommandExecutor<C> executor) {
        // Keep the executor on the branch instead of the argument instance, so one argument instance can be reused
        // as the last argument of several branches without them overwriting each other's executor.
        this.executor = Objects.requireNonNull(executor);
    }

    @Nullable
    CommandExecutor<C> executorFor(CommonArgument<?, C, ?> argument) {
        // The branch can end at an argument when every argument after it is optional.
        int index = arguments.indexOf(argument);
        boolean endsBranch = index >= 0 && arguments.subList(index + 1, arguments.size())
                                                    .stream()
                                                    .allMatch(CommonArgument::isOptional);
        if (endsBranch && executor != null) {
            return executor;
        }
        return argument.executor();
    }

    void description(String description) {
        this.description = ctx -> description;
        this.staticDescription = description;
    }

    void description(Function<C, String> description) {
        this.description = Objects.requireNonNull(description);
        this.staticDescription = null;
    }

    @Nullable
    String staticDescription() {
        return staticDescription;
    }

    String description(C ctx) {
        return description.apply(ctx);
    }

    void permission(@NotNull String node) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
    }

    void permission(@NotNull DefaultPermission defaultPermission) {
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
    }

    void permission(@NotNull DefaultPermission defaultPermission, @NotNull String description) {
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
        this.permissionDescription = Objects.requireNonNull(description);
    }

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
    }

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission, @NotNull String description) {
        this.permissionNodeOverride = Objects.requireNonNull(node);
        this.defaultPermissionOverride = Objects.requireNonNull(defaultPermission);
        this.permissionDescription = Objects.requireNonNull(description);
    }

    void permissionDescription(@NotNull String description) {
        this.permissionDescription = Objects.requireNonNull(description);
    }

    String permissionName(@NotNull CommonCommand<C, ?> parent, @NotNull String prefix) {
        if (permissionNodeOverride != null) {
            return permissionNodeOverride;
        }
        return prefix + "." + permissionNameWithoutPrefix(parent);
    }

    String permissionName(@NotNull String prefix) {
        return permissionName(owner, prefix);
    }

    PermissionConfig permissionConfig(@NotNull CommonCommand<C, ?> parent, @NotNull String prefix) {
        return new PermissionConfig(permissionName(parent, prefix),
                                    effectiveDefaultPermission(),
                                    permissionDescription);
    }

    PermissionConfig permissionConfig(@NotNull String prefix) {
        return permissionConfig(owner, prefix);
    }

    /**
     * Whether the command runs when the input ends with this branch, instead of needing a child command.
     */
    boolean isExecutable() {
        return arguments.isEmpty() ? executor != null : executorFor(arguments.get(arguments.size() - 1)) != null;
    }

    DefaultPermission effectiveDefaultPermission() {
        if (defaultPermissionOverride != null) {
            return defaultPermissionOverride;
        }
        return owner.effectiveDefaultPermission();
    }

    private String permissionNameWithoutPrefix(@NotNull CommonCommand<C, ?> parent) {
        String argumentNames = arguments.stream()
                                        .map(CommonArgument::name)
                                        .collect(Collectors.joining("."));
        if (argumentNames.isEmpty()) {
            return parent.permissionNameWithoutPrefix();
        }
        return parent.permissionNameWithoutPrefix() + "." + argumentNames;
    }
}
