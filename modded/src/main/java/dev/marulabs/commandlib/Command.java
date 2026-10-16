package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
//? if >=1.18 && !fabric
import net.minecraft.network.chat.Component;
//? if neoforge {
/*import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
*///?} elif forge && >=1.18 {
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
//?} elif <1.18 {
/*import net.minecraftforge.server.permission.DefaultPermissionLevel;

import java.util.AbstractMap;
import java.util.Map;
*///?}

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

    /**
     * Returns the player executing the command, and fails the command with {@link Caption#REQUIRE_PLAYER} for any
     * other source.
     */
    public static Extractor<CommandContext, ServerPlayer> playerRequirer() {
        return ctx -> {
            if (!(ctx.getEntity() instanceof ServerPlayer)) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_PLAYER);
            }
            return (ServerPlayer) ctx.getEntity();
        };
    }

    /**
     * Returns the source of a command the console or a remote console executes, and fails the command with
     * {@link Caption#REQUIRE_CONSOLE} for any other source.
     */
    public static Extractor<CommandContext, CommandSourceStack> consoleRequirer() {
        return ctx -> {
            if (!ctx.getActor()
                    .isConsole()) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_CONSOLE);
            }
            return ctx.getSender();
        };
    }

    public final void requirePlayer() {
        addPrerequisite(playerRequirer()::extract);
    }

    public final void requireConsole() {
        addPrerequisite(consoleRequirer()::extract);
    }

    //? if >=1.18 {
    //? if !fabric {
    final List<PermissionNode<Boolean>> permissionEntries(@NotNull String prefix) {
        return permissionConfigs(prefix).stream()
                                        .map(c -> {
                                            PermissionNode<Boolean> node = new PermissionNode<>("commandlib",
                                                                                                c.node(),
                                                                                                PermissionTypes.BOOLEAN,
                                                                                                (player,
                                                                                                 playerId,
                                                                                                 context) -> resolveDefault(c.defaultPermission(),
                                                                                                                            player));
                                            node.setInformation(Component.nullToEmpty(c.node()),
                                                                Component.nullToEmpty(c.description()));
                                            return node;
                                        })
                                        .collect(Collectors.toList());
    }
    //?}

    // The default of a node, for a permission handler that does not know it.
    static boolean resolveDefault(@NotNull DefaultPermission common, ServerPlayer player) {
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
