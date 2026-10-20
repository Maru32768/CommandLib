package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPlayers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
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
