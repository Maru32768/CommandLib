package dev.marulabs.commandlib.nms.spigot_1_20_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;

public class NMSArgumentScoreboardTeam_spigot_1_20_4 extends NMSArgumentScoreboardTeam {
    public NMSArgumentScoreboardTeam_spigot_1_20_4() {
        super(null, TeamArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return TeamArgument.team();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSScoreboardTeam parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return NMSScoreboardTeam.create(TeamArgument.getTeam(context, name));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
