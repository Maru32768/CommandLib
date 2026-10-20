package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.commandlib.internal.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.internal.nms.server.NMSDataPackResources;
import dev.marulabs.commandlib.internal.nms.server.NMSDedicatedServer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.v1_16_R3.CustomFunctionManager;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.GameProfilerDisabled;
import net.minecraft.server.v1_16_R3.MinecraftKey;

public class NMSDedicatedServer_spigot_1_16_5 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_16_5(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).getCommandDispatcher());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).vanillaCommandDispatcher);
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(getDataPackResourcesHandle());
    }

    @Override
    public Object getDataPackResourcesHandle() {
        return ((DedicatedServer) requireHandle()).dataPackResources;
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher, boolean reloaded) {
        DedicatedServer server = (DedicatedServer) requireHandle();
        CustomFunctionManager library = server.dataPackResources.a();
        boolean loadMissing = reloaded && loadTagEmpty(library);
        setFunctionDispatcher(library, CustomFunctionManager.class, dispatcher);
        // Mohist remaps IReloadListener but not its inner barrier interface, so a class implementing it fails to link
        // there; the barrier is a proxy of the reload method's parameter type instead.
        Method reload = null;
        for (Method method : CustomFunctionManager.class.getMethods()) {
            if (method.getParameterCount() == 6 && method.getReturnType() == CompletableFuture.class) {
                reload = method;
            }
        }
        if (reload == null) {
            throw new IllegalStateException("CustomFunctionManager has no reload method");
        }
        Class<?> barrierType = reload.getParameterTypes()[0];
        Object barrier = Proxy.newProxyInstance(barrierType.getClassLoader(),
                                                new Class<?>[]{barrierType},
                                                (proxy, method, args) -> {
                                                    switch (method.getName()) {
                                                        case "equals":
                                                            return proxy == args[0];
                                                        case "hashCode":
                                                            return System.identityHashCode(proxy);
                                                        case "toString":
                                                            return "CommandLib function recompilation";
                                                        default:
                                                            return CompletableFuture.completedFuture(args[0]);
                                                    }
                                                });
        Executor direct = Runnable::run;
        try {
            ((CompletableFuture<?>) reload.invoke(library,
                                                  barrier,
                                                  server.dataPackResources.h(),
                                                  GameProfilerDisabled.a,
                                                  GameProfilerDisabled.a,
                                                  direct,
                                                  direct)).join();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        keepPendingLoad(server.getFunctionData(),
                        () -> server.getFunctionData().a(library),
                        loadMissing && !loadTagEmpty(library));
    }

    private static boolean loadTagEmpty(CustomFunctionManager library) {
        return library.b().b(new MinecraftKey("minecraft", "load")).getTagged().isEmpty();
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) requireHandle()).isRunning();
    }
}
