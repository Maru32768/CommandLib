package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What one plugin's registrations share: how their commands reach the dispatcher, and the work they schedule once per
 * tick. {@link #mode}, {@link #instances} and the state of each instance are guarded by this object.
 */
final class PluginRegistrationState {
    Mode mode = new Mode.Unhandled();
    // The instances not unregistered yet, which the plugin's COMMANDS handler adds on every data reload.
    final List<CommandLib> instances = new ArrayList<>();
    // Main thread only: the changes of one tick share one update of the players' command trees and one compilation.
    private boolean commandTreeUpdateScheduled;
    private boolean functionRecompileScheduled;

    /**
     * How the plugin's commands reach Paper's dispatcher.
     */
    sealed interface Mode {
        /**
         * No COMMANDS handler, before the first registration or after Paper refused one and no dispatcher was cached
         * for a direct add; the next registration tries again.
         */
        record Unhandled() implements Mode {
        }

        /**
         * The plugin's COMMANDS handler adds the commands, on startup and on every data reload.
         *
         * @param dispatcher the dispatcher the commands are in, null until the event fires
         */
        record Handled(@Nullable CommandDispatcher<CommandSourceStack> dispatcher) implements Mode {
        }

        /**
         * Registered after the plugin's initialization, when Paper refuses a handler, so the commands are added to the
         * live dispatcher, and another plugin's handler adds them again to the dispatcher of a data reload.
         */
        record Direct(CommandDispatcher<CommandSourceStack> dispatcher) implements Mode {
        }

        /**
         * The dispatcher the commands are in, or null when they are in none yet.
         */
        @Nullable
        default CommandDispatcher<CommandSourceStack> dispatcher() {
            return switch (this) {
                case Unhandled ignored -> null;
                case Handled handled -> handled.dispatcher();
                case Direct direct -> direct.dispatcher();
            };
        }
    }

    /**
     * Sends the players their command trees after a change to the live dispatcher, on the next tick so that the changes
     * of one tick share one update, or at once while the plugin is being disabled. Paper builds the trees on an
     * asynchronous pool while the main thread goes on, which a change of the root's children maps would break
     * (ConcurrentModificationException), so the changes replace those maps instead
     * ({@link PaperDispatcher#replaceChildren}).
     */
    void updateCommandTrees(Plugin plugin, boolean later) {
        if (!later) {
            if (!Bukkit.isStopping()) {
                sendCommandTrees();
            }
            return;
        }
        if (commandTreeUpdateScheduled) {
            return;
        }
        commandTreeUpdateScheduled = true;
        Bukkit.getGlobalRegionScheduler()
              .execute(plugin, () -> {
                  commandTreeUpdateScheduled = false;
                  sendCommandTrees();
              });
    }

    /**
     * Compiles the data pack functions again on the next tick, once for the changes of a tick, after the server compiled
     * them without the commands ({@link ServerFunctions}). Bukkit and global region tasks run before the server runs
     * functions in a tick, so the #load functions of a data reload run once, with the commands.
     */
    void recompileFunctionsLater(Plugin plugin) {
        // Counted even when a compilation is scheduled, since another copy of CommandLib may compile before it.
        FunctionCompilationMarker.changed();
        if (functionRecompileScheduled || !plugin.isEnabled()) {
            return;
        }
        functionRecompileScheduled = true;
        Bukkit.getGlobalRegionScheduler()
              .execute(plugin, () -> {
                  functionRecompileScheduled = false;
                  ServerFunctions.recompileFunctions(plugin.getLogger());
              });
    }

    private static void sendCommandTrees() {
        Bukkit.getOnlinePlayers()
              .forEach(Player::updateCommands);
    }
}
