package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.ChatColorUtil;
import org.jetbrains.annotations.NotNull;

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
    private String permissionNodeOverride = null;
    private DefaultPermission defaultPermissionOverride = null;
    private String permissionDescription = "";
    private CommandExecutor<C> executor = null;
    private boolean executorSet = false;

    Arguments(CommonCommand<C, ?> owner,
              List<? extends CommonArgument<?, C, ?>> arguments,
              Collection<? extends CommonCommand<C, ?>> children) {
        this.owner = owner;
        this.arguments = arguments;
        this.children = new ArrayList<>(children);
    }

    void parse(C ctx) throws ArgumentParseException {
        for (CommonArgument<?, C, ?> argument : arguments) {
            // Help actions can run before the whole branch is typed; stop at the first missing token so partial
            // input still produces help instead of an unrelated parse failure.
            if (!ctx.hasInput(argument.name())) {
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

        return arguments.stream()
                        .map(x -> String.format(ChatColorUtil.GRAY + "<" + ChatColorUtil.YELLOW + "%s" + ChatColorUtil.GRAY + ">",
                                                x.name()))
                        .collect(Collectors.joining(" "));
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
        // A null executor is kept as an explicit choice so that it still overrides the argument's own executor and
        // the branch falls back to the help action.
        this.executor = executor;
        this.executorSet = true;
    }

    CommandExecutor<C> executorFor(CommonArgument<?, C, ?> argument) {
        boolean isLast = !arguments.isEmpty() && arguments.get(arguments.size() - 1) == argument;
        if (isLast && executorSet) {
            return executor;
        }
        return argument.executor();
    }

    void description(String description) {
        this.description = ctx -> description;
    }

    void description(Function<C, String> description) {
        this.description = Objects.requireNonNull(description);
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
                                    effectiveDefaultPermission(parent),
                                    permissionDescription);
    }

    DefaultPermission effectiveDefaultPermission(@NotNull CommonCommand<C, ?> parent) {
        if (defaultPermissionOverride != null) {
            return defaultPermissionOverride;
        }
        return parent.effectiveDefaultPermission();
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
