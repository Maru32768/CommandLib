package net.kunmc.lab.commandlib.util.nms;

import net.kunmc.lab.commandlib.util.nms.access.AccessTarget;
import net.kunmc.lab.commandlib.util.nms.hybrid.HybridClassLoader;
import net.kunmc.lab.commandlib.util.nms.unlinkable.UnlinkableTypedLookUp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypedClassLinkageTest {
    @Test
    void class_whose_references_resolve_is_linkable() {
        assertThat(TypedClassLinkage.isLinkable(Caller.class)).isTrue();
    }

    @Test
    void class_referring_to_missing_class_is_not_linkable() {
        assertThat(TypedClassLinkage.isLinkable(UnlinkableTypedLookUp.class)).isFalse();
    }

    @Test
    void references_resolve_in_unmodified_class_file() throws Exception {
        assertThat(TypedClassLinkage.checkReferences(classFile(Caller.class),
                                                     Caller.class)).contains(LinkedTarget.class.getName());
    }

    @Test
    void missing_method_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "linkedMethod", "missedMethod");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void missing_field_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "linkedField", "missedField");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void method_with_changed_return_type_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), ")Ljava/lang/String;", ")Ljava/lang/Object;");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void missing_class_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "LinkedTarget", "MissedTarget");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                ClassNotFoundException.class);
    }

    @Test
    void instance_field_that_became_static_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "linkedField", "staticField");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void static_method_that_became_an_instance_method_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "linkedMethod", "memberMethod");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void method_that_became_private_is_reported() throws Exception {
        byte[] bytes = replace(classFile(Caller.class), "publicMethod", "hiddenMethod");

        assertThatThrownBy(() -> TypedClassLinkage.checkReferences(bytes,
                                                                   Caller.class)).isInstanceOf(
                TypedClassLinkage.NoSuchMemberException.class);
    }

    @Test
    void members_of_unrelated_methods_are_not_resolved() throws Exception {
        // TargetWithBrokenMember declares a method whose signature names a class the loader lacks, which reflecting
        // over all its methods would fail on. BrokenOwnerCaller only uses another method of it.
        Class<?> caller = Class.forName("net.kunmc.lab.commandlib.util.nms.hybrid.BrokenOwnerCaller",
                                        false,
                                        new HybridClassLoader());

        assertThat(TypedClassLinkage.isLinkable(caller)).isTrue();
    }

    private static byte[] classFile(Class<?> clazz) throws IOException {
        try (InputStream in = clazz.getResourceAsStream(clazz.getName()
                                                             .substring(clazz.getName()
                                                                             .lastIndexOf('.') + 1) + ".class")) {
            return in.readAllBytes();
        }
    }

    /**
     * Replaces every occurrence of a string of the same length, so the constant pool keeps its layout.
     */
    private static byte[] replace(byte[] bytes, String target, String replacement) {
        byte[] from = target.getBytes(StandardCharsets.UTF_8);
        byte[] to = replacement.getBytes(StandardCharsets.UTF_8);
        assertThat(to).hasSameSizeAs(from);
        byte[] result = bytes.clone();
        int replaced = 0;
        for (int i = 0; i + from.length <= result.length; i++) {
            boolean matches = true;
            for (int j = 0; j < from.length && matches; j++) {
                matches = result[i + j] == from[j];
            }
            if (matches) {
                System.arraycopy(to, 0, result, i, to.length);
                replaced++;
            }
        }
        assertThat(replaced).as("occurrences of %s", target)
                            .isPositive();
        return result;
    }

    static class LinkedTarget {
        static int staticField;
        int linkedField;

        static String linkedMethod(int value) {
            return String.valueOf(value);
        }

        String memberMethod(int value) {
            return String.valueOf(value);
        }
    }

    static class Caller {
        Object run(Runnable callback) {
            LinkedTarget target = new LinkedTarget();
            target.linkedField = 1;
            callback.run();
            Runnable lambda = () -> System.out.println(target.linkedField);
            lambda.run();
            AccessTarget.publicMethod();
            int[] copy = new int[]{target.linkedField}.clone();
            return LinkedTarget.linkedMethod(copy[0]);
        }
    }
}
