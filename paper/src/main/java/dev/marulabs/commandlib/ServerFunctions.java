package dev.marulabs.commandlib;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compiles the data pack functions again. Paper fires the COMMANDS event for plugins after the function library has
 * compiled the functions, at startup and on every data reload, so a function using a command registered from a plugin
 * was dropped. The library compiles with the dispatcher that the plugin commands are added to, so compiling it again
 * is enough. Paper has no API for it; the server classes are read by their Mojang names, which Paper runs with from
 * 1.20.5.
 */
final class ServerFunctions {
    // Main thread only: set after a failure, which would repeat on every attempt.
    private static boolean unavailable;

    private ServerFunctions() {
    }

    static boolean available() {
        return !unavailable;
    }

    /**
     * Named as {@link FunctionLoadErrorFilter#RECOMPILE_METHOD}, so the errors of this compilation are logged.
     */
    static void recompileFunctions(Logger logger) {
        if (unavailable) {
            return;
        }
        try {
            recompileFunctions();
        } catch (ReflectiveOperationException | RuntimeException e) {
            unavailable = true;
            logger.log(Level.WARNING,
                       "Could not compile data pack functions again, so functions cannot use commands registered from"
                               + " plugins",
                       e);
        }
    }

    private static void recompileFunctions() throws ReflectiveOperationException {
        Object server = Bukkit.getServer()
                              .getClass()
                              .getMethod("getServer")
                              .invoke(Bukkit.getServer());
        Class<?> minecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
        Field resourcesField = minecraftServer.getDeclaredField("resources");
        resourcesField.setAccessible(true);
        Object resources = resourcesField.get(server);
        Object resourceManager = invoke(resources, "resourceManager");
        Object library = invoke(invoke(resources, "managers"), "getFunctionLibrary");

        Method reload = null;
        for (Method method : library.getClass()
                                    .getMethods()) {
            if (method.getName()
                      .equals("reload")
                    && method.getReturnType() == CompletableFuture.class) {
                reload = method;
            }
        }
        if (reload == null) {
            throw new NoSuchMethodException(library.getClass()
                                                   .getName()
                    + ".reload");
        }
        Class<?>[] parameterTypes = reload.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            arguments[i] = reloadArgument(parameterTypes[i], resourceManager);
        }
        ((CompletableFuture<?>) reload.invoke(library, arguments)).join();

        Object functionManager = minecraftServer.getMethod("getFunctions")
                                                .invoke(server);
        functionManager.getClass()
                       .getMethod("replaceLibrary", library.getClass())
                       .invoke(functionManager, library);
    }

    /**
     * An argument of ServerFunctionLibrary#reload, whose parameters changed between releases: (PreparationBarrier,
     * ResourceManager, ProfilerFiller, ProfilerFiller, Executor, Executor) up to 1.21.1, without the profilers from
     * 1.21.2, and (SharedState, Executor, PreparationBarrier, Executor) later. Both executors run the work at once.
     */
    private static Object reloadArgument(Class<?> type, Object resourceManager) throws ReflectiveOperationException {
        String name = type.getSimpleName();
        if (type == Executor.class) {
            Executor direct = Runnable::run;
            return direct;
        }
        if (type.isInstance(resourceManager)) {
            return resourceManager;
        }
        if (name.equals("PreparationBarrier")) {
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "equals":
                        return proxy == args[0];
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "toString":
                        return "CommandLib function recompilation";
                    default:
                        // wait(T): nothing else to wait for.
                        return CompletableFuture.completedFuture(args[0]);
                }
            });
        }
        if (name.equals("ProfilerFiller")) {
            return Class.forName("net.minecraft.util.profiling.InactiveProfiler")
                        .getField("INSTANCE")
                        .get(null);
        }
        if (name.equals("SharedState")) {
            for (Constructor<?> constructor : type.getConstructors()) {
                if (constructor.getParameterCount() == 1
                        && constructor.getParameterTypes()[0].isInstance(resourceManager)) {
                    return constructor.newInstance(resourceManager);
                }
            }
        }
        throw new IllegalStateException("Unknown parameter of ServerFunctionLibrary#reload: " + type.getName());
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass()
                              .getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
