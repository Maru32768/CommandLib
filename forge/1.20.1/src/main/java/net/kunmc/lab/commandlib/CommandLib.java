package net.kunmc.lab.commandlib;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class CommandLib {
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = new ConcurrentHashMap<>();

    private final Collection<? extends Command> commands;
    private final String permissionPrefix;

    public static void register(@NotNull String permissionPrefix,
                                @NotNull Command command,
                                @NotNull Command... commands) {
        ArrayList<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        register(permissionPrefix, list);
    }

    public static void register(@NotNull String permissionPrefix, @NotNull Collection<? extends Command> commands) {
        new CommandLib(commands, permissionPrefix);
    }

    private CommandLib(Collection<? extends Command> commands, String permissionPrefix) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;

        // Always listen: RegisterCommandsEvent fires again on /reload and on every integrated server start,
        // and PermissionGatherEvent.Nodes may still be pending when the server object already exists.
        MinecraftForge.EVENT_BUS.register(this);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // The current dispatcher has already been built, so add the commands to it directly.
            // Nodes that miss PermissionGatherEvent.Nodes are resolved by their default resolver.
            permissionNodes().forEach(node -> PERMISSIONS.putIfAbsent(node.getNodeName(), node));
            register(server.getCommands()
                           .getDispatcher()
                           .getRoot());
            server.getPlayerList()
                  .getPlayers()
                  .forEach(player -> server.getCommands()
                                           .sendCommands(player));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher()
                  .getRoot());
    }

    @SubscribeEvent
    public void onGatherPermissionNodes(PermissionGatherEvent.Nodes e) {
        Set<String> registered = e.getNodes()
                                  .stream()
                                  .map(PermissionNode::getNodeName)
                                  .collect(Collectors.toSet());
        for (PermissionNode<Boolean> node : permissionNodes()) {
            // The same node name may be declared by several commands; Forge rejects duplicates.
            if (registered.add(node.getNodeName())) {
                e.addNodes(node);
                PERMISSIONS.put(node.getNodeName(), node);
            }
        }
    }

    private List<PermissionNode<Boolean>> permissionNodes() {
        return commands.stream()
                       .flatMap(x -> x.permissionEntries(permissionPrefix)
                                      .stream())
                       .collect(Collectors.toList());
    }

    private void register(RootCommandNode<CommandSourceStack> root) {
        new CommandNodeCreator<>(commands, permissionPrefix).build()
                                                            .forEach(root::addChild);
    }

    static boolean hasPermission(CommandSourceStack source, ServerPlayer player, String node) {
        PermissionNode<Boolean> permissionNode = PERMISSIONS.get("commandlib." + node);
        if (permissionNode == null) {
            return source.hasPermission(4);
        }
        if (PermissionAPI.getRegisteredNodes()
                         .contains(permissionNode)) {
            return PermissionAPI.getPermission(player, permissionNode);
        }
        return permissionNode.getDefaultResolver()
                             .resolve(player, player.getUUID());
    }
}
