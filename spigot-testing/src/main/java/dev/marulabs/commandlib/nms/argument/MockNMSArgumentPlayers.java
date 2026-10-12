package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPlayers;
import org.bukkit.entity.Player;

import java.util.List;

public class MockNMSArgumentPlayers extends NMSArgumentPlayers {
    public MockNMSArgumentPlayers() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockEntitySelector.type(false, true);
    }

    @Override
    protected List<Player> parseImpl(CommandContext<?> ctx, String name) {
        return MockEntitySelector.selectManyPlayers(ctx.getArgument(name, String.class));
    }
}
