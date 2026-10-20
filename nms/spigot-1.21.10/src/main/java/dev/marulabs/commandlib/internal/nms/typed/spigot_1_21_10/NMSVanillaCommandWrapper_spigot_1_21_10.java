package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_10;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.internal.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_21_R6.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_21_10 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_21_10() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.requireHandle(), (CommandNode<CommandSourceStack>) command);
    }
}
