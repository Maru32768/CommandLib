package dev.marulabs.commandlib.internal.nms.typed.spigot_1_17_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentVec3D;
import dev.marulabs.commandlib.internal.nms.world.NMSVec3D;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

public class NMSArgumentVec3D_spigot_1_17_1 extends NMSArgumentVec3D {
    public NMSArgumentVec3D_spigot_1_17_1() {
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

    @Override
    public ArgumentType<?> argument(boolean centerIntegers) {
        return Vec3Argument.vec3(centerIntegers);
    }
}
