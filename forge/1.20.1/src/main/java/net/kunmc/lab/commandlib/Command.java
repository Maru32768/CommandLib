package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Command extends CommonCommand<CommandContext, Command> {
    public Command(@NotNull String name) {
        super(name);
    }

    final List<PermissionNode<Boolean>> permissionEntries(@NotNull String prefix) {
        return permissionConfigs(prefix).stream()
                                        .map(c -> {
                                            PermissionNode<Boolean> node = new PermissionNode<>("commandlib",
                                                                                                c.node(),
                                                                                                PermissionTypes.BOOLEAN,
                                                                                                (player, playerId, context) -> toForgeDefault(c.defaultPermission(),
                                                                                                                                               player));
                                            node.setInformation(Component.literal(c.node()),
                                                                Component.literal(c.description()));
                                            return node;
                                        })
                                        .collect(Collectors.toList());
    }

    private static boolean toForgeDefault(@NotNull DefaultPermission common, ServerPlayer player) {
        switch (common) {
            case ALL:
                return true;
            case NONE:
                return false;
            default:
                return player != null && player.hasPermissions(4);
        }
    }
}
