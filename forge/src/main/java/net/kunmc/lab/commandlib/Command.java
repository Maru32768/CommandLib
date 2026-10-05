package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.CommonCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
//? if >=1.18 {
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
//?} else {
/*import net.minecraftforge.server.permission.DefaultPermissionLevel;

import java.util.AbstractMap;
import java.util.Map;
*///?}

//? if >=1.18 {
public abstract class Command extends CommonCommand<CommandContext, Command> {
//?} else
/*public class Command extends CommonCommand<CommandContext, Command> {*/
    public Command(@NotNull String name) {
        super(name);
    }

    //? if >=1.18 {
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
    //?} else {
    /*public final void permission(@NotNull DefaultPermissionLevel level) {
        permission(toDefaultPermission(level));
    }

    public final void permission(@NotNull DefaultPermissionLevel level, @NotNull String description) {
        permission(toDefaultPermission(level));
        permissionDescription(description);
    }

    public final void permission(@NotNull String node, @NotNull DefaultPermissionLevel level) {
        permission(node, toDefaultPermission(level));
    }

    public final void permission(@NotNull String node,
                                 @NotNull DefaultPermissionLevel level,
                                 @NotNull String description) {
        permission(node, toDefaultPermission(level));
        permissionDescription(description);
    }

    final List<Map.Entry<String, DefaultPermissionLevel>> permissionEntries(@NotNull String prefix) {
        return permissionConfigs(prefix).stream()
                                        .map(c -> new AbstractMap.SimpleEntry<>(c.node(),
                                                                                toForgeDefault(c.defaultPermission())))
                                        .collect(Collectors.toList());
    }

    private static DefaultPermission toDefaultPermission(@NotNull DefaultPermissionLevel forge) {
        switch (forge) {
            case ALL:
                return DefaultPermission.ALL;
            case NONE:
                return DefaultPermission.NONE;
            default:
                return DefaultPermission.OP;
        }
    }

    private static DefaultPermissionLevel toForgeDefault(@NotNull DefaultPermission common) {
        switch (common) {
            case ALL:
                return DefaultPermissionLevel.ALL;
            case NONE:
                return DefaultPermissionLevel.NONE;
            default:
                return DefaultPermissionLevel.OP;
        }
    }
    *///?}
}
