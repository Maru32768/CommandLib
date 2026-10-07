package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentVec3D;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

public class NMSArgumentVec3D_paper_1_20_6 extends NMSArgumentVec3D {
    public NMSArgumentVec3D_paper_1_20_6() {
        super(null, "commands.arguments.coordinates.Vec3Argument");
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
