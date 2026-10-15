package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.RootCommandNode;
import dev.marulabs.commandlib.util.CurrentServer;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if fabric {
/*import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
*///?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.PermissionAPI;
*///?} else {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.permission.PermissionAPI;
//?}
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
//? if >=1.18 {
//? if neoforge {
/*import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
*///?} elif !fabric {
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
//?}

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
//?} else {
/*import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
*///?}

public final class CommandLib {
    //? if fabric {
    /*private static final Map<String, DefaultPermission> PERMISSIONS = new ConcurrentHashMap<>();
    *///?} elif >=1.18 {
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = new ConcurrentHashMap<>();
    //?}

    // The registrations of this copy of CommandLib, which last for the game's lifetime.
    private static final List<CommandLib> REGISTRATIONS = new CopyOnWriteArrayList<>();

    private final Collection<? extends Command> commands;
    private final String permissionPrefix;

    public static CommandLib register(@NotNull String permissionPrefix,
                                      @NotNull Command command,
                                      @NotNull Command... commands) {
        ArrayList<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return register(permissionPrefix, list);
    }

    public static CommandLib register(@NotNull String permissionPrefix,
                                      @NotNull Collection<? extends Command> commands) {
        CommandLib registration = new CommandLib(commands, permissionPrefix);
        REGISTRATIONS.add(registration);
        return registration;
    }

    /**
     * Returns the documentation of this registration's commands with its permission prefix: Markdown and JSON of the
     * commands and of the permission nodes registered for them. On 1.18+ the nodes that permission handlers see have
     * a {@code commandlib.} namespace in front on Forge and NeoForge.
     */
    public CommandDocs docs() {
        return CommandDocs.of(permissionPrefix, commands);
    }

    /**
     * Reports the platform and the registrations of this copy of CommandLib: the loader and Minecraft version, how the
     * commands reach the dispatcher, and each registration's commands and permissions. The owner of a registration is
     * its permission prefix. {@link CommandLibDiagnosticsCommand} sends it to a source.
     */
    public static CommandLibDiagnostics diagnostics() {
        boolean running = CurrentServer.get() != null;
        List<String> platform = new ArrayList<>();
        //? if fabric {
        /*String loader = "fabric";
        *///?} elif neoforge {
        /*String loader = "neoforge";
        *///?} else
        String loader = "forge";
        platform.add("Artifact: " + loader);
        platform.add("CommandLib package: " + CommandLib.class.getPackage()
                                                              .getName());
        //? if fabric {
        /*platform.add("Loader: " + loader + " " + loaderVersion("fabricloader"));
        *///?} else
        platform.add("Loader: " + loader + " " + loaderVersion(loader));
        platform.add("Minecraft: " + minecraftVersion());
        platform.add("Java: " + System.getProperty("java.version"));
        //? if fabric {
        /*platform.add("Command registration: CommandRegistrationCallback on every data reload, or the running server's "
                + "dispatcher when registered later");
        platform.add("Permission nodes: fabric-permissions-api "
                + (FabricLoader.getInstance()
                               .isModLoaded("fabric-permissions-api-v0") ? "loaded" : "not loaded, defaults decide")
                + ", named <node>");
        *///?} elif >=1.18 {
        platform.add("Command registration: RegisterCommandsEvent on every data reload, or the running server's "
                + "dispatcher when registered later");
        platform.add("Permission nodes: PermissionGatherEvent.Nodes, named commandlib.<node>");
        //?} else {
        /*platform.add("Command registration: the server's dispatcher when the server has started");
        platform.add("Permission nodes: PermissionAPI.registerNode when the server has started");
        *///?}
        platform.add("Server: " + (running ? "running" : "not running"));

        List<CommandLibDiagnostics.Registration> registrations = new ArrayList<>();
        for (CommandLib registration : REGISTRATIONS) {
            CommandDocs docs = registration.docs();
            registrations.add(new CommandLibDiagnostics.Registration(registration.permissionPrefix,
                                                                     running ? "server dispatcher"
                                                                             : "waiting for the server",
                                                                     running ? docs.labels() : List.of(),
                                                                     docs));
        }
        return new CommandLibDiagnostics(platform, registrations);
    }

    //? if fabric {
    /*private static String loaderVersion(String modId) {
        return FabricLoader.getInstance()
                           .getModContainer(modId)
                           .map(ModContainer::getMetadata)
                           .map(x -> x.getVersion()
                                      .getFriendlyString())
                           .orElse("(unknown)");
    }
    *///?} else {
    private static String loaderVersion(String modId) {
        ModList mods = ModList.get();
        if (mods == null) {
            return "(mods not loaded)";
        }
        return mods.getModContainerById(modId)
                   .map(x -> x.getModInfo()
                              .getVersion()
                              .toString())
                   .orElse("(unknown)");
    }
    //?}

    private static String minecraftVersion() {
        try {
            return SharedConstants.getCurrentVersion()
                                  .getName();
        } catch (RuntimeException e) {
            // The version is detected when the game bootstraps, which tests may skip.
            return "(unknown)";
        }
    }

    /**
     * Sends the command tree to the player again, so the commands and branches they see follow a change of their
     * permissions without a rejoin. The server sends the tree on join and when the player's operator status changes,
     * but not when a permission handler changes a permission. Called from another thread, it sends the tree on the
     * server thread later.
     */
    public static void refresh(@NotNull ServerPlayer player) {
        MinecraftServer server = player.server;
        // execute runs at once on the server thread and queues the task from other threads.
        server.execute(() -> server.getCommands()
                                   .sendCommands(player));
    }

    /**
     * Same as {@link #refresh(ServerPlayer)} for every player on the running server. Does nothing when no server runs.
     */
    public static void refreshAll() {
        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return;
        }
        server.execute(() -> server.getPlayerList()
                                   .getPlayers()
                                   .forEach(player -> server.getCommands()
                                                            .sendCommands(player)));
    }

    //? if fabric {
    /*private CommandLib(Collection<? extends Command> commands, String permissionPrefix) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;
        // The first command that declares a node decides its default, as on Forge.
        commands.forEach(command -> command.permissionConfigs(permissionPrefix)
                                           .forEach(c -> PERMISSIONS.putIfAbsent(c.node(), c.defaultPermission())));

        // The callback fires again on /reload and on every integrated server start.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher.getRoot()));

        MinecraftServer server = CurrentServer.get();
        if (server != null) {
            // The current dispatcher has already been built, so add the commands to it directly.
            register(server.getCommands()
                           .getDispatcher()
                           .getRoot());
            server.getPlayerList()
                  .getPlayers()
                  .forEach(player -> server.getCommands()
                                           .sendCommands(player));
        }
    }

    private void register(RootCommandNode<CommandSourceStack> root) {
        new CommandNodeCreator<>(commands, permissionPrefix, ServerCommandScheduler.INSTANCE).build()
                                                                                             .forEach(root::addChild);
    }

    static boolean hasPermission(CommandSourceStack source, ServerPlayer player, String node) {
        DefaultPermission defaultPermission = PERMISSIONS.get(node);
        boolean fallback = defaultPermission == null ? source.hasPermission(4)
                                                     : Command.resolveDefault(defaultPermission, player);
        // fabric-permissions-api is optional: a mod that wants permission handlers such as LuckPerms ships it.
        if (FabricLoader.getInstance()
                        .isModLoaded("fabric-permissions-api-v0")) {
            return Permissions.check(player, node, fallback);
        }
        return fallback;
    }
    *///?} elif >=1.18 {
    private CommandLib(Collection<? extends Command> commands, String permissionPrefix) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;

        // Always listen: RegisterCommandsEvent fires again on /reload and on every integrated server start,
        // and PermissionGatherEvent.Nodes may still be pending when the server object already exists.
        //? if neoforge {
        /*NeoForge.EVENT_BUS.register(this);
        *///?} else
        MinecraftForge.EVENT_BUS.register(this);

        MinecraftServer server = CurrentServer.get();
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
        new CommandNodeCreator<>(commands, permissionPrefix, ServerCommandScheduler.INSTANCE).build()
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
    //?} else {
    /*private CommandLib(Collection<? extends Command> commands, String permissionPrefix) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;

        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            register(server);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(FMLServerStartedEvent e) {
        register(e.getServer());
    }

    private void register(MinecraftServer server) {
        RootCommandNode<CommandSourceStack> root = server.getCommands()
                                                         .getDispatcher()
                                                         .getRoot();

        new CommandNodeCreator<>(commands, permissionPrefix, ServerCommandScheduler.INSTANCE).build()
                                                            .forEach(root::addChild);

        commands.stream()
                .flatMap(x -> x.permissionEntries(permissionPrefix)
                               .stream())
                .forEach(e -> PermissionAPI.registerNode(e.getKey(), e.getValue(), ""));
    }
    *///?}
}
