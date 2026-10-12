package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.server.v1_16_R3.ArgumentScoreboardTeam;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentScoreboardTeam_spigot_1_16_5 extends NMSArgumentScoreboardTeam {
    public NMSArgumentScoreboardTeam_spigot_1_16_5() {
        super(null, ArgumentScoreboardTeam.class);
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
