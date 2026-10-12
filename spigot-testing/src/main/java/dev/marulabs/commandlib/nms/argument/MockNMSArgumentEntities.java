package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEntities;
import org.bukkit.entity.Entity;

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
