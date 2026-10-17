package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Changes Paper's live dispatcher as its registrar does, for registrations that have no COMMANDS event to add them.
 */
final class PaperDispatcher {
    private static final @Nullable PaperInternals PAPER = PaperInternals.find();

    private PaperDispatcher() {
    }

    /**
     * The dispatcher that Paper hands to the COMMANDS handlers, which it replaces on every data reload. Paper has no
     * API for it outside the event, so it is read from its registrar, or else the given one.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    static CommandDispatcher<CommandSourceStack> live(@Nullable CommandDispatcher<CommandSourceStack> lastSeen) {
        try {
            Class<?> registrar = Class.forName("io.papermc.paper.command.brigadier.PaperCommands");
            return (CommandDispatcher<CommandSourceStack>) registrar.getMethod("getDispatcherInternal")
                                                                    .invoke(registrar.getField("INSTANCE")
                                                                                     .get(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return lastSeen;
        }
    }

    /**
     * Removes the commands with the given names from the root and adds the given ones, replacing those of the same
     * name. On Paper the root mirrors the vanilla dispatcher, which Paper also reads on an asynchronous pool to build
     * the players' command trees, so its children are replaced without changing the maps such a build may be
     * iterating ({@link CommandNodeChildren}), each node converted as the mirror's addChild would.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void replaceChildren(RootCommandNode<CommandSourceStack> root,
                                Collection<String> names,
                                Collection<? extends CommandNode<CommandSourceStack>> nodes) {
        if (PAPER == null || !PAPER.mirrorRoot()
                                   .isInstance(root)) {
            names.forEach(name -> removeFromNode(root, name));
            nodes.forEach(root::addChild);
            return;
        }
        try {
            CommandNode vanillaRoot = ((CommandDispatcher) PAPER.mirroredDispatcher()
                                                                .invoke(root)).getRoot();
            List<CommandNode<?>> converted = new ArrayList<>();
            for (CommandNode<CommandSourceStack> node : nodes) {
                converted.add((CommandNode<?>) PAPER.unwrapNode()
                                                    .invoke(root, node));
            }
            CommandNodeChildren.replace(vanillaRoot, names, converted);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The node under {@code name} in the root. On Paper it is the vanilla dispatcher's node, which the mirror root holds
     * for it.
     */
    @Nullable
    static Object child(RootCommandNode<CommandSourceStack> root, String name) {
        if (PAPER == null || !PAPER.mirrorRoot()
                                   .isInstance(root)) {
            return root.getChild(name);
        }
        try {
            return ((CommandDispatcher<?>) PAPER.mirroredDispatcher()
                                                .invoke(root)).getRoot()
                                                              .getChild(name);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void removeFromNode(RootCommandNode<CommandSourceStack> root, String name) {
        // Paper's dispatcher root mirrors the vanilla dispatcher and overrides its own removeCommand(String) to
        // remove the node there as well. Editing the root's maps directly would leave the command executable.
        try {
            root.getClass()
                .getMethod("removeCommand", String.class)
                .invoke(root, name);
            return;
        } catch (NoSuchMethodException ignored) {
            // Plain Brigadier (e.g. in tests) has no removeCommand.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to remove command " + name, e);
        }
        CommandNodeChildren.replace(root, List.of(name), List.of());
    }

    /**
     * The node of a label. On Paper each label is a PluginCommandNode, as Paper's registrar makes them, so the command
     * map shows a PluginVanillaCommandWrapper, which names the plugin and has no minecraft.command.&lt;label&gt;
     * permission, instead of a Mojang command.
     */
    @SuppressWarnings("unchecked")
    static LiteralCommandNode<CommandSourceStack> labelNode(Plugin plugin,
                                                            LiteralCommandNode<CommandSourceStack> node,
                                                            String label,
                                                            String description) {
        if (PAPER == null) {
            return label.equals(node.getLiteral()) ? node : renamed(node, label);
        }
        try {
            return (LiteralCommandNode<CommandSourceStack>) PAPER.pluginCommandNode()
                                                                 .newInstance(label,
                                                                              plugin.getPluginMeta(),
                                                                              node,
                                                                              description);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static LiteralCommandNode<CommandSourceStack> renamed(LiteralCommandNode<CommandSourceStack> node, String label) {
        LiteralCommandNode<CommandSourceStack> copy = new LiteralCommandNode<>(label,
                                                                               node.getCommand(),
                                                                               node.getRequirement(),
                                                                               node.getRedirect(),
                                                                               node.getRedirectModifier(),
                                                                               node.isFork());
        node.getChildren()
            .forEach(copy::addChild);
        return copy;
    }

    /**
     * Paper's internal classes for changing its live dispatcher as its registrar does.
     */
    private record PaperInternals(Class<?> mirrorRoot, Method mirroredDispatcher, Method unwrapNode,
            Constructor<?> pluginCommandNode) {
        @Nullable
        static PaperInternals find() {
            try {
                Class<?> mirrorRoot = Class.forName("io.papermc.paper.command.brigadier.ApiMirrorRootNode");
                Method unwrapNode = mirrorRoot.getDeclaredMethod("unwrapNode", CommandNode.class);
                unwrapNode.setAccessible(true);
                return new PaperInternals(mirrorRoot,
                                          mirrorRoot.getMethod("getDispatcher"),
                                          unwrapNode,
                                          Class.forName("io.papermc.paper.command.brigadier.PluginCommandNode")
                                               .getConstructor(String.class,
                                                               PluginMeta.class,
                                                               LiteralCommandNode.class,
                                                               String.class));
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Not a Paper server, as in tests, or Paper changed these internals: nodes are then added as plain
                // nodes and the root's own methods change it.
                return null;
            }
        }
    }
}
