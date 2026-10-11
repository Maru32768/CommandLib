package net.kunmc.lab.commandlib;

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

import static org.assertj.core.api.Assertions.assertThat;

class MinecraftNamespacedCommandRemovalTest {
    /**
     * Stands in for CraftBukkit's VanillaCommandWrapper, which exposes the wrapped node as a public field.
     */
    public static final class FakeVanillaCommandWrapper extends BukkitCommand {
        public final CommandNode<Object> vanillaCommand;

        FakeVanillaCommandWrapper(CommandNode<Object> vanillaCommand) {
            super(vanillaCommand.getName(), "", "", Collections.emptyList());
            this.vanillaCommand = vanillaCommand;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }
    }

    private static LiteralCommandNode<Object> literal(String name) {
        return LiteralArgumentBuilder.literal(name)
                                     .build();
    }

    @Test
    void removesTheNamespacedWrapperOfTheNode() throws Exception {
        LiteralCommandNode<Object> node = literal("foo");
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(literal("minecraft:foo"));
        Map<String, org.bukkit.command.Command> knownCommands = new HashMap<>();
        knownCommands.put("minecraft:foo", new FakeVanillaCommandWrapper(node));

        CommandLib.removeMinecraftNamespacedCommand(root, knownCommands, node);

        assertThat(knownCommands).doesNotContainKey("minecraft:foo");
        assertThat(root.getChild("minecraft:foo")).isNull();
    }

    @Test
    void keepsTheNamespacedWrapperOfAnotherNode() throws Exception {
        LiteralCommandNode<Object> node = literal("give");
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(literal("minecraft:give"));
        Map<String, org.bukkit.command.Command> knownCommands = new HashMap<>();
        FakeVanillaCommandWrapper vanilla = new FakeVanillaCommandWrapper(literal("give"));
        knownCommands.put("minecraft:give", vanilla);

        CommandLib.removeMinecraftNamespacedCommand(root, knownCommands, node);

        assertThat(knownCommands).containsEntry("minecraft:give", vanilla);
        assertThat(root.getChild("minecraft:give")).isNotNull();
    }

    @Test
    void keepsACommandThatIsNotAVanillaWrapper() throws Exception {
        LiteralCommandNode<Object> node = literal("foo");
        RootCommandNode<Object> root = new RootCommandNode<>();
        Map<String, org.bukkit.command.Command> knownCommands = new HashMap<>();
        org.bukkit.command.Command other = new BukkitCommand("foo") {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return true;
            }
        };
        knownCommands.put("minecraft:foo", other);

        CommandLib.removeMinecraftNamespacedCommand(root, knownCommands, node);

        assertThat(knownCommands).containsEntry("minecraft:foo", other);
    }
}
