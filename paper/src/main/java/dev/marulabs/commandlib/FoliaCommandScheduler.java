package dev.marulabs.commandlib;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Runs async commands on Folia, which has no main thread and refuses the Bukkit scheduler. The sync executor is the
 * thread that owns the command's source, as Folia dispatches the command there: the entity scheduler of the entity it
 * runs as ({@code /execute as} included), the region of a command block, and the global region for the console. A task
 * given on that thread runs at once. Async work runs on the async scheduler.
 *
 * <p>The follow-up for an entity that was removed, such as a player who left, runs on the global region thread instead,
 * so the post-hooks of the middlewares still run.
 */
@SuppressWarnings("UnstableApiUsage")
final class FoliaCommandScheduler implements CommandScheduler {
    // Folia's RegionizedServer#isGlobalTickThread. Paper has no such class, and its API has the Bukkit method only from
    // 1.21.1.
    private static final @Nullable MethodHandle IS_GLOBAL_TICK_THREAD = findIsGlobalTickThread();
    /**
     * Whether the server is Folia.
     */
    static final boolean ON_FOLIA = IS_GLOBAL_TICK_THREAD != null;
    // Folia's TickRegionScheduler#getCurrentRegion: the region the calling thread ticks, null on the global region thread
    // and outside the tick loop, such as while plugins are enabled.
    private static final @Nullable Method GET_CURRENT_REGION = findGetCurrentRegion();

    private final Supplier<Plugin> plugin;
    private final BooleanSupplier globalTickThread;

    /**
     * @param plugin           the plugin, looked up when a task is scheduled, since a bootstrapper registers commands
     *                         before the plugin exists
     * @param globalTickThread whether the calling thread is the global region thread
     */
    FoliaCommandScheduler(Supplier<Plugin> plugin, BooleanSupplier globalTickThread) {
        this.plugin = plugin;
        this.globalTickThread = globalTickThread;
    }

    /**
     * The scheduler for this server: Folia's schedulers on Folia, the Bukkit scheduler on Paper.
     */
    static CommandScheduler forServer(Supplier<Plugin> plugin) {
        MethodHandle isGlobalTickThread = IS_GLOBAL_TICK_THREAD;
        if (isGlobalTickThread == null) {
            return new BukkitCommandScheduler(plugin);
        }
        return new FoliaCommandScheduler(plugin, () -> {
            try {
                return (boolean) isGlobalTickThread.invokeExact();
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public Executor sync(CommonCommandContext<?, ?> ctx) {
        CommandSourceStack source = (CommandSourceStack) ctx.getHandle()
                                                            .getSource();
        Entity entity = source.getExecutor();
        if (entity != null) {
            return task -> {
                if (Bukkit.isOwnedByCurrentRegion(entity)) {
                    task.run();
                } else {
                    // The entity scheduler drops the task of a removed entity: retired runs if it is removed before the
                    // task, and execute returns false if it already was.
                    Runnable global = () -> Bukkit.getGlobalRegionScheduler()
                                                  .execute(plugin(), task);
                    if (!entity.getScheduler()
                               .execute(plugin(), task, global, 1L)) {
                        global.run();
                    }
                }
            };
        }
        if (source.getSender() instanceof BlockCommandSender block) {
            Location location = block.getBlock()
                                     .getLocation();
            return task -> {
                if (Bukkit.isOwnedByCurrentRegion(location)) {
                    task.run();
                } else {
                    Bukkit.getRegionScheduler()
                          .execute(plugin(), location, task);
                }
            };
        }
        return task -> {
            if (globalTickThread.getAsBoolean()) {
                task.run();
            } else {
                Bukkit.getGlobalRegionScheduler()
                      .execute(plugin(), task);
            }
        };
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return task -> Bukkit.getAsyncScheduler()
                             .runNow(plugin(), ignored -> task.run());
    }

    /**
     * Whether the calling thread ticks a region other than the global region. Always false on Paper.
     */
    static boolean onRegionThread() {
        if (GET_CURRENT_REGION == null) {
            return false;
        }
        try {
            return GET_CURRENT_REGION.invoke(null) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Plugin plugin() {
        return Objects.requireNonNull(plugin.get());
    }

    @Nullable
    private static Method findGetCurrentRegion() {
        try {
            return Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler",
                                 false,
                                 FoliaCommandScheduler.class.getClassLoader())
                        .getMethod("getCurrentRegion");
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findIsGlobalTickThread() {
        try {
            return MethodHandles.publicLookup()
                                .findStatic(Class.forName("io.papermc.paper.threadedregions.RegionizedServer",
                                                          false,
                                                          FoliaCommandScheduler.class.getClassLoader()),
                                            "isGlobalTickThread",
                                            MethodType.methodType(boolean.class));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
