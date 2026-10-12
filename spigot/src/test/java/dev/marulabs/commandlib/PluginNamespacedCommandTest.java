package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class PluginNamespacedCommandTest {
    private static final BukkitCommand WRAPPER = new BukkitCommand("foo", "", "", Collections.emptyList()) {
        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }
    };

    @Test
    void addsTheLabelToTheCommandMapAndACopyOfTheNodeToTheSyncedDispatcher() throws Exception {
        Command<Object> command = ctx -> 1;
        Predicate<Object> requirement = source -> true;
        LiteralCommandNode<Object> node = LiteralArgumentBuilder.literal("foo")
                                                                .executes(command)
                                                                .requires(requirement)
                                                                .then(LiteralArgumentBuilder.literal("sub"))
                                                                .build();
        RootCommandNode<Object> root = new RootCommandNode<>();
        Map<String, org.bukkit.command.Command> knownCommands = new HashMap<>();

        CommandLib.addNamespacedCommand(root, knownCommands, "plugin", node, WRAPPER);

        assertThat(knownCommands).containsEntry("plugin:foo", WRAPPER);
        CommandNode<Object> copy = root.getChild("plugin:foo");
        assertThat(copy.getCommand()).isSameAs(command);
        assertThat(copy.getRequirement()).isSameAs(requirement);
        assertThat(copy.getChild("sub")).isSameAs(node.getChild("sub"));
    }

    @Test
    void leavesTheDispatcherToCraftBukkitBeforeTheCommandsAreSynced() throws Exception {
        LiteralCommandNode<Object> node = LiteralArgumentBuilder.literal("foo")
                                                                .build();
        Map<String, org.bukkit.command.Command> knownCommands = new HashMap<>();

        CommandLib.addNamespacedCommand(null, knownCommands, "plugin", node, WRAPPER);

        assertThat(knownCommands).containsEntry("plugin:foo", WRAPPER);
    }
}
