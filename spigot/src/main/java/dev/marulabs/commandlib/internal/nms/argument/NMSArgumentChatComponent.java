package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla text component argument ({@code ComponentArgument}), converted to a Bungee component through JSON.
 */
@ApiStatus.Internal
public abstract class NMSArgumentChatComponent extends NMSArgument<BaseComponent> {
    public static NMSArgumentChatComponent create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChatComponent.class))
                             .newInstance();
    }

    public NMSArgumentChatComponent(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
