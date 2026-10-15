package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNBTTag;

public class MockNMSArgumentNBTTag extends NMSArgumentNBTTag {
    public MockNMSArgumentNBTTag() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // Finds the end of the compound but does not check its contents, and returns it as written.
        return reader -> {
            if (!reader.canRead() || reader.peek() != '{') {
                throw MockArgumentTypes.syntaxError("Expected '{'", reader);
            }
            return MockArgumentTypes.readValue(reader);
        };
    }

    @Override
    protected String parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}
