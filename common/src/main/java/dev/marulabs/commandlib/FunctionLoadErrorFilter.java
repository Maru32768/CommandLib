package dev.marulabs.commandlib;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Drops the "Failed to load function" errors that the server logs when it compiles data pack functions without
 * CommandLib's commands, while CommandLib is going to compile them again with the commands. Bukkit servers compile the
 * functions on a data reload before a plugin can add commands to the new dispatcher (Spigot has no hook, and Paper fires
 * the COMMANDS event of plugins afterwards), so every function using a CommandLib command was reported as broken on
 * each reload although it works a tick later. The errors of CommandLib's own compilation, whose stack has a
 * {@value #RECOMPILE_METHOD} method, pass, so a function that is really broken is still reported, once. A function tag,
 * such as {@code #minecraft:load}, that lists such a function was reported as missing it as well, so the error of a
 * function tag is dropped the same way.
 *
 * <p>It is a context-wide Log4j filter, which the logger checks in the logging thread before the event is made, even
 * with asynchronous loggers, so only its variants for a logger call are needed. Servers log through Log4j, which plugins see from the server's class loader; without it
 * nothing is filtered.
 */
final class FunctionLoadErrorFilter extends AbstractFilter {
    /**
     * The format of the error the function library logs for a function it could not compile, the same from 1.16.5.
     */
    static final String MESSAGE = "Failed to load function {}";
    /**
     * The formats of the error the tag loader logs for a tag that lists a missing element: with the tag type on 1.16.5,
     * without it on later versions such as 1.21.11 and 26.3. The function library builds its tags with the functions it compiled.
     */
    static final List<String> TAG_MESSAGES = List.of("Couldn't load {} tag {} as it is missing following references: {}",
                                                     "Couldn't load tag {} as it is missing following references: {}");
    /**
     * The function library, whose stack frames tell a function tag from the tags of other registries: its Spigot,
     * Mojang and MCP (Mohist 1.16.5) names.
     */
    private static final List<String> FUNCTION_LIBRARIES = List.of("CustomFunctionManager",
                                                                   "ServerFunctionLibrary",
                                                                   "FunctionReloader");
    /**
     * The name of the methods that compile the functions again: NMSDedicatedServer#recompileFunctions in the spigot
     * module and ServerFunctions#recompileFunctions in the paper module. Methods are matched by name, so the filter of
     * one plugin's shaded copy of CommandLib also lets the errors of another copy pass.
     */
    static final String RECOMPILE_METHOD = "recompileFunctions";

    // Set while a function load error is judged, whose checks may log through this filter again.
    private static final ThreadLocal<Boolean> DECIDING = ThreadLocal.withInitial(() -> false);

    private final Configuration configuration;
    private final BooleanSupplier recompiling;

    private FunctionLoadErrorFilter(Configuration configuration, BooleanSupplier recompiling) {
        this.configuration = configuration;
        this.recompiling = recompiling;
    }

    /**
     * Adds a filter to the Log4j configuration of the server.
     *
     * @param serverClassLoader the class loader of the server, whose Log4j context the server logs to
     * @param recompiling       whether CommandLib compiles the functions again after the server's compilation, checked
     *                          when the error is logged
     * @return the filter, or null when the server does not log through this Log4j
     */
    @Nullable
    static FunctionLoadErrorFilter install(ClassLoader serverClassLoader, BooleanSupplier recompiling) {
        org.apache.logging.log4j.spi.LoggerContext context = LogManager.getContext(serverClassLoader, false);
        if (!(context instanceof LoggerContext)) {
            return null;
        }
        Configuration configuration = ((LoggerContext) context).getConfiguration();
        FunctionLoadErrorFilter filter = new FunctionLoadErrorFilter(configuration, recompiling);
        filter.start();
        // A class loader may log each class it loads, as ModLauncher on Mohist does, which would call the filter again
        // while it resolves a class, endlessly. So every path runs once before the filter is added.
        filter.filter(null, null, null, (String) null, (Object[]) null);
        filter.filter(null, null, null, (Message) null, null);
        filter.decide(MESSAGE);
        filter.decide(TAG_MESSAGES.get(0));
        // decide checks the stack only while CommandLib is going to compile the functions again.
        fromRecompilation();
        fromFunctionLibrary();
        // A composite filter stops at the first filter that accepts or denies, and Forge's configuration, which Mohist
        // uses, has filters that accept errors, so this one goes first.
        Filter existing = configuration.getFilter();
        if (existing != null) {
            configuration.removeFilter(existing);
        }
        configuration.addFilter(filter);
        if (existing != null) {
            configuration.addFilter(existing);
        }
        return filter;
    }

    void uninstall() {
        configuration.removeFilter(this);
        stop();
    }

    @Override
    // Log4j passes null for what a call does not give, such as the marker.
    public Result filter(@Nullable Logger logger,
                         @Nullable Level level,
                         @Nullable Marker marker,
                         @Nullable String msg,
                         @Nullable Object @Nullable... params) {
        return decide(msg);
    }

    @Override
    public Result filter(@Nullable Logger logger,
                         @Nullable Level level,
                         @Nullable Marker marker,
                         @Nullable Message msg,
                         @Nullable Throwable t) {
        return decide(msg == null ? null : msg.getFormat());
    }

    Result decide(@Nullable String format) {
        // List.of rejects a null element even in contains.
        boolean tag = format != null && TAG_MESSAGES.contains(format);
        if (!(tag || MESSAGE.equals(format)) || DECIDING.get()) {
            return Result.NEUTRAL;
        }
        DECIDING.set(true);
        try {
            return recompiling.getAsBoolean() && !fromRecompilation() && (!tag || fromFunctionLibrary()) ? Result.DENY
                    : Result.NEUTRAL;
        } finally {
            DECIDING.set(false);
        }
    }

    private static boolean fromFunctionLibrary() {
        return StackWalker.getInstance()
                          .walk(frames -> frames.anyMatch(frame -> FUNCTION_LIBRARIES.stream()
                                                                                     .anyMatch(frame.getClassName()::endsWith)));
    }

    private static boolean fromRecompilation() {
        return StackWalker.getInstance()
                          .walk(frames -> frames.anyMatch(frame -> frame.getMethodName()
                                                                        .equals(RECOMPILE_METHOD)));
    }
}
