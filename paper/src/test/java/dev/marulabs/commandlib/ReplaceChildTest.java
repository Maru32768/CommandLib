package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplaceChildTest {
    @Test
    void replacesAnExistingCommandInsteadOfMergingIntoIt() {
        RootCommandNode<CommandSourceStack> root = new RootCommandNode<>();
        root.addChild(LiteralArgumentBuilder.<CommandSourceStack>literal("give")
                                            .requires(x -> false)
                                            .then(LiteralArgumentBuilder.literal("vanillaChild"))
                                            .build());
        Command<CommandSourceStack> executor = ctx -> 1;
        LiteralCommandNode<CommandSourceStack> node = LiteralArgumentBuilder.<CommandSourceStack>literal("give")
                                                                            .executes(executor)
                                                                            .build();

        // Brigadier's addChild would merge the node into the existing child, keeping its requirement and children.
        CommandLib.replaceChildren(root, List.of("give"), List.of(node));

        assertThat(root.getChild("give")).isSameAs(node);
        assertThat(node.getChild("vanillaChild")).isNull();
    }

    @Test
    void renamedNodeKeepsTheCommandRequirementAndChildren() {
        Command<CommandSourceStack> executor = ctx -> 1;
        LiteralCommandNode<CommandSourceStack> node = LiteralArgumentBuilder.<CommandSourceStack>literal("foo")
                                                                            .executes(executor)
                                                                            .then(LiteralArgumentBuilder.literal("sub"))
                                                                            .build();

        LiteralCommandNode<CommandSourceStack> copy = CommandLib.renamed(node, "plugin:foo");

        assertThat(copy.getLiteral()).isEqualTo("plugin:foo");
        assertThat(copy.getCommand()).isSameAs(executor);
        assertThat(copy.getRequirement()).isSameAs(node.getRequirement());
        assertThat(copy.getChild("sub")).isSameAs(node.getChild("sub"));
    }
}
