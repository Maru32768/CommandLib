package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentEntity extends NMSArgumentEntity {
    public MockNMSArgumentEntity() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockEntitySelector.type(true, false);
    }

    @Override
    protected Entity parseImpl(CommandContext<?> ctx, String name) {
        return MockEntitySelector.selectOne(ctx.getArgument(name, String.class));
    }
}
