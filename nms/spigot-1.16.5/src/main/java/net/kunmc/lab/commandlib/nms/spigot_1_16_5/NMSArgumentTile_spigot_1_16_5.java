package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTile;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.minecraft.server.v1_16_R3.ArgumentTile;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentTile_spigot_1_16_5 extends NMSArgumentTile {
    public NMSArgumentTile_spigot_1_16_5() {
        super(null, "ArgumentTile");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentTile.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSArgumentTileLocation parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return NMSArgumentTileLocation.create(ArgumentTile.a(context, name));
    }
}
