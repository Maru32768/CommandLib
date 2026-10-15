package dev.marulabs.commandlib.nms.spigot_1_19_3;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_19_R2.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_19_3 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_19_3() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.requireHandle(), (CommandNode<CommandSourceStack>) command);
    }
}
