package net.kunmc.lab.commandlib.nms.spigot_1_19;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentVec3D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

public class NMSArgumentVec3D_spigot_1_19 extends NMSArgumentVec3D {
    public NMSArgumentVec3D_spigot_1_19() {
        super(null, "commands.arguments.coordinates.ArgumentVec3");
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
