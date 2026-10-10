package net.kunmc.lab.commandlib.util.nms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks that every class, field and method a typed NMS class refers to resolves on the running server the way the
 * JVM would resolve it, so {@link NMSClassRegistry} can fall back to the reflection implementation instead of failing
 * with a {@link LinkageError} at command time. The JVM resolves those references lazily, so loading the class alone
 * does not tell. A server inside a typed range can still differ from the jar the class was compiled against, such as
 * Spigot 1.20.5-1.20.6 for the Mojang-named Paper classes, a fork, or a hybrid server.
 *
 * <p>The check covers the class and the classes it uses from the same package:
 * <ul>
 *     <li>It links each class without initializing it, so the verifier rejects a server class that no longer has
 *     the supertype the bytecode assigns it to.</li>
 *     <li>It reads the constant pool and the instructions that use each field and method reference, and resolves the
 *     member through a {@link MethodHandles.Lookup} of the class with the instruction's kind: instance or static,
 *     virtual, special or interface. A lookup resolves only that member, checks access as the bytecode would, and
 *     initializes no class.</li>
 * </ul>
 */
final class TypedClassLinkage {
    /** Why each checked class does not link, or an empty string when it links. */
    private static final Map<String, String> FAILURES = new ConcurrentHashMap<>();
    // Reference kinds of CONSTANT_MethodHandle (JVMS 4.4.8), also used for the instructions with the same meaning.
    private static final int GET_FIELD = 1;
    private static final int GET_STATIC = 2;
    private static final int PUT_FIELD = 3;
    private static final int PUT_STATIC = 4;
    private static final int INVOKE_VIRTUAL = 5;
    private static final int INVOKE_STATIC = 6;
    private static final int INVOKE_SPECIAL = 7;
    private static final int NEW_INVOKE_SPECIAL = 8;
    private static final int INVOKE_INTERFACE = 9;

    private TypedClassLinkage() {
    }

    static boolean isLinkable(Class<?> typedClass) {
        return failure(typedClass).isEmpty();
    }

    /**
     * Why the class does not link, such as the member that does not resolve, or an empty string when it links.
     */
    static String failure(Class<?> typedClass) {
        return FAILURES.computeIfAbsent(typedClass.getName(), x -> check(typedClass));
    }

    private static String check(Class<?> typedClass) {
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
                    return "no class file for " + className;
                }
                Class<?> clazz = Class.forName(className, false, loader);
                // HotSpot links, and so verifies, a class before it reflects its members, without initializing it.
                clazz.getDeclaredConstructors();
                for (String referenced : checkReferences(bytes, clazz)) {
                    if (referenced.startsWith(packagePrefix) && referenced.indexOf('.', packagePrefix.length()) < 0) {
                        pending.add(referenced);
                    }
                }
            }
            return "";
        } catch (ReflectiveOperationException | NoSuchMemberException | LinkageError | IOException |
                 RuntimeException e) {
            return e.toString();
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
     * Resolves the references in a class file as the given class, which the class file defines, and returns the names
     * of the classes it refers to.
     */
    static List<String> checkReferences(byte[] classFile,
                                        Class<?> lookupClass) throws IOException, ReflectiveOperationException, NoSuchMemberException {
        ClassFile file = ClassFile.parse(classFile);
        ClassLoader loader = lookupClass.getClassLoader();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(lookupClass, MethodHandles.lookup());

        List<String> classNames = new ArrayList<>();
        for (int i = 1; i < file.tags.length; i++) {
            if (file.tags[i] == 7) {
                Class<?> clazz = resolveInternalName(file.utf8[file.first[i]], loader);
                if (clazz != null) {
                    lookup.accessClass(clazz);
                    classNames.add(clazz.getName());
                }
            } else if (file.tags[i] == 9 || file.tags[i] == 10 || file.tags[i] == 11) {
                String ownerName = file.utf8[file.first[file.first[i]]];
                // An array type, as in a clone call on an array, only has the members of Object.
                if (ownerName.startsWith("[")) {
                    continue;
                }
                Class<?> owner = resolveInternalName(ownerName, loader);
                int nameAndType = file.second[i];
                String name = file.utf8[file.first[nameAndType]];
                String descriptor = file.utf8[file.second[nameAndType]];
                // Method resolution and interface method resolution fail when the owner is of the other kind.
                if (file.tags[i] != 9 && owner.isInterface() != (file.tags[i] == 11)) {
                    throw new NoSuchMemberException(owner.getName() + "." + name + descriptor);
                }
                // A reference that no instruction or method handle uses never resolves; javac does not emit one.
                BitSet kinds = file.kinds.getOrDefault(i, new BitSet());
                for (int kind = kinds.nextSetBit(0); kind >= 0; kind = kinds.nextSetBit(kind + 1)) {
                    try {
                        resolve(lookup, kind, owner, name, descriptor);
                    } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
                        throw new NoSuchMemberException(owner.getName() + "." + name + descriptor + ": " + e);
                    }
                }
            }
        }
        return classNames;
    }

    private static void resolve(MethodHandles.Lookup lookup,
                                int kind,
                                Class<?> owner,
                                String name,
                                String descriptor) throws ReflectiveOperationException {
        ClassLoader loader = lookup.lookupClass()
                                   .getClassLoader();
        MethodType type = methodType(descriptor, loader);
        switch (kind) {
            case GET_FIELD:
            case PUT_FIELD:
                // A setter lookup rejects final fields, which the class writes in its own constructor, so a write
                // resolves like a read.
                lookup.findGetter(owner, name, type.returnType());
                return;
            case GET_STATIC:
            case PUT_STATIC:
                lookup.findStaticGetter(owner, name, type.returnType());
                return;
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
                lookup.findVirtual(owner, name, type);
                return;
            case INVOKE_STATIC:
                lookup.findStatic(owner, name, type);
                return;
            case INVOKE_SPECIAL:
            case NEW_INVOKE_SPECIAL:
                if (!name.equals("<init>")) {
                    lookup.findSpecial(owner, name, type, lookup.lookupClass());
                } else if (owner == lookup.lookupClass()
                                          .getSuperclass()) {
                    // super(...) may call a protected constructor, which a lookup only allows within the package.
                    Constructor<?> constructor = owner.getDeclaredConstructor(type.parameterArray());
                    if (Modifier.isPrivate(constructor.getModifiers())) {
                        throw new IllegalAccessException(constructor.toString());
                    }
                } else {
                    lookup.findConstructor(owner, type);
                }
                return;
            default:
                throw new IllegalArgumentException("Unknown reference kind " + kind);
        }
    }

    /**
     * The type of a method descriptor, or of a field descriptor as the return type of a method without parameters. It
     * resolves the classes with {@link Class#forName(String, boolean, ClassLoader)}, because Paper rewrites
     * {@link MethodType#fromMethodDescriptorString} in a plugin it remaps to take Spigot names, while the class file
     * of the remapped plugin has Mojang names, and some Mojang names are Spigot names of other classes.
     */
    private static MethodType methodType(String descriptor,
                                         ClassLoader loader) throws ClassNotFoundException {
        List<Class<?>> parameters = new ArrayList<>();
        int index = descriptor.startsWith("(") ? 1 : 0;
        while (index < descriptor.length() && descriptor.charAt(index) != ')') {
            int end = typeEnd(descriptor, index);
            parameters.add(resolveType(descriptor.substring(index, end), loader));
            index = end;
        }
        String returnType = descriptor.startsWith("(") ? descriptor.substring(index + 1) : descriptor;
        return MethodType.methodType(resolveType(returnType, loader), parameters);
    }

    private static int typeEnd(String descriptor, int start) {
        int index = start;
        while (descriptor.charAt(index) == '[') {
            index++;
        }
        return descriptor.charAt(index) == 'L' ? descriptor.indexOf(';', index) + 1 : index + 1;
    }

    private static Class<?> resolveType(String descriptor, ClassLoader loader) throws ClassNotFoundException {
        switch (descriptor) {
            case "V":
                return void.class;
            case "Z":
                return boolean.class;
            case "B":
                return byte.class;
            case "C":
                return char.class;
            case "S":
                return short.class;
            case "I":
                return int.class;
            case "J":
                return long.class;
            case "F":
                return float.class;
            case "D":
                return double.class;
            default:
                if (descriptor.startsWith("[")) {
                    return Class.forName(descriptor.replace('/', '.'), false, loader);
                }
                return Class.forName(descriptor.substring(1, descriptor.length() - 1)
                                               .replace('/', '.'), false, loader);
        }
    }

    /**
     * The class an internal name or array descriptor refers to, or its element class for an array, or null for a
     * primitive element type.
     */
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

    /**
     * The constant pool of a class file, and the reference kinds that its instructions and method handle constants
     * use each field and method reference with.
     */
    private static final class ClassFile {
        private final int[] tags;
        private final int[] first;
        private final int[] second;
        private final String[] utf8;
        private final Map<Integer, BitSet> kinds = new HashMap<>();

        private ClassFile(int count) {
            tags = new int[count];
            first = new int[count];
            second = new int[count];
            utf8 = new String[count];
        }

        static ClassFile parse(byte[] bytes) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != 0xCAFEBABE) {
                throw new IOException("Not a class file");
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            ClassFile file = new ClassFile(in.readUnsignedShort());
            file.readConstantPool(in);

            in.readUnsignedShort();
            in.readUnsignedShort();
            in.readUnsignedShort();
            in.skipBytes(in.readUnsignedShort() * 2);
            file.readMembers(in);
            file.readMembers(in);
            return file;
        }

        private void readConstantPool(DataInputStream in) throws IOException {
            for (int i = 1; i < tags.length; i++) {
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
                        use(second[i], first[i]);
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
        }

        /**
         * Reads the fields or the methods, and the instructions in each method's Code attribute.
         */
        private void readMembers(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                in.skipBytes(6);
                int attributes = in.readUnsignedShort();
                for (int j = 0; j < attributes; j++) {
                    String name = utf8[in.readUnsignedShort()];
                    byte[] attribute = new byte[in.readInt()];
                    in.readFully(attribute);
                    if ("Code".equals(name)) {
                        DataInputStream code = new DataInputStream(new ByteArrayInputStream(attribute));
                        code.skipBytes(4);
                        byte[] instructions = new byte[code.readInt()];
                        code.readFully(instructions);
                        readInstructions(instructions);
                    }
                }
            }
        }

        private void readInstructions(byte[] code) throws IOException {
            int pc = 0;
            while (pc < code.length) {
                int opcode = code[pc] & 0xFF;
                int kind = referenceKind(opcode);
                if (kind != 0) {
                    use(((code[pc + 1] & 0xFF) << 8) | (code[pc + 2] & 0xFF), kind);
                }
                pc += instructionLength(code, pc, opcode);
            }
        }

        private void use(int reference, int kind) {
            kinds.computeIfAbsent(reference, x -> new BitSet())
                 .set(kind);
        }

        private static int referenceKind(int opcode) {
            switch (opcode) {
                case 0xB2:
                    return GET_STATIC;
                case 0xB3:
                    return PUT_STATIC;
                case 0xB4:
                    return GET_FIELD;
                case 0xB5:
                    return PUT_FIELD;
                case 0xB6:
                    return INVOKE_VIRTUAL;
                case 0xB7:
                    return INVOKE_SPECIAL;
                case 0xB8:
                    return INVOKE_STATIC;
                case 0xB9:
                    return INVOKE_INTERFACE;
                default:
                    return 0;
            }
        }

        private static int instructionLength(byte[] code, int pc, int opcode) throws IOException {
            switch (opcode) {
                case 0x10: // bipush
                case 0x12: // ldc
                case 0xA9: // ret
                case 0xBC: // newarray
                    return 2;
                case 0x11: // sipush
                case 0x13: // ldc_w
                case 0x14: // ldc2_w
                case 0x84: // iinc
                case 0xB2:
                case 0xB3:
                case 0xB4:
                case 0xB5:
                case 0xB6:
                case 0xB7:
                case 0xB8:
                case 0xBB: // new
                case 0xBD: // anewarray
                case 0xC0: // checkcast
                case 0xC1: // instanceof
                case 0xC6: // ifnull
                case 0xC7: // ifnonnull
                    return 3;
                case 0xC5: // multianewarray
                    return 4;
                case 0xB9: // invokeinterface
                case 0xBA: // invokedynamic
                case 0xC8: // goto_w
                case 0xC9: // jsr_w
                    return 5;
                case 0xC4: // wide
                    return (code[pc + 1] & 0xFF) == 0x84 ? 6 : 4;
                case 0xAA: { // tableswitch
                    int base = pc + 1 + (3 - pc % 4);
                    int low = readInt(code, base + 4);
                    int high = readInt(code, base + 8);
                    return base + 12 + (high - low + 1) * 4 - pc;
                }
                case 0xAB: { // lookupswitch
                    int base = pc + 1 + (3 - pc % 4);
                    return base + 8 + readInt(code, base + 4) * 8 - pc;
                }
                default:
                    if (opcode >= 0x15 && opcode <= 0x19 || opcode >= 0x36 && opcode <= 0x3A) {
                        return 2; // loads and stores with a local variable index
                    }
                    if (opcode >= 0x99 && opcode <= 0xA8) {
                        return 3; // branches
                    }
                    if (opcode > 0xC9) {
                        throw new IOException("Unknown opcode " + opcode);
                    }
                    return 1;
            }
        }

        private static int readInt(byte[] code, int index) {
            return (code[index] & 0xFF) << 24 | (code[index + 1] & 0xFF) << 16 | (code[index + 2] & 0xFF) << 8
                    | (code[index + 3] & 0xFF);
        }
    }

    static final class NoSuchMemberException extends Exception {
        NoSuchMemberException(String member) {
            super(member);
        }
    }
}
