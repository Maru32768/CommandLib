package dev.marulabs.commandlib.nms.spigot_1_19_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentVec3D;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

public class NMSArgumentVec3D_spigot_1_19_2 extends NMSArgumentVec3D {
    public NMSArgumentVec3D_spigot_1_19_2() {
        super(null, Vec3Argument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return Vec3Argument.vec3();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSVec3D parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSVec3D.create(Vec3Argument.getVec3(context, name));
    }
}
