package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.tree.CommandNode;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.server.v1_16_R3.CommandDispatcher;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_16_R3.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_16_5 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_16_5() {
        super(null, "command.VanillaCommandWrapper");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((CommandDispatcher) dispatcher.getHandle(),
                                         (CommandNode<CommandListenerWrapper>) command);
    }
}
