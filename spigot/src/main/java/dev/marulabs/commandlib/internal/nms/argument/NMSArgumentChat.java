package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla chat message argument ({@code MessageArgument}), with its selectors resolved, converted to a Bungee
 * component through JSON.
 */
@ApiStatus.Internal
public abstract class NMSArgumentChat extends NMSArgument<BaseComponent> {
    public static NMSArgumentChat create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChat.class))
                             .newInstance();
    }

    public NMSArgumentChat(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
