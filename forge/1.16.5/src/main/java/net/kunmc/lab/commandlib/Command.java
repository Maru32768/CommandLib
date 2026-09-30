package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.CommonCommand;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Command extends CommonCommand<CommandContext, Command> {
    public Command(@NotNull String name) {
        super(name);
    }

    public final void permission(@NotNull DefaultPermissionLevel level) {
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
}
