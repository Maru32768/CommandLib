package dev.marulabs.commandlib.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtil {
    public static Field getFieldIncludingSuperclasses(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                throw e;
            }
            return getFieldIncludingSuperclasses(superclass, name);
        }
    }

    public static Method getMethodIncludingSuperclasses(Class<?> clazz,
                                                        String name,
                                                        Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                throw e;
            }
            return getMethodIncludingSuperclasses(superclass, name, parameterTypes);
        }
    }

    public static List<Method> getMethodsByNameIncludingSuperclasses(Class<?> clazz, String name) {
        List<Method> list = new ArrayList<>();

        Class<?> target = clazz;
        while (target != null) {
            Arrays.stream(target.getDeclaredMethods())
                  .filter(x -> x.getName()
                                .equals(name))
                  .forEach(list::add);

            target = target.getSuperclass();
        }

        return list;
    }

    public static <T> ConstructorWrapper<T> getConstructor(Class<T> clazz, Class<?>... parameterClasses) {
        try {
            return new ConstructorWrapper<>(clazz.getConstructor(parameterClasses));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private ReflectionUtil() {
    }
}
