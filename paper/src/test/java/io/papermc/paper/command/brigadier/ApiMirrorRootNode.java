package io.papermc.paper.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

/**
 * A stand-in for the server's root of the API dispatcher, which mirrors the vanilla dispatcher: it changes the vanilla
 * root in place, as the server's does. Paper 1.21.4+ has it but no PluginCommandNode, as here.
 */
public abstract class ApiMirrorRootNode extends RootCommandNode<CommandSourceStack> {
    public abstract CommandDispatcher<CommandSourceStack> getDispatcher();

    @SuppressWarnings("unused") // Called by reflection, as on the server.
    private CommandNode<CommandSourceStack> unwrapNode(CommandNode<CommandSourceStack> node) {
        return node;
    }

    @Override
    public void addChild(CommandNode<CommandSourceStack> node) {
        getDispatcher().getRoot()
                       .addChild(unwrapNode(node));
    }

    public void removeCommand(String name) {
        RootCommandNode<CommandSourceStack> root = getDispatcher().getRoot();
        root.getChildren()
            .removeIf(x -> x.getName()
                            .equals(name));
    }
}
