package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TimeArgument;

public class NMSArgumentTime_spigot_1_18_2 extends NMSArgumentTime {
    public NMSArgumentTime_spigot_1_18_2() {
        super(null, TimeArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return TimeArgument.time();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Integer parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return context.getArgument(name, Integer.class);
    }

    @Override
    public ArgumentType<?> argument(int minimum) {
        return TimeArgument.time();
    }
}
