package net.kunmc.lab.commandlib.util.nms.chat.v1_19_0;

import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NMSTranslatableContents_v1_19_0 extends NMSTranslatableContents {
    private static final Map<Class<?>, Method> ARGS_GETTERS = new ConcurrentHashMap<>();

    public NMSTranslatableContents_v1_19_0(Object handle) {
        super(handle, "network.chat.contents.TranslatableContents");
    }

    @Override
    public String getKey() {
        return ((String) invokeMethod("a"));
    }

    @Override
    public Object[] getArgs() {
        // The arguments getter is "b" in 1.19.0-1.19.2 and "c" from 1.19.4, where "b" became the fallback text.
        // It is the only no-argument method returning Object[].
        Method getter = ARGS_GETTERS.computeIfAbsent(clazz, NMSTranslatableContents_v1_19_0::findArgsGetter);
        try {
            return (Object[]) getter.invoke(getHandle());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method findArgsGetter(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == Object[].class) {
                return method;
            }
        }
        throw new IllegalStateException("No arguments getter in " + clazz.getName());
    }
}
