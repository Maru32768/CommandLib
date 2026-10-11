package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentNamespacedKey;

public class MockNMSArgumentNamespacedKey extends NMSArgumentNamespacedKey {
    public MockNMSArgumentNamespacedKey() {
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
    protected String parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}
