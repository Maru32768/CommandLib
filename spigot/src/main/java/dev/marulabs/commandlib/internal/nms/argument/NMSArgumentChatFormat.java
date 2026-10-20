package dev.marulabs.commandlib.internal.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@ApiStatus.Internal
public abstract class NMSArgumentChatFormat extends NMSArgument<ChatColor> {
    // 26.2 renamed ColorArgument; the 26.x module runs on both names.
    private static final List<String> COLOR_ARGUMENT_CLASSES = List.of("net.minecraft.commands.arguments.TeamColorArgument",
                                                                       "net.minecraft.commands.arguments.ColorArgument");

    public static NMSArgumentChatFormat create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChatFormat.class))
                             .newInstance();
    }

    public NMSArgumentChatFormat(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * The color argument type of a server with Mojang names: {@code TeamColorArgument} from 26.2, which renamed
     * {@code ColorArgument}, so a module for both releases cannot name it.
     *
     * @throws IllegalStateException if the server has neither
     */
    protected static Class<?> colorArgumentClass() {
        for (String name : COLOR_ARGUMENT_CLASSES) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                // Try the name of the other releases.
            }
        }
        throw new IllegalStateException("The server has none of " + COLOR_ARGUMENT_CLASSES);
    }

    /**
     * A new instance of {@link #colorArgumentClass()}, made by its factory method ({@code teamColor()} or
     * {@code color()}).
     */
    protected static ArgumentType<?> colorArgument() {
        Class<?> type = colorArgumentClass();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                    && method.getReturnType() == type) {
                try {
                    return (ArgumentType<?>) method.invoke(null);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(type + " has no factory method");
    }

    /**
     * Converts a value of the color argument, a {@code ChatFormatting} or {@code TeamColor}, whose constants are named
     * as those of {@link ChatColor}.
     */
    protected static ChatColor chatColor(Enum<?> color) {
        return ChatColor.valueOf(color.name());
    }
}
