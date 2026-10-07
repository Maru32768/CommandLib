package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentProfile;
import net.minecraft.server.v1_16_R3.ArgumentProfile;

public class NMSArgumentProfile_spigot_1_16_5 extends NMSArgumentProfile {
    public NMSArgumentProfile_spigot_1_16_5() {
        super(null, "ArgumentProfile");
    }

    @Override
    public ArgumentType<?> argument() {
        return new ArgumentProfile();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException();
    }
}
