package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentPlayer extends NMSArgumentPlayer {
    public MockNMSArgumentPlayer() {
        super(null, Object.class);
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
