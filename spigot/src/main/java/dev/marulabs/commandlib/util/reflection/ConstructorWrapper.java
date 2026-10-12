package dev.marulabs.commandlib.util.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorWrapper<T> {
    private final Constructor<T> constructor;

    public ConstructorWrapper(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    public T newInstance(Object... parameters) {
        try {
            return constructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
