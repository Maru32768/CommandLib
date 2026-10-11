package net.kunmc.lab.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.Test;

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

        CommandLib.replaceChild(root, node);

        assertThat(root.getChild("give")).isSameAs(node);
        assertThat(node.getChild("vanillaChild")).isNull();
    }
}
