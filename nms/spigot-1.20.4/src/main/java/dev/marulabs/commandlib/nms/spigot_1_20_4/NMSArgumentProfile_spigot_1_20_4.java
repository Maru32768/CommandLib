package dev.marulabs.commandlib.nms.spigot_1_20_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentProfile;
import net.minecraft.commands.arguments.GameProfileArgument;

public class NMSArgumentProfile_spigot_1_20_4 extends NMSArgumentProfile {
    public NMSArgumentProfile_spigot_1_20_4() {
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
