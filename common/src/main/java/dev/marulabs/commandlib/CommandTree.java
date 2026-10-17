package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A snapshot of registered commands for the renderers that describe them ({@link CommandTreeRenderer},
 * {@link CommandDocs}): each command with its route, permission, options, argument branches and child commands. The
 * one walk of the command classes lives here, so every rendering agrees on routes and permission nodes.
 */
final class CommandTree {
    private CommandTree() {
    }

    static List<CommandEntry> of(Collection<? extends CommonCommand<?, ?>> commands, String permissionPrefix) {
        List<CommandEntry> entries = new ArrayList<>();
        for (CommonCommand<?, ?> command : commands) {
            entries.add(command(command, "/" + command.name(), permissionPrefix));
        }
        return entries;
    }

    private static CommandEntry command(CommonCommand<?, ?> command, String route, String prefix) {
        List<OptionEntry> options = new ArrayList<>();
        for (CommandOption<?, ?> option : command.options()) {
            PermissionConfig permission = option.permissionConfig(command, prefix);
            options.add(new OptionEntry(option.names(),
                                        option.shortName(),
                                        option.hasValue() ? option.name() : null,
                                        option.isRequired(),
                                        option.maxOccurrences(),
                                        option.description(),
                                        permission == null ? null
                                                : new PermissionEntry(permission,
                                                                      route + " --" + option.name(),
                                                                      PermissionEntry.OPTION)));
        }
        List<BranchEntry> branches = new ArrayList<>();
        for (Arguments<?> arguments : command.argumentsList()) {
            branches.add(branch(arguments, route, prefix));
        }
        List<CommandEntry> children = new ArrayList<>();
        for (CommonCommand<?, ?> child : command.children()) {
            children.add(command(child, route + " " + child.name(), prefix));
        }
        // Listed on the command that sets the cooldown, not on the children that inherit it.
        Cooldown cooldown = command.ownCooldown();
        String bypassNode = cooldown == null ? null : cooldown.bypassNode();
        return new CommandEntry(command.name(),
                                route,
                                command.aliases(),
                                command.staticDescription(),
                                new PermissionEntry(command.permissionConfig(prefix), route, PermissionEntry.COMMAND),
                                bypassNode == null
                                        ? null : new PermissionEntry(
                                                                     new PermissionConfig(bypassNode,
                                                                                          DefaultPermission.OP,
                                                                                          "Skips the cooldown of "
                                                                                                  + route),
                                                                     route,
                                                                     PermissionEntry.COOLDOWN_BYPASS),
                                command.executor() != null,
                                options,
                                branches,
                                children,
                                command.examples()
                                       .stream()
                                       .map(x -> x.startsWith("/") ? x : "/" + x)
                                       .collect(Collectors.toList()));
    }

    private static BranchEntry branch(Arguments<?> arguments, String route, String prefix) {
        List<ArgumentEntry> argumentEntries = new ArrayList<>();
        arguments.stream()
                 .forEach(x -> argumentEntries.add(argument(x)));
        String tags = argumentEntries.stream()
                                     .map(ArgumentEntry::tag)
                                     .collect(Collectors.joining(" "));
        String branchRoute = tags.isEmpty() ? route : route + " " + tags;
        List<CommandEntry> children = new ArrayList<>();
        for (CommonCommand<?, ?> child : arguments.children()) {
            children.add(command(child, branchRoute + " " + child.name(), prefix));
        }
        return new BranchEntry(argumentEntries,
                               branchRoute,
                               arguments.staticDescription(),
                               new PermissionEntry(arguments.permissionConfig(prefix),
                                                   branchRoute,
                                                   PermissionEntry.ARGUMENT),
                               arguments.isExecutable(),
                               children);
    }

    private static ArgumentEntry argument(CommonArgument<?, ?, ?> argument) {
        if (argument instanceof VariadicArgument) {
            VariadicArgument<?, ?> variadic = (VariadicArgument<?, ?>) argument;
            return new ArgumentEntry(argument.name(),
                                     typeName(variadic.element()),
                                     argument.isOptional(),
                                     variadic.nodeCount());
        }
        return new ArgumentEntry(argument.name(), typeName(argument), argument.isOptional(), 0);
    }

    private static String typeName(Object argument) {
        Class<?> type = argument.getClass();
        // An anonymous subclass has no simple name; show the argument class it extends.
        while (type.isAnonymousClass()) {
            type = type.getSuperclass();
        }
        return type.getSimpleName();
    }

    /**
     * Every permission node of the commands, each command followed by its cooldown's bypass node, its options that need
     * a permission, its argument branches, the commands under each branch, and then its child commands.
     */
    static List<PermissionEntry> permissions(List<CommandEntry> commands) {
        List<PermissionEntry> permissions = new ArrayList<>();
        commands.forEach(x -> addPermissions(permissions, x));
        return permissions;
    }

    /**
     * The nodes of {@link #permissions(List)} for {@code commands}, which every platform registers.
     */
    static List<PermissionConfig> permissionConfigs(Collection<? extends CommonCommand<?, ?>> commands,
                                                    String permissionPrefix) {
        return permissions(of(commands, permissionPrefix)).stream()
                                                          .map(x -> new PermissionConfig(x.node,
                                                                                         x.defaultPermission,
                                                                                         x.description))
                                                          .collect(Collectors.toList());
    }

    /**
     * The cooldown bypass nodes of {@code commands}. Unlike the other nodes, which follow the commands, a bypass node can
     * be any node, so a platform leaves one that already exists as is.
     */
    static Set<String> cooldownBypassNodes(Collection<? extends CommonCommand<?, ?>> commands,
                                           String permissionPrefix) {
        return permissions(of(commands, permissionPrefix)).stream()
                                                          .filter(x -> x.kind.equals(PermissionEntry.COOLDOWN_BYPASS))
                                                          .map(x -> x.node)
                                                          .collect(Collectors.toSet());
    }

    private static void addPermissions(List<PermissionEntry> permissions, CommandEntry command) {
        permissions.add(command.permission);
        if (command.cooldownBypass != null) {
            permissions.add(command.cooldownBypass);
        }
        for (OptionEntry option : command.options) {
            if (option.permission != null) {
                permissions.add(option.permission);
            }
        }
        for (BranchEntry branch : command.branches) {
            permissions.add(branch.permission);
            branch.children.forEach(x -> addPermissions(permissions, x));
        }
        command.children.forEach(x -> addPermissions(permissions, x));
    }

    static final class CommandEntry {
        final String name;
        /**
         * The route from the root command by names, such as {@code /config <key> set}.
         */
        final String route;
        final List<String> aliases;
        // Null when the description depends on the sender.
        final @Nullable String description;
        final PermissionEntry permission;
        // Null when the command sets no cooldown with a bypass permission.
        final @Nullable PermissionEntry cooldownBypass;
        /**
         * Whether the command runs by itself instead of showing its help.
         */
        final boolean executable;
        final List<OptionEntry> options;
        final List<BranchEntry> branches;
        final List<CommandEntry> children;
        /**
         * The examples set with {@code examples(...)}, each starting with {@code /}.
         */
        final List<String> examples;

        CommandEntry(String name,
                     String route,
                     List<String> aliases,
                     @Nullable String description,
                     PermissionEntry permission,
                     @Nullable PermissionEntry cooldownBypass,
                     boolean executable,
                     List<OptionEntry> options,
                     List<BranchEntry> branches,
                     List<CommandEntry> children,
                     List<String> examples) {
            this.name = name;
            this.route = route;
            this.aliases = List.copyOf(aliases);
            this.description = description;
            this.permission = permission;
            this.cooldownBypass = cooldownBypass;
            this.executable = executable;
            this.options = List.copyOf(options);
            this.branches = List.copyOf(branches);
            this.children = List.copyOf(children);
            this.examples = List.copyOf(examples);
        }

        /**
         * The route with {@code [options]} after it when the command has options.
         */
        String usagePrefix() {
            return options.isEmpty() ? route : route + " [options]";
        }
    }

    static final class BranchEntry {
        final List<ArgumentEntry> arguments;
        /**
         * The command's route followed by the argument tags, such as {@code /config <key>}.
         */
        final String route;
        // Null when the description depends on the sender.
        final @Nullable String description;
        final PermissionEntry permission;
        /**
         * Whether the command runs when the input ends with this branch.
         */
        final boolean executable;
        final List<CommandEntry> children;

        BranchEntry(List<ArgumentEntry> arguments,
                    String route,
                    @Nullable String description,
                    PermissionEntry permission,
                    boolean executable,
                    List<CommandEntry> children) {
            this.arguments = List.copyOf(arguments);
            this.route = route;
            this.description = description;
            this.permission = permission;
            this.executable = executable;
            this.children = List.copyOf(children);
        }

        String tags() {
            return arguments.stream()
                            .map(ArgumentEntry::tag)
                            .collect(Collectors.joining(" "));
        }
    }

    static final class ArgumentEntry {
        final String name;
        final String type;
        final boolean optional;
        /**
         * The number of values a variadic argument takes at most, or 0 for an argument that takes one.
         */
        final int maxCount;

        ArgumentEntry(String name, String type, boolean optional, int maxCount) {
            this.name = name;
            this.type = type;
            this.optional = optional;
            this.maxCount = maxCount;
        }

        boolean variadic() {
            return maxCount > 0;
        }

        /**
         * The argument as usages show it: {@code <name>}, {@code [name]} or {@code <name...>}.
         */
        String tag() {
            String text = name + (variadic() ? "..." : "");
            return optional ? "[" + text + "]" : "<" + text + ">";
        }

        List<String> notes() {
            List<String> notes = new ArrayList<>();
            if (optional) {
                notes.add("optional");
            }
            if (variadic()) {
                notes.add("variadic, up to " + maxCount);
            }
            return notes;
        }
    }

    static final class OptionEntry {
        /**
         * The long names, the option's name first and then its aliases.
         */
        final List<String> names;
        final @Nullable Character shortName;
        // Null for a flag.
        final @Nullable String valueName;
        final boolean required;
        final int maxOccurrences;
        final String description;
        // Null for an option that needs no permission.
        final @Nullable PermissionEntry permission;

        OptionEntry(List<String> names,
                    @Nullable Character shortName,
                    @Nullable String valueName,
                    boolean required,
                    int maxOccurrences,
                    String description,
                    @Nullable PermissionEntry permission) {
            this.names = List.copyOf(names);
            this.shortName = shortName;
            this.valueName = valueName;
            this.required = required;
            this.maxOccurrences = maxOccurrences;
            this.description = description;
            this.permission = permission;
        }

        /**
         * The names as typed: {@code -n}, {@code --limit}, {@code --max}.
         */
        List<String> spellings() {
            List<String> spellings = new ArrayList<>();
            if (shortName != null) {
                spellings.add("-" + shortName);
            }
            names.forEach(x -> spellings.add("--" + x));
            return spellings;
        }

        List<String> notes() {
            List<String> notes = new ArrayList<>();
            if (required) {
                notes.add("required");
            }
            if (maxOccurrences > 1) {
                notes.add("up to " + maxOccurrences + " times");
            }
            return notes;
        }
    }

    static final class PermissionEntry {
        static final String COMMAND = "command";
        static final String ARGUMENT = "argument";
        static final String OPTION = "option";
        static final String COOLDOWN_BYPASS = "cooldown-bypass";

        final String node;
        final DefaultPermission defaultPermission;
        final String description;
        /**
         * The route the node guards: a command's, an argument branch's with its argument tags, or a command's with the
         * option's long name. For a cooldown bypass node, the route of the command that sets the cooldown.
         */
        final String route;
        /**
         * {@link #COMMAND}, {@link #ARGUMENT}, {@link #OPTION} or {@link #COOLDOWN_BYPASS}.
         */
        final String kind;

        PermissionEntry(PermissionConfig config, String route, String kind) {
            this.node = config.node();
            this.defaultPermission = config.defaultPermission();
            this.description = config.description();
            this.route = route;
            this.kind = kind;
        }
    }
}
