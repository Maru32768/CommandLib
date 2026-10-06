package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentProfile;

public class MockNMSArgumentProfile extends NMSArgumentProfile {
    public MockNMSArgumentProfile() {
        super(null, "Mock");
    }

    @Override
    public ArgumentType<?> argument() {
        // A game profile argument reads a single player name or selector token, like on a server.
        return MockArgumentTypes.token();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException("MockNMSArgumentProfile.parseImpl is not called directly; " + "callers use ctx.getInput() instead.");
    }
}
