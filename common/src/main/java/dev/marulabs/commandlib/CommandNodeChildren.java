package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Changes the children of a live command node without changing the maps another thread may be iterating. Paper builds
 * each player's command tree on an asynchronous pool by iterating the dispatcher root's children, so changing those maps
 * on the main thread meanwhile throws ConcurrentModificationException there. Instead, the changes are made on copies,
 * which then replace the node's maps: a tree being built keeps iterating the old maps, and a later one reads the new.
 */
final class CommandNodeChildren {
    private static final Field CHILDREN = field("children");
    private static final Field LITERALS = field("literals");
    private static final Field ARGUMENTS = field("arguments");

    private CommandNodeChildren() {
    }

    /**
     * Removes the children with the given names, then adds the given nodes under their names, replacing children of the
     * same name instead of merging into them as {@link CommandNode#addChild} does.
     */
    static void replace(CommandNode<?> parent,
                        Collection<String> removedNames,
                        Collection<? extends CommandNode<?>> addedNodes) {
        Map<String, CommandNode<?>> children = copy(CHILDREN, parent);
        Map<String, CommandNode<?>> literals = copy(LITERALS, parent);
        Map<String, CommandNode<?>> arguments = copy(ARGUMENTS, parent);
        for (String name : removedNames) {
            children.remove(name);
            literals.remove(name);
            arguments.remove(name);
        }
        for (CommandNode<?> node : addedNodes) {
            String name = node.getName();
            literals.remove(name);
            arguments.remove(name);
            children.put(name, node);
            if (node instanceof LiteralCommandNode) {
                literals.put(name, node);
            } else if (node instanceof ArgumentCommandNode) {
                arguments.put(name, node);
            }
        }
        // The maps are complete before another thread can see them.
        VarHandle.releaseFence();
        set(LITERALS, parent, literals);
        set(ARGUMENTS, parent, arguments);
        // The tree builders read only the children, so they see the change last.
        set(CHILDREN, parent, children);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CommandNode<?>> copy(Field field, CommandNode<?> node) {
        try {
            return new LinkedHashMap<>((Map<String, CommandNode<?>>) field.get(node));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Field field, CommandNode<?> node, Map<String, CommandNode<?>> map) {
        try {
            field.set(node, map);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            // The fields are final; reflection may still set them on an ordinary class.
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
