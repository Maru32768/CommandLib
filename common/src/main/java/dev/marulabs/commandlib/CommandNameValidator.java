package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Rejects command trees whose node names collide. Brigadier keys the literal and argument children of a node by name
 * and silently merges children with the same name, which would make one of them unreachable.
 */
final class CommandNameValidator {
    private CommandNameValidator() {
    }

    /**
     * Rejects top-level commands whose names or aliases collide.
     */
    static void validateTopLevel(Collection<? extends CommonCommand<?, ?>> commands) {
        ChildNames childNames = new ChildNames(null);
        for (CommonCommand<?, ?> command : commands) {
            childNames.addLiteral(command);
        }
    }

    /**
     * Rejects collisions among the direct children of {@code command}, including the children and arguments of its
     * argument branches.
     */
    static void validateChildren(CommonCommand<?, ?> command) {
        // Each argument path, from the command itself (empty path) down to the last argument of a branch, is one
        // Brigadier node whose children are checked together.
        Map<List<String>, ChildNames> childNamesByPath = new HashMap<>();
        for (CommonCommand<?, ?> child : command.children()) {
            childNamesAt(childNamesByPath, command, List.of()).addLiteral(child);
        }
        for (Arguments<?> arguments : command.argumentsList()) {
            List<String> path = new ArrayList<>();
            arguments.stream()
                     .forEach(argument -> {
                         childNamesAt(childNamesByPath, command, path).addArgument(argument.name(), argument.type());
                         path.add(argument.name());
                     });
            for (CommonCommand<?, ?> child : arguments.children()) {
                childNamesAt(childNamesByPath, command, path).addLiteral(child);
            }
        }
    }

    private static ChildNames childNamesAt(Map<List<String>, ChildNames> childNamesByPath,
                                           CommonCommand<?, ?> command,
                                           List<String> path) {
        return childNamesByPath.computeIfAbsent(List.copyOf(path), x -> new ChildNames(location(command, x)));
    }

    private static String location(CommonCommand<?, ?> command, List<String> argumentPath) {
        if (argumentPath.isEmpty()) {
            return command.name();
        }
        return command.name() + " <" + String.join("> <", argumentPath) + ">";
    }

    /**
     * Names of the children of one Brigadier node.
     */
    private static final class ChildNames {
        private final @Nullable String location;
        private final Map<String, CommonCommand<?, ?>> literals = new HashMap<>();
        private final Map<String, ArgumentType<?>> arguments = new HashMap<>();

        private ChildNames(@Nullable String location) {
            this.location = location;
        }

        void addLiteral(CommonCommand<?, ?> command) {
            List<String> names = new ArrayList<>();
            names.add(command.name());
            names.addAll(command.aliases());
            for (String name : names) {
                if (arguments.containsKey(name)) {
                    throw conflictWithArgument(name);
                }
                // Repeating a name within one command, or adding the same command twice, merges identical nodes and
                // is harmless, so only collisions between different commands are rejected.
                CommonCommand<?, ?> owner = literals.putIfAbsent(name, command);
                if (owner != null && owner != command) {
                    throw new IllegalArgumentException((location == null ? "Duplicate command name or alias '"
                            : "Duplicate child command name or alias '") + name + "'" + suffix());
                }
            }
        }

        void addArgument(String name, ArgumentType<?> type) {
            // Argument nodes with the same name at the same position are the shared prefix of variable-length
            // branches and merge on purpose, so only a literal of the same name is a conflict.
            if (literals.containsKey(name)) {
                throw conflictWithArgument(name);
            }
            // The merged node parses with the type of one of them, whose value the other branches then read.
            ArgumentType<?> shared = arguments.putIfAbsent(name, type);
            if (shared != null && !sameType(shared, type)) {
                throw new IllegalArgumentException("Arguments named '" + name + "' share one node" + suffix()
                        + " but parse differently: " + shared + " and " + type);
            }
        }

        // A type without its own equals, such as a vanilla one, has no settings that change what it accepts.
        private static boolean sameType(ArgumentType<?> a, ArgumentType<?> b) {
            if (a.getClass() != b.getClass()) {
                return false;
            }
            try {
                return a.getClass()
                        .getMethod("equals", Object.class)
                        .getDeclaringClass() == Object.class || a.equals(b);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        private IllegalArgumentException conflictWithArgument(String name) {
            return new IllegalArgumentException("Command name or alias '" + name
                    + "' conflicts with an argument of the same name" + suffix());
        }

        private String suffix() {
            return location == null ? "" : " under '" + location + "'";
        }
    }
}
