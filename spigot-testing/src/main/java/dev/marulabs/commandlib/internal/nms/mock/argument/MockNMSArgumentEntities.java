package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentEntities;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class MockNMSArgumentEntities extends NMSArgumentEntities {
    public MockNMSArgumentEntities() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockEntitySelector.type(false, false);
    }

    @Override
    protected List<Entity> parseImpl(CommandContext<?> ctx, String name) {
        return MockEntitySelector.selectMany(ctx.getArgument(name, String.class));
    }
}
