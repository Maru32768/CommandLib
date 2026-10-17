package dev.marulabs.commandlib.util.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;

/**
 * Vanilla argument types that Paper's {@code ArgumentTypes} does not offer. Paper's command API accepts an argument
 * type the server's argument registry knows, and these have no API, so they are created by their Mojang names, which
 * Paper runs with from 1.20.5. paper-testing replaces this class with test doubles.
 */
public final class VanillaArgumentTypes {
    private VanillaArgumentTypes() {
    }

    /**
     * The vanilla {@code nbt_compound_tag} argument. Its parsed value is a vanilla {@code CompoundTag}, whose
     * {@code toString()} is SNBT.
     */
    public static ArgumentType<?> compoundTag() {
        return create("net.minecraft.commands.arguments.CompoundTagArgument", "compoundTag");
    }

    /**
     * The vanilla {@code color} argument. Its parsed value is a vanilla {@code ChatFormatting}, an enum whose constant
     * names are the color names in upper case, and {@code RESET}.
     */
    public static ArgumentType<?> color() {
        return create("net.minecraft.commands.arguments.ColorArgument", "color");
    }

    private static ArgumentType<?> create(String className, String factory) {
        try {
            return (ArgumentType<?>) Class.forName(className)
                                          .getMethod(factory)
                                          .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the vanilla argument type " + className, e);
        }
    }
}
