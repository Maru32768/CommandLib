package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentProfile;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentProfile extends NMSArgumentProfile {
    public MockNMSArgumentProfile() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // A game profile argument reads a single player name or selector token, like on a server.
        return MockArgumentTypes.token();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException("MockNMSArgumentProfile.parseImpl is not called directly; "
                + "callers use ctx.getInput() instead.");
    }
}
