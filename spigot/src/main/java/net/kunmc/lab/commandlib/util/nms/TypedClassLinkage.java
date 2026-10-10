package net.kunmc.lab.commandlib.util.nms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 *     <li>It links each class without initializing it. HotSpot verifies a class when it links it, which rejects a
 *     server class that is no longer a subclass of the class the bytecode assigns it to. The verifier treats
 *     interface types as {@code Object}, though, and a JVM with verification turned off checks no supertypes, so a
 *     server class that dropped an interface still fails at command time.</li>
 *     <li>It reads the instructions, and resolves each class they use and each field and method reference through a
 *     {@link MethodHandles.Lookup} of the class with the instruction's kind: instance or static, virtual, special or
 *     interface. A lookup resolves only that member, checks access as the bytecode would, and initializes no class.
 *     Class references that only attributes such as {@code InnerClasses} name are never resolved by the JVM, so
 *     they are skipped.</li>
 * </ul>
 */
final class TypedClassLinkage {
    /** Why each checked class does not link, or an empty string when it links. */
    private static final Map<String, String> FAILURES = new ConcurrentHashMap<>();
    /*
     * Servers that remap a plugin from Spigot names also rewrite its calls to these Lookup methods, to map the member
     * name as a Spigot name. Whether the class files read here need that depends on the server: Paper remaps the
     * plugin jar itself, so its class files already have the server's names, and a Mojang name can be the Spigot name
     * of another member; Mohist remaps the classes as it defines them, so the class files keep the Spigot names. A
     * member is therefore resolved through these method handles, which no server rewrites, and when that fails,
     * through a plain call that the server may rewrite.
     */
    private static final MethodHandle FIND_GETTER = lookupMethod("findGetter", Class.class, String.class, Class.class);
    private static final MethodHandle FIND_STATIC_GETTER = lookupMethod("findStaticGetter",
                                                                        Class.class,
                                                                        String.class,
                                                                        Class.class);
    private static final MethodHandle FIND_VIRTUAL = lookupMethod("findVirtual",
                                                                  Class.class,
                                                                  String.class,
                                                                  MethodType.class);
    private static final MethodHandle FIND_STATIC = lookupMethod("findStatic",
                                                                 Class.class,
                                                                 String.class,
                                                                 MethodType.class);
    private static final MethodHandle FIND_SPECIAL = lookupMethod("findSpecial",
                                                                  Class.class,
                                                                  String.class,
                                                                  MethodType.class,
                                                                  Class.class);
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

    private static MethodHandle lookupMethod(String name, Class<?>... parameterTypes) {
        try {
            return MethodHandles.publicLookup()
                                .unreflect(MethodHandles.Lookup.class.getMethod(name, parameterTypes));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Resolves a member through the method handle of a Lookup method, and when that fails, through the plain call,
     * which a server that remaps the plugin may rewrite.
     */
    private static MethodHandle find(Finder plainCall,
                                     MethodHandle lookupMethod,
                                     Object... arguments) throws ReflectiveOperationException {
        try {
            return (MethodHandle) lookupMethod.invokeWithArguments(arguments);
        } catch (ReflectiveOperationException e) {
            try {
                return plainCall.find();
            } catch (ReflectiveOperationException rewritten) {
                e.addSuppressed(rewritten);
                throw e;
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    private interface Finder {
        MethodHandle find() throws ReflectiveOperationException;
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
                List<String> referenced = new ArrayList<>(checkReferences(bytes, clazz));
                // Inherited methods run the code of the supertypes, which the instructions may not name.
                if (clazz.getSuperclass() != null) {
                    referenced.add(clazz.getSuperclass()
                                        .getName());
                }
                for (Class<?> anInterface : clazz.getInterfaces()) {
                    referenced.add(anInterface.getName());
                }
                for (String name : referenced) {
                    if (name.startsWith(packagePrefix) && name.indexOf('.', packagePrefix.length()) < 0) {
                        pending.add(name);
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
     * of the classes whose references it uses.
     */
    static List<String> checkReferences(byte[] classFile,
                                        Class<?> lookupClass) throws IOException, ReflectiveOperationException, NoSuchMemberException {
        ClassFile file = ClassFile.parse(classFile);
        ClassLoader loader = lookupClass.getClassLoader();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(lookupClass, MethodHandles.lookup());

        List<String> classNames = new ArrayList<>();
        for (int i = 1; i < file.tags.length; i++) {
            if (file.tags[i] == 7) {
                // The JVM resolves, and checks access to, only the classes that instructions and exception handlers
                // use. Attributes such as InnerClasses may name classes the class cannot access.
                if (!file.usedClasses.get(i)) {
                    continue;
                }
                Class<?> clazz = resolveInternalName(file.utf8[file.first[i]], loader);
                if (clazz != null) {
                    lookup.accessClass(clazz);
                    classNames.add(clazz.getName());
                }
            } else if (file.tags[i] == 9 || file.tags[i] == 10 || file.tags[i] == 11) {
                // A reference that no instruction or method handle uses never resolves; javac does not emit one.
                BitSet kinds = file.kinds.get(i);
                String ownerName = file.utf8[file.first[file.first[i]]];
                // An array type, as in a clone call on an array, only has the members of Object.
                if (kinds == null || ownerName.startsWith("[")) {
                    continue;
                }
                Class<?> owner = resolveInternalName(ownerName, loader);
                classNames.add(owner.getName());
                int nameAndType = file.second[i];
                String name = file.utf8[file.first[nameAndType]];
                String descriptor = file.utf8[file.second[nameAndType]];
                // Method resolution and interface method resolution fail when the owner is of the other kind.
                if (file.tags[i] != 9 && owner.isInterface() != (file.tags[i] == 11)) {
                    throw new NoSuchMemberException(owner.getName() + "." + name + descriptor);
                }
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
                getter(lookup, owner, name, type.returnType());
                return;
            case GET_STATIC:
                staticGetter(lookup, owner, name, type.returnType());
                return;
            case PUT_FIELD:
                checkWritable(lookup, getter(lookup, owner, name, type.returnType()));
                return;
            case PUT_STATIC:
                checkWritable(lookup, staticGetter(lookup, owner, name, type.returnType()));
                return;
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
                find(() -> lookup.findVirtual(owner, name, type), FIND_VIRTUAL, lookup, owner, name, type);
                return;
            case INVOKE_STATIC:
                find(() -> lookup.findStatic(owner, name, type), FIND_STATIC, lookup, owner, name, type);
                return;
            case INVOKE_SPECIAL:
            case NEW_INVOKE_SPECIAL:
                if (name.equals("<init>")) {
                    resolveConstructor(lookup, owner, type);
                } else {
                    Class<?> caller = lookup.lookupClass();
                    find(() -> lookup.findSpecial(owner, name, type, caller),
                         FIND_SPECIAL,
                         lookup,
                         owner,
                         name,
                         type,
                         caller);
                }
                return;
            default:
                throw new IllegalArgumentException("Unknown reference kind " + kind);
        }
    }

    private static MethodHandle getter(MethodHandles.Lookup lookup,
                                       Class<?> owner,
                                       String name,
                                       Class<?> type) throws ReflectiveOperationException {
        return find(() -> lookup.findGetter(owner, name, type), FIND_GETTER, lookup, owner, name, type);
    }

    private static MethodHandle staticGetter(MethodHandles.Lookup lookup,
                                             Class<?> owner,
                                             String name,
                                             Class<?> type) throws ReflectiveOperationException {
        return find(() -> lookup.findStaticGetter(owner, name, type), FIND_STATIC_GETTER, lookup, owner, name, type);
    }

    /**
     * Rejects a write to a final field of another class, which the JVM rejects. A setter lookup would also reject the
     * final fields that the class writes in its own constructor.
     */
    private static void checkWritable(MethodHandles.Lookup lookup,
                                      MethodHandle getter) throws IllegalAccessException {
        MethodHandleInfo field = lookup.revealDirect(getter);
        if (Modifier.isFinal(field.getModifiers()) && field.getDeclaringClass() != lookup.lookupClass()) {
            throw new IllegalAccessException("write to final field " + field);
        }
    }

    /**
     * Resolves a constructor for {@code new} or a {@code this(...)} or {@code super(...)} call. A lookup checks a
     * constructor as {@code new} uses it, which a protected constructor of the superclass in another package fails,
     * although {@code super(...)} may call it. Only that case reads the constructor's modifiers, through a lookup in
     * the superclass, so no other constructor is resolved.
     */
    private static void resolveConstructor(MethodHandles.Lookup lookup,
                                           Class<?> owner,
                                           MethodType type) throws ReflectiveOperationException {
        try {
            lookup.findConstructor(owner, type);
        } catch (IllegalAccessException e) {
            if (owner != lookup.lookupClass()
                               .getSuperclass()) {
                throw e;
            }
            MethodHandles.Lookup ownerLookup;
            try {
                ownerLookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
            } catch (IllegalAccessException notOpen) {
                throw e;
            }
            MethodHandleInfo constructor = ownerLookup.revealDirect(ownerLookup.findConstructor(owner, type));
            if (!Modifier.isProtected(constructor.getModifiers())) {
                throw e;
            }
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
     * The constant pool of a class file, the reference kinds that its instructions and method handle constants use
     * each field and method reference with, and the constants that instructions and exception handlers use as classes.
     */
    private static final class ClassFile {
        private final int[] tags;
        private final int[] first;
        private final int[] second;
        private final String[] utf8;
        private final Map<Integer, BitSet> kinds = new HashMap<>();
        /** Indexes of constants that instructions and exception handlers use; only class constants matter. */
        private final BitSet usedClasses = new BitSet();

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
         * Reads the fields or the methods, and the instructions and exception handlers in each method's Code
         * attribute.
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
                        int handlers = code.readUnsignedShort();
                        for (int k = 0; k < handlers; k++) {
                            code.skipBytes(6);
                            // The catch type, or 0 for a handler of every exception.
                            usedClasses.set(code.readUnsignedShort());
                        }
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
                switch (opcode) {
                    case 0x12: // ldc
                        usedClasses.set(code[pc + 1] & 0xFF);
                        break;
                    case 0x13: // ldc_w
                    case 0xBB: // new
                    case 0xBD: // anewarray
                    case 0xC0: // checkcast
                    case 0xC1: // instanceof
                    case 0xC5: // multianewarray
                        usedClasses.set(((code[pc + 1] & 0xFF) << 8) | (code[pc + 2] & 0xFF));
                        break;
                    default:
                        break;
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
