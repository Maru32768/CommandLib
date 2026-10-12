package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentDimension;
import org.bukkit.World;

public class MockNMSArgumentDimension extends NMSArgumentDimension {
    public MockNMSArgumentDimension() {
        super(null, Object.class);
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
