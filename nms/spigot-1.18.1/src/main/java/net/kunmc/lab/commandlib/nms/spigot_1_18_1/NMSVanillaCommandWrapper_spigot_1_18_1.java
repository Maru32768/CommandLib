package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import com.mojang.brigadier.tree.CommandNode;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_18_R1.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_18_1 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_18_1() {
        super(null, "command.VanillaCommandWrapper");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.getHandle(), (CommandNode<CommandSourceStack>) command);
    }
}
