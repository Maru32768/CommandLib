package dev.marulabs.commandlib.internal.nms.typed.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPlayers;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import org.bukkit.entity.Player;

public class NMSArgumentPlayers_paper_1_20_6 extends NMSArgumentPlayers {
    public NMSArgumentPlayers_paper_1_20_6() {
        super(null, EntityArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return EntityArgument.players();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Player> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return EntityArgument.getPlayers(context, name).stream()
                                 .map(x -> ((Player) x.getBukkitEntity()))
                                 .collect(Collectors.toList());
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
