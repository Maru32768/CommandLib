package dev.marulabs.commandlib;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Renders registered commands as indented text: each command with its aliases and permission, then its options,
 * argument branches and child commands. The testing artifacts compare it with golden snapshot files.
 */
final class CommandTreeRenderer {
    private CommandTreeRenderer() {
    }

    static String render(Collection<? extends CommonCommand<?, ?>> commands, String permissionPrefix) {
        StringBuilder out = new StringBuilder();
        commands.forEach(x -> renderCommand(out, x, permissionPrefix, 0));
        return out.toString();
    }

    private static void renderCommand(StringBuilder out, CommonCommand<?, ?> command, String prefix, int depth) {
        String aliases = command.aliases()
                                .isEmpty() ? "" : " (aliases: " + String.join(", ", command.aliases()) + ")";
        line(out,
             depth,
             command.name() + aliases
                     + permission(command.permissionName(prefix), command.effectiveDefaultPermission()));
        for (CommandOption<?, ?> option : command.options()) {
            line(out,
                 depth + 1,
                 option.formatName() + (option.shortName() == null ? "" : ", -" + option.shortName())
                         + (option.hasValue() ? " <value>" : ""));
        }
        for (Arguments<?> arguments : command.argumentsList()) {
            String names = arguments.stream()
                                    .map(x -> "<" + x.name() + ": " + typeName(x) + ">")
                                    .collect(Collectors.joining(" "));
            line(out,
                 depth + 1,
                 names + permission(arguments.permissionName(prefix), arguments.effectiveDefaultPermission()));
            arguments.children()
                     .forEach(x -> renderCommand(out, x, prefix, depth + 2));
        }
        command.children()
               .forEach(x -> renderCommand(out, x, prefix, depth + 1));
    }

    private static String typeName(Object argument) {
        Class<?> type = argument.getClass();
        // An anonymous subclass has no simple name; show the argument class it extends.
        while (type.isAnonymousClass()) {
            type = type.getSuperclass();
        }
        return type.getSimpleName();
    }

    private static String permission(String node, DefaultPermission defaultPermission) {
        return " [" + node + ", " + defaultPermission + "]";
    }

    private static void line(StringBuilder out, int depth, String text) {
        out.append("  ".repeat(depth))
           .append(text)
           .append('\n');
    }
}
