package dev.marulabs.commandlib.internal.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.ApiStatus;

/**
 * Vanilla argument types that Paper's {@code ArgumentTypes} does not offer. Paper's command API accepts an argument
 * type the server's argument registry knows, and these have no API, so they are created by their Mojang names, which
 * Paper runs with from 1.20.5. paper-testing replaces this class with test doubles.
 */
@ApiStatus.Internal
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
     * The vanilla {@code color} argument: {@code TeamColorArgument} from 26.2, which renamed {@code ColorArgument}. Its
     * parsed value is an enum whose constant names are the color names in upper case: a {@code TeamColor}, or up to
     * 26.1 a {@code ChatFormatting}, which also has {@code RESET}.
     */
    public static ArgumentType<?> color() {
        try {
            Class.forName("net.minecraft.commands.arguments.TeamColorArgument");
        } catch (ClassNotFoundException e) {
            return create("net.minecraft.commands.arguments.ColorArgument", "color");
        }
        return create("net.minecraft.commands.arguments.TeamColorArgument", "teamColor");
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
