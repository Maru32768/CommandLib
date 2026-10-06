package net.kunmc.lab.commandlib.util.nms;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.exception.MethodNotFoundException;
import net.kunmc.lab.commandlib.util.nms.exception.NMSClassNotAssignableException;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NMSClass {
    private static final Map<List<Object>, Method> METHODS_BY_SIGNATURE = new ConcurrentHashMap<>();
    private final Object handle;
    protected final Class<?> clazz;

    public NMSClass(@Nullable Object handle, @NotNull Class<?> clazz) {
        this.handle = handle;
        this.clazz = Objects.requireNonNull(clazz);

        if (handle != null && !clazz.isAssignableFrom(handle.getClass())) {
            throw new NMSClassNotAssignableException(handle, clazz);
        }
    }

    public final Object getHandle() {
        return handle;
    }

    public final Class<?> getFoundClass() {
        return clazz;
    }

    public final Object newInstance(Class<?>[] parameterTypes, Object[] args) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public final Object invokeMethod(String methodName, Object... args) {
        return invokeMethod(new String[]{methodName}, args);
    }

    public final Object invokeStaticMethod(String methodName, Object... args) {
        return invokeStaticMethod(new String[]{methodName}, args);
    }

    public final Object invokeMethod(String methodName, String methodName2, Object... args) {
        return invokeMethod(new String[]{methodName, methodName2}, args);
    }

    public final Object invokeStaticMethod(String methodName, String methodName2, Object... args) {
        return invokeStaticMethod(new String[]{methodName, methodName2}, args);
    }

    public final Object invokeMethod(String methodName, String methodName2, String methodName3, Object... args) {
        return invokeMethod(new String[]{methodName, methodName2, methodName3}, args);
    }

    public final Object invokeStaticMethod(String methodName, String methodName2, String methodName3, Object... args) {
        return invokeStaticMethod(new String[]{methodName, methodName2, methodName3}, args);
    }

    public final Object invokeMethod(String[] methodNames, Object... args) {
        Class<?>[] argClasses = Arrays.stream(args)
                                      .map(Object::getClass)
                                      .toArray(Class[]::new);

        return invokeMethod(methodNames, argClasses, args);
    }

    public final Object invokeStaticMethod(String[] methodNames, Object... args) {
        Class<?>[] argClasses = Arrays.stream(args)
                                      .map(Object::getClass)
                                      .toArray(Class[]::new);

        return invokeStaticMethod(methodNames, argClasses, args);
    }

    public final Object invokeMethod(String methodName, Class<?>[] parameterClasses, Object... args) {
        return invokeMethod(new String[]{methodName}, parameterClasses, args);
    }

    public final Object invokeStaticMethod(String methodName, Class<?>[] parameterClasses, Object... args) {
        return invokeStaticMethod(new String[]{methodName}, parameterClasses, args);
    }

    public final Object invokeMethod(String[] methodNames, Class<?>[] parameterClasses, Object... args) {
        return invokeMethod(handle, methodNames, parameterClasses, args);
    }

    public final Object invokeStaticMethod(String[] methodNames, Class<?>[] parameterClasses, Object... args) {
        return invokeMethod(null, methodNames, parameterClasses, args);
    }

    private Object invokeMethod(Object handle, String[] methodNames, Class<?>[] parameterClasses, Object... args) {
        Method method = null;
        for (String methodName : methodNames) {
            try {
                method = getMethod(methodName, parameterClasses);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }

        if (method == null) {
            throw new MethodNotFoundException(methodNames);
        }

        return invoke(handle, method, args);
    }

    /**
     * Invokes a method found by {@link #findMethodByReturnType} on the handle, or statically when the method is static.
     */
    protected final Object invokeFoundMethod(Method method, Object... args) {
        return invoke(Modifier.isStatic(method.getModifiers()) ? null : handle, method, args);
    }

    /**
     * Finds the public method of this class with the given return type and parameter types. Obfuscated method names
     * move between releases while the signatures stay, so this selects a method by its signature instead. Callers
     * resolve the return type through {@link NMSReflection} so that hybrid servers remapping class names find it too.
     * Methods declaring no exceptions are preferred, so that "or fail" variants are chosen only when no plain getter
     * exists.
     */
    protected final Method findMethodByReturnType(boolean isStatic, Class<?> returnType, Class<?>... parameterTypes) {
        List<Object> key = List.of(clazz, isStatic, returnType, List.of(parameterTypes));
        return METHODS_BY_SIGNATURE.computeIfAbsent(key, k -> {
            Method found = Arrays.stream(clazz.getMethods())
                                 .filter(x -> Modifier.isStatic(x.getModifiers()) == isStatic)
                                 .filter(x -> x.getReturnType() == returnType)
                                 .filter(x -> Arrays.equals(x.getParameterTypes(), parameterTypes))
                                 .min(Comparator.comparingInt(x -> x.getExceptionTypes().length))
                                 .orElseThrow(() -> new MethodNotFoundException(new String[]{String.format(
                                         "%s method of %s returning %s with parameters %s",
                                         isStatic ? "static" : "instance",
                                         clazz.getName(),
                                         returnType.getName(),
                                         Arrays.toString(parameterTypes))}));
            found.setAccessible(true);
            return found;
        });
    }

    private static Object invoke(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();

            if (cause instanceof CommandSyntaxException) {
                throw new UncheckedCommandSyntaxException(((CommandSyntaxException) cause));
            }

            throw new RuntimeException(e);
        }
    }

    public final Object getValue(String name, String... names) {
        return getValue(Object.class, name, names);
    }

    public final <T> T getValue(Class<T> tClass, String name, String... names) {
        List<String> list = Stream.concat(Stream.of(name), Stream.of(names))
                                  .collect(Collectors.toList());
        return list.stream()
                   .map(x -> {
                       try {
                           return ReflectionUtil.getFieldIncludingSuperclasses(clazz, x);
                       } catch (NoSuchFieldException e) {
                           return null;
                       }
                   })
                   .filter(Objects::nonNull)
                   .map(x -> {
                       x.setAccessible(true);
                       try {
                           return tClass.cast(x.get(handle));
                       } catch (IllegalAccessException e) {
                           return null;
                       }
                   })
                   .filter(Objects::nonNull)
                   .findFirst()
                   .orElseThrow(() -> new RuntimeException(String.format("Could not find fields %s", list)));
    }

    private Method getMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ReflectionUtil.getMethodIncludingSuperclasses(clazz, methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
