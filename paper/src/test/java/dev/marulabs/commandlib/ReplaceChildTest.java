package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
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
        PaperDispatcher.replaceChildren(root, List.of("give"), List.of(node));

        assertThat(root.getChild("give")).isSameAs(node);
        assertThat(node.getChild("vanillaChild")).isNull();
    }

    @Test
    void replacesTheMirroredRootsMapsInsteadOfChangingThem() {
        CommandDispatcher<CommandSourceStack> vanilla = new CommandDispatcher<>();
        vanilla.getRoot()
               .addChild(LiteralArgumentBuilder.<CommandSourceStack>literal("old")
                                               .build());
        ApiMirrorRootNode mirror = new ApiMirrorRootNode() {
            @Override
            public CommandDispatcher<CommandSourceStack> getDispatcher() {
                return vanilla;
            }
        };
        // A player's tree that Paper is building on its pool meanwhile.
        Iterator<CommandNode<CommandSourceStack>> building = vanilla.getRoot()
                                                                    .getChildren()
                                                                    .iterator();

        PaperDispatcher.replaceChildren(mirror,
                                        List.of("old"),
                                        List.of(LiteralArgumentBuilder.<CommandSourceStack>literal("new")
                                                                      .build()));

        assertThat(building.next()
                           .getName()).isEqualTo("old");
        assertThat(vanilla.getRoot()
                          .getChildren()).extracting(CommandNode::getName)
                                         .containsExactly("new");
    }

    @Test
    void renamedNodeKeepsTheCommandRequirementAndChildren() {
        Command<CommandSourceStack> executor = ctx -> 1;
        LiteralCommandNode<CommandSourceStack> node = LiteralArgumentBuilder.<CommandSourceStack>literal("foo")
                                                                            .executes(executor)
                                                                            .then(LiteralArgumentBuilder.literal("sub"))
                                                                            .build();

        LiteralCommandNode<CommandSourceStack> copy = PaperDispatcher.renamed(node, "plugin:foo");

        assertThat(copy.getLiteral()).isEqualTo("plugin:foo");
        assertThat(copy.getCommand()).isSameAs(executor);
        assertThat(copy.getRequirement()).isSameAs(node.getRequirement());
        assertThat(copy.getChild("sub")).isSameAs(node.getChild("sub"));
    }
}
