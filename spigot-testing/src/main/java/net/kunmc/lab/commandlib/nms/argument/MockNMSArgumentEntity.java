package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEntity;
import org.bukkit.entity.Entity;

public class MockNMSArgumentEntity extends NMSArgumentEntity {
    public MockNMSArgumentEntity() {
        super(null, "Mock");
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
