package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.server.v1_16_R3.ArgumentScoreboardTeam;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentScoreboardTeam_spigot_1_16_5 extends NMSArgumentScoreboardTeam {
    public NMSArgumentScoreboardTeam_spigot_1_16_5() {
        super(null, "ArgumentScoreboardTeam");
    }

    @Override
    public ArgumentType<?> argument() {
        return new ArgumentScoreboardTeam();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSScoreboardTeam parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return NMSScoreboardTeam.create(ArgumentScoreboardTeam.a(context, name));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
