package dev.marulabs.commandlib.util.nms.access;

import java.lang.reflect.Field;

/**
 * Calls the JDK's caller-sensitive reflection methods, as the typed classes that read private server fields do.
 */
public class ReflectiveCaller {
    public Object firstField(Object target) throws IllegalAccessException {
        Field field = target.getClass()
                            .getDeclaredFields()[0];
        field.setAccessible(true);
        return field.get(target);
    }
}
