package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.exception.CommandPrerequisiteException;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class Command extends CommonCommand<CommandContext, Command> {
    public Command(@NotNull String name) {
        super(name);
    }

    public static Extractor<CommandContext, Player> playerRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof Player)) {
                throw new CommandPrerequisiteException("This command can only be executed by a player.");
            }
            return (Player) ctx.getSender();
        };
    }

    public static Extractor<CommandContext, ConsoleCommandSender> consoleRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof ConsoleCommandSender)) {
                throw new CommandPrerequisiteException("This command can only be executed from the console.");
            }
            return (ConsoleCommandSender) ctx.getSender();
        };
    }

    public final void requirePlayer() {
        addPrerequisite(playerRequirer()::extract);
    }

    public final void requireConsole() {
        addPrerequisite(consoleRequirer()::extract);
    }

    public final void permission(@NotNull PermissionDefault defaultPermission) {
        permission(toDefaultPermission(defaultPermission));
    }

    public final void permission(@NotNull PermissionDefault defaultPermission, @NotNull String description) {
        permission(toDefaultPermission(defaultPermission));
        permissionDescription(description);
    }

    public final void permission(@NotNull String node, @NotNull PermissionDefault defaultPermission) {
        permission(node, toDefaultPermission(defaultPermission));
    }

    public final void permission(@NotNull String node,
                                 @NotNull PermissionDefault defaultPermission,
                                 @NotNull String description) {
        permission(node, toDefaultPermission(defaultPermission));
        permissionDescription(description);
    }

    final List<Permission> permissions(@NotNull String prefix) {
        return permissionConfigs(prefix).stream()
                                        .map(c -> new Permission(c.node(),
                                                                 c.description(),
                                                                 toBukkitDefault(c.defaultPermission())))
                                        .collect(Collectors.toList());
    }

    private static DefaultPermission toDefaultPermission(@NotNull PermissionDefault bukkit) {
        switch (bukkit) {
            case TRUE:
                return DefaultPermission.ALL;
            case FALSE:
                return DefaultPermission.NONE;
            case OP:
                return DefaultPermission.OP;
            default:
                throw new IllegalArgumentException("Unsupported PermissionDefault: " + bukkit.name() + " (use TRUE, FALSE or OP)");
        }
    }

    private static PermissionDefault toBukkitDefault(@NotNull DefaultPermission common) {
        switch (common) {
            case ALL:
                return PermissionDefault.TRUE;
            case NONE:
                return PermissionDefault.FALSE;
            default:
                return PermissionDefault.OP;
        }
    }
}
