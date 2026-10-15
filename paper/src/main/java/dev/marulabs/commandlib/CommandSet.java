package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The commands of one registration and the prefix of their permissions.
 *
 * @param byLabel   the command of each name and alias
 * @param scheduler the threads of the async commands
 */
record CommandSet(List<Command> commands, String permissionPrefix, Map<String, Command> byLabel,
        CommandScheduler scheduler) {
    static CommandSet of(String permissionPrefix, Collection<? extends Command> commands, CommandScheduler scheduler) {
        Objects.requireNonNull(permissionPrefix);
        Objects.requireNonNull(commands);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        commands.forEach(Objects::requireNonNull);
        CommandNameValidator.validateTopLevel(commands);

        Map<String, Command> byLabel = new HashMap<>();
        for (Command command : commands) {
            byLabel.put(command.name(), command);
            command.aliases()
                   .forEach(alias -> byLabel.put(alias, command));
        }
        return new CommandSet(List.copyOf(commands), permissionPrefix, Map.copyOf(byLabel), scheduler);
    }

    List<LiteralCommandNode<CommandSourceStack>> buildNodes() {
        return new CommandNodeCreator<CommandSourceStack, Component, CommandContext, Command>(commands,
                                                                                              permissionPrefix,
                                                                                              scheduler).build();
    }

    /**
     * The command of a label, with or without the plugin's namespace.
     */
    @Nullable
    Command command(String label) {
        return byLabel.get(label.substring(label.indexOf(':') + 1));
    }

    /**
     * The short help text of a node that {@link #buildNodes()} made.
     */
    String description(LiteralCommandNode<CommandSourceStack> node, String pluginName) {
        return Objects.requireNonNull(byLabel.get(node.getLiteral()))
                      .serverHelpShortText(pluginName);
    }

    List<Permission> permissions() {
        return commands.stream()
                       .flatMap(c -> c.permissions(permissionPrefix)
                                      .stream())
                       .collect(Collectors.toList());
    }
}
