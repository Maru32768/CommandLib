package dev.marulabs.commandlib.nms.spigot_1_21_5;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_21_R4.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_21_5 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_21_5() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.getHandle(), (CommandNode<CommandSourceStack>) command);
    }
}
