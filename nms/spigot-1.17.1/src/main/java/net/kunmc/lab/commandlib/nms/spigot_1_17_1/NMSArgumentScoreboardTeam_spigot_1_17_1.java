package net.kunmc.lab.commandlib.nms.spigot_1_17_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;

public class NMSArgumentScoreboardTeam_spigot_1_17_1 extends NMSArgumentScoreboardTeam {
    public NMSArgumentScoreboardTeam_spigot_1_17_1() {
        super(null, "commands.arguments.ArgumentScoreboardTeam");
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
