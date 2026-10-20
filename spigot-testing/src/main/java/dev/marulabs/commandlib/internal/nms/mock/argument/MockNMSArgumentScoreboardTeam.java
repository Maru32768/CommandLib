package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardTeam;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSScoreboardTeam;
import dev.marulabs.commandlib.internal.nms.world.NMSScoreboardTeam;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentScoreboardTeam extends NMSArgumentScoreboardTeam {
    public MockNMSArgumentScoreboardTeam() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return StringArgumentType.word();
    }

    @Override
    protected NMSScoreboardTeam parseImpl(CommandContext<?> ctx, String name) {
        String teamName = StringArgumentType.getString(ctx, name);
        if (Bukkit.getScoreboardManager()
                  .getMainScoreboard()
                  .getTeam(teamName) == null) {
            throw MockArgumentTypes.resolveError("Unknown team '" + teamName + "'");
        }
        return new MockNMSScoreboardTeam(teamName);
    }
}
