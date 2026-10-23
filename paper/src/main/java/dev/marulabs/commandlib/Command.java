package dev.marulabs.commandlib;

import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Command extends CommonCommand<CommandContext, Command> {
    public Command(@NotNull String name) {
        super(name);
    }

    /**
     * A builder of a command named {@code name}, which configures it in one chain. See {@link CommandBuilder}.
     */
    @NotNull
    public static CommandBuilder<CommandContext, Command> builder(@NotNull String name) {
        return CommandBuilder.of(new Command(name));
    }

    public static Extractor<CommandContext, Player> playerRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof Player)) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_PLAYER);
            }
            return (Player) ctx.getSender();
        };
    }

    public static Extractor<CommandContext, ConsoleCommandSender> consoleRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof ConsoleCommandSender)) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_CONSOLE);
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
        return permissions(List.of(this), prefix);
    }

    static List<Permission> permissions(@NotNull Collection<? extends Command> commands, @NotNull String prefix) {
        return CommandTree.permissionConfigs(commands, prefix)
                          .stream()
                          .map(c -> new Permission(c.node(), c.description(), toBukkitDefault(c.defaultPermission())))
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
            case NOT_OP:
                break;
        }
        throw new IllegalArgumentException("Unsupported PermissionDefault: " + bukkit.name() + " (use TRUE, FALSE or OP)");
    }

    private static PermissionDefault toBukkitDefault(@NotNull DefaultPermission common) {
        switch (common) {
            case ALL:
                return PermissionDefault.TRUE;
            case NONE:
                return PermissionDefault.FALSE;
            case OP:
                return PermissionDefault.OP;
        }
        throw new AssertionError(common);
    }
}
