package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEntity;
import org.bukkit.entity.Entity;

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
