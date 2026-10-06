package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.bukkit.Bukkit;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.nms.world.MockNMSScoreboardTeam;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;

public class MockNMSArgumentScoreboardTeam extends NMSArgumentScoreboardTeam {
    public MockNMSArgumentScoreboardTeam() {
        super(null, "Mock");
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
