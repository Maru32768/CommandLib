package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandTree.ArgumentEntry;
import dev.marulabs.commandlib.CommandTree.BranchEntry;
import dev.marulabs.commandlib.CommandTree.CommandEntry;
import dev.marulabs.commandlib.CommandTree.OptionEntry;
import dev.marulabs.commandlib.CommandTree.PermissionEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders registered commands as indented text: each command with its aliases and permission, then its cooldown
 * bypass permission, options, argument branches and child commands. The testing artifacts compare it with golden snapshot files, and
 * {@link CommandDocs#tree()} returns it.
 */
final class CommandTreeRenderer {
    private CommandTreeRenderer() {
    }

    static String render(Collection<? extends CommonCommand<?, ?>> commands, String permissionPrefix) {
        return render(CommandTree.of(commands, permissionPrefix));
    }

    static String render(List<CommandEntry> commands) {
        StringBuilder out = new StringBuilder();
        commands.forEach(x -> renderCommand(out, x, 0));
        return out.toString();
    }

    private static void renderCommand(StringBuilder out, CommandEntry command, int depth) {
        String aliases = command.aliases.isEmpty() ? "" : " (aliases: " + String.join(", ", command.aliases) + ")";
        line(out, depth, command.name + aliases + permission(command.permission));
        if (command.cooldownBypass != null) {
            line(out, depth + 1, "cooldown bypass" + permission(command.cooldownBypass));
        }
        for (OptionEntry option : command.options) {
            List<String> names = new ArrayList<>();
            option.names.forEach(x -> names.add("--" + x));
            if (option.shortName != null) {
                names.add("-" + option.shortName);
            }
            line(out,
                 depth + 1,
                 String.join(", ", names) + (option.valueName == null ? "" : " <" + option.valueName + ">")
                         + notes(option.notes()) + (option.permission == null ? "" : permission(option.permission)));
        }
        for (BranchEntry branch : command.branches) {
            String arguments = branch.arguments.stream()
                                               .map(CommandTreeRenderer::argument)
                                               .collect(Collectors.joining(" "));
            line(out, depth + 1, arguments + permission(branch.permission));
            branch.children.forEach(x -> renderCommand(out, x, depth + 2));
        }
        command.children.forEach(x -> renderCommand(out, x, depth + 1));
    }

    private static String argument(ArgumentEntry argument) {
        // Examples: "<name: StringArgument>", "[count: IntegerArgument]", "<targets...: PlayerArgument>"
        String text = argument.name + (argument.variadic() ? "..." : "") + ": " + argument.type;
        return argument.optional ? "[" + text + "]" : "<" + text + ">";
    }

    private static String notes(List<String> notes) {
        return notes.isEmpty() ? "" : " (" + String.join(", ", notes) + ")";
    }

    private static String permission(PermissionEntry permission) {
        return " [" + permission.node + ", " + permission.defaultPermission + "]";
    }

    private static void line(StringBuilder out, int depth, String text) {
        out.append("  ".repeat(depth))
           .append(text)
           .append('\n');
    }
}
