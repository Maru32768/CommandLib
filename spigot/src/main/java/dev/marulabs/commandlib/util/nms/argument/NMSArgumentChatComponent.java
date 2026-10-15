package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla text component argument ({@code ComponentArgument}), converted to a Bungee component through JSON.
 */
public abstract class NMSArgumentChatComponent extends NMSArgument<BaseComponent> {
    public static NMSArgumentChatComponent create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChatComponent.class))
                             .newInstance();
    }

    public NMSArgumentChatComponent(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
