package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla chat message argument ({@code MessageArgument}), with its selectors resolved, converted to a Bungee
 * component through JSON.
 */
public abstract class NMSArgumentChat extends NMSArgument<BaseComponent> {
    public static NMSArgumentChat create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChat.class))
                             .newInstance();
    }

    public NMSArgumentChat(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
