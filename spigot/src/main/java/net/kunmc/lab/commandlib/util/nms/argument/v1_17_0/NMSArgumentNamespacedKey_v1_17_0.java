package net.kunmc.lab.commandlib.util.nms.argument.v1_17_0;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import net.kunmc.lab.commandlib.util.nms.exception.MethodNotFoundException;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class NMSArgumentNamespacedKey_v1_17_0 extends NMSArgumentNamespacedKey {
    public NMSArgumentNamespacedKey_v1_17_0() {
        super(null, "commands.arguments.ArgumentMinecraftKeyRegistered");
    }

    @Override
    public ArgumentType<?> argument() {
        return (ArgumentType<?>) invokeStaticMethod("a");
    }

    @Override
    protected String parseImpl(CommandContext<?> ctx, String name) {
        // Every getter of this class takes (CommandContext, String) and the obfuscated name of the MinecraftKey
        // getter moved from "f" (1.17) to "e" (1.18+), so it is selected by its return type instead.
        Method getter = Arrays.stream(clazz.getMethods())
                              .filter(x -> Modifier.isStatic(x.getModifiers()))
                              .filter(x -> x.getParameterCount() == 2 && x.getParameterTypes()[0] == CommandContext.class)
                              .filter(x -> x.getReturnType()
                                            .getSimpleName()
                                            .equals("MinecraftKey"))
                              .findFirst()
                              .orElseThrow(() -> new MethodNotFoundException(new String[]{"MinecraftKey getter of " + clazz.getName()}));
        try {
            return getter.invoke(null, ctx, name)
                         .toString();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof CommandSyntaxException) {
                throw new UncheckedCommandSyntaxException((CommandSyntaxException) e.getCause());
            }
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
