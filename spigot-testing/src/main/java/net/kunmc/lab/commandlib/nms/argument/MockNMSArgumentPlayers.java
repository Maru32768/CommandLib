package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPlayers;
import org.bukkit.entity.Player;

public class MockNMSArgumentPlayers extends NMSArgumentPlayers {
    public MockNMSArgumentPlayers() {
        super(null, "Mock");
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
