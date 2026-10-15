package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.minecraft.server.v1_16_R3.CommandDispatcher;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_16_R3.command.VanillaCommandWrapper;

public class NMSVanillaCommandWrapper_spigot_1_16_5 extends NMSVanillaCommandWrapper {
    public NMSVanillaCommandWrapper_spigot_1_16_5() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((CommandDispatcher) dispatcher.requireHandle(),
                                         (CommandNode<CommandListenerWrapper>) command);
    }
}
