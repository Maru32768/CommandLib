package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentProfile;
import net.minecraft.commands.arguments.GameProfileArgument;

public class NMSArgumentProfile_spigot_1_18_2 extends NMSArgumentProfile {
    public NMSArgumentProfile_spigot_1_18_2() {
        super(null, "commands.arguments.ArgumentProfile");
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
