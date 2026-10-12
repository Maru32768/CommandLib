package dev.marulabs.commandlib.nms.paper_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentProfile;
import net.minecraft.commands.arguments.GameProfileArgument;

public class NMSArgumentProfile_paper_1_21_1 extends NMSArgumentProfile {
    public NMSArgumentProfile_paper_1_21_1() {
        super(null, GameProfileArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return GameProfileArgument.gameProfile();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException();
    }
}
