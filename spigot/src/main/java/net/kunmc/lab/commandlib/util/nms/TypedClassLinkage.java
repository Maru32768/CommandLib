package net.kunmc.lab.commandlib.util.nms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks that every class, field and method a typed NMS class refers to exists on the running server, so
 * {@link NMSClassRegistry} can fall back to the reflection implementation instead of failing with a
 * {@link LinkageError} at command time. The JVM resolves those references lazily, so loading the class alone does not
 * tell. A server inside a typed range can still differ from the jar the class was compiled against, such as Spigot
 * 1.20.5-1.20.6 for the Mojang-named Paper classes, a fork, or a hybrid server.
 *
 * <p>The check reads the constant pool of the class file and of the classes it uses from the same package. It looks
 * up members by name and descriptor without initializing any class.
 */
final class TypedClassLinkage {
    private static final Map<String, Boolean> LINKABLE = new ConcurrentHashMap<>();

    private TypedClassLinkage() {
    }

    static boolean isLinkable(Class<?> typedClass) {
        return LINKABLE.computeIfAbsent(typedClass.getName(), x -> check(typedClass));
    }

    private static boolean check(Class<?> typedClass) {
        ClassLoader loader = typedClass.getClassLoader();
        String packagePrefix = typedClass.getName()
                                         .substring(0,
                                                    typedClass.getName()
                                                              .lastIndexOf('.') + 1);
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(typedClass.getName());
        try {
            while (!pending.isEmpty()) {
                String className = pending.poll();
                if (!visited.add(className)) {
                    continue;
                }
                byte[] bytes = readClassFile(loader, className);
                if (bytes == null) {
                    return false;
                }
                for (String referenced : checkReferences(bytes, loader)) {
                    if (referenced.startsWith(packagePrefix) && referenced.indexOf('.', packagePrefix.length()) < 0) {
                        pending.add(referenced);
                    }
                }
            }
            return true;
        } catch (ClassNotFoundException | NoSuchMemberException | LinkageError | IOException e) {
            return false;
        }
    }

    private static byte[] readClassFile(ClassLoader loader, String className) throws IOException {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(resource) : loader.getResourceAsStream(
                resource)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * Resolves the references in the constant pool of a class file and returns the names of the classes it refers to.
     */
    static List<String> checkReferences(byte[] classFile,
                                        ClassLoader loader) throws IOException, ClassNotFoundException, NoSuchMemberException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(classFile));
        if (in.readInt() != 0xCAFEBABE) {
            throw new IOException("Not a class file");
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        int[] tags = new int[count];
        int[] first = new int[count];
        int[] second = new int[count];
        String[] utf8 = new String[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
                case 1:
                    utf8[i] = in.readUTF();
                    break;
                case 3:
                case 4:
                    in.readInt();
                    break;
                case 5:
                case 6:
                    in.readLong();
                    i++;
                    break;
                case 7:
                case 8:
                case 16:
                case 19:
                case 20:
                    first[i] = in.readUnsignedShort();
                    break;
                case 15:
                    first[i] = in.readUnsignedByte();
                    second[i] = in.readUnsignedShort();
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    first[i] = in.readUnsignedShort();
                    second[i] = in.readUnsignedShort();
                    break;
                default:
                    throw new IOException("Unknown constant pool tag " + tag);
            }
        }

        List<String> classNames = new ArrayList<>();
        for (int i = 1; i < count; i++) {
            if (tags[i] == 7) {
                Class<?> clazz = resolveInternalName(utf8[first[i]], loader);
                if (clazz != null && !clazz.isArray()) {
                    classNames.add(clazz.getName());
                }
            } else if (tags[i] == 9 || tags[i] == 10 || tags[i] == 11) {
                Class<?> owner = resolveInternalName(utf8[first[first[i]]], loader);
                int nameAndType = second[i];
                String name = utf8[first[nameAndType]];
                String descriptor = utf8[second[nameAndType]];
                if (owner == null || owner.isArray()) {
                    continue;
                }
                boolean found = tags[i] == 9 ? hasField(owner, name, descriptor) : hasMethod(owner, name, descriptor);
                if (!found) {
                    throw new NoSuchMemberException(owner.getName() + "." + name + descriptor);
                }
            }
        }
        return classNames;
    }

    private static Class<?> resolveInternalName(String internalName,
                                                ClassLoader loader) throws ClassNotFoundException {
        String name = internalName;
        while (name.startsWith("[")) {
            name = name.substring(1);
        }
        if (name.length() == 1) {
            return null;
        }
        if (name.startsWith("L") && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        }
        return Class.forName(name.replace('/', '.'), false, loader);
    }

    private static boolean hasField(Class<?> owner, String name, String descriptor) {
        for (Class<?> type : supertypes(owner)) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName()
                         .equals(name) && descriptor(field.getType()).equals(descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMethod(Class<?> owner, String name, String descriptor) {
        if (name.equals("<init>")) {
            for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
                if (descriptor(constructor.getParameterTypes(), void.class).equals(descriptor)) {
                    return true;
                }
            }
            return false;
        }
        for (Class<?> type : supertypes(owner)) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName()
                           .equals(name)) {
                    continue;
                }
                // MethodHandle.invoke and the like take any arguments at each call site.
                if (method.isVarArgs() && Modifier.isNative(method.getModifiers()) && type.getName()
                                                                                          .startsWith(
                                                                                                  "java.lang.invoke.")) {
                    return true;
                }
                if (descriptor(method.getParameterTypes(), method.getReturnType()).equals(descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The class, its superclasses and its superinterfaces, then {@link Object}, which interface method references can
     * also name.
     */
    private static List<Class<?>> supertypes(Class<?> owner) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            Class<?> type = pending.poll();
            if (!result.add(type)) {
                continue;
            }
            if (type.getSuperclass() != null) {
                pending.add(type.getSuperclass());
            }
            pending.addAll(Arrays.asList(type.getInterfaces()));
        }
        result.add(Object.class);
        return new ArrayList<>(result);
    }

    private static String descriptor(Class<?>[] parameterTypes, Class<?> returnType) {
        StringBuilder builder = new StringBuilder("(");
        for (Class<?> parameterType : parameterTypes) {
            builder.append(descriptor(parameterType));
        }
        return builder.append(')')
                      .append(descriptor(returnType))
                      .toString();
    }

    private static String descriptor(Class<?> type) {
        if (type.isArray()) {
            return "[" + descriptor(type.getComponentType());
        }
        if (!type.isPrimitive()) {
            return "L" + type.getName()
                             .replace('.', '/') + ";";
        }
        if (type == void.class) {
            return "V";
        }
        if (type == boolean.class) {
            return "Z";
        }
        if (type == byte.class) {
            return "B";
        }
        if (type == char.class) {
            return "C";
        }
        if (type == short.class) {
            return "S";
        }
        if (type == int.class) {
            return "I";
        }
        if (type == long.class) {
            return "J";
        }
        if (type == float.class) {
            return "F";
        }
        return "D";
    }

    static final class NoSuchMemberException extends Exception {
        NoSuchMemberException(String member) {
            super(member);
        }
    }
}
