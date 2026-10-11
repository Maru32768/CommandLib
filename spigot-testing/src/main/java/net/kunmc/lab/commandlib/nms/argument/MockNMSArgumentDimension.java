package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentDimension;
import org.bukkit.World;

public class MockNMSArgumentDimension extends NMSArgumentDimension {
    public MockNMSArgumentDimension() {
        super(null, "Mock");
    }

    @Override
    public ArgumentType<?> argument() {
        return reader -> {
            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                reader.skip();
            }
            return reader.getString()
                         .substring(start, reader.getCursor());
        };
    }

    @Override
    protected World parseImpl(CommandContext<?> ctx, String name) {
        String key = ctx.getArgument(name, String.class);
        World world = CommandTester.getFakeWorld(key);
        if (world == null) {
            throw MockArgumentTypes.resolveError("Unknown dimension '" + key + "'");
        }
        return world;
    }
}
