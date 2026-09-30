package net.kunmc.lab.commandlib;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class CommandLib {
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

    @SubscribeEvent
    public void onRegisterCommands(FMLServerStartedEvent e) {
        register();
    }

    private void register() {
        RootCommandNode<CommandSource> root = ServerLifecycleHooks.getCurrentServer()
                                                                  .getCommandManager()
                                                                  .getDispatcher()
                                                                  .getRoot();

        new CommandNodeCreator<>(commands, permissionPrefix).build()
                                                            .forEach(root::addChild);

        commands.stream()
                .flatMap(x -> x.permissionEntries(permissionPrefix)
                               .stream())
                .forEach(e -> PermissionAPI.registerNode(e.getKey(), e.getValue(), ""));
    }
}
