package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentVec3D;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.server.v1_16_R3.ArgumentVec3;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentVec3D_spigot_1_16_5 extends NMSArgumentVec3D {
    public NMSArgumentVec3D_spigot_1_16_5() {
        super(null, "ArgumentVec3");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentVec3.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSVec3D parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return NMSVec3D.create(ArgumentVec3.a(context, name));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
