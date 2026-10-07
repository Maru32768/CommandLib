package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentProfile;
import net.minecraft.commands.arguments.GameProfileArgument;

public class NMSArgumentProfile_paper_1_20_6 extends NMSArgumentProfile {
    public NMSArgumentProfile_paper_1_20_6() {
        super(null, "commands.arguments.GameProfileArgument");
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
