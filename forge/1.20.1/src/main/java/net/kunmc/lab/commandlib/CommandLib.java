package net.kunmc.lab.commandlib;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            register();
        }
    }

    private void register() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        register(server.getCommands()
                       .getDispatcher()
                       .getRoot());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher()
                  .getRoot());
    }

    @SubscribeEvent
    public void onGatherPermissionNodes(PermissionGatherEvent.Nodes e) {
        commands.stream()
                .flatMap(x -> x.permissionEntries(permissionPrefix)
                               .stream())
                .peek(node -> PERMISSIONS.put(node.getNodeName(), node))
                .forEach(e::addNodes);
    }

    private void register(RootCommandNode<CommandSourceStack> root) {
        new CommandNodeCreator<>(commands, permissionPrefix).build()
                                                            .forEach(root::addChild);
    }

    static PermissionNode<Boolean> permissionNode(String node) {
        return PERMISSIONS.get("commandlib." + node);
    }
}
