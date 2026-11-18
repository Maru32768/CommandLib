package dev.marulabs.cadence.internal.nms.typed.spigot_1_17;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentScoreboardTeam;
import dev.marulabs.cadence.internal.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.cadence.internal.nms.world.NMSScoreboardTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;

public class NMSArgumentScoreboardTeam_spigot_1_17 extends NMSArgumentScoreboardTeam {
    public NMSArgumentScoreboardTeam_spigot_1_17() {
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
