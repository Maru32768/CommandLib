package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPlayer;
import org.bukkit.entity.Player;

public class MockNMSArgumentPlayer extends NMSArgumentPlayer {
    public MockNMSArgumentPlayer() {
        super(null, "Mock");
    }

    @Override
    public ArgumentType<?> argument() {
        return MockEntitySelector.type(true, true);
    }

    @Override
    protected Player parseImpl(CommandContext<?> ctx, String name) {
        return MockEntitySelector.selectOnePlayer(ctx.getArgument(name, String.class));
    }
}
