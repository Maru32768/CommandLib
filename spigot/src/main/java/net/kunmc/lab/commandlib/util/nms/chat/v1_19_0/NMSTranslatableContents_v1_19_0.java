package net.kunmc.lab.commandlib.util.nms.chat.v1_19_0;

import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMSTranslatableContents_v1_19_0 extends NMSTranslatableContents {
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
        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == Object[].class) {
                try {
                    return (Object[]) method.invoke(getHandle());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalStateException("No arguments getter in " + clazz.getName());
    }
}
