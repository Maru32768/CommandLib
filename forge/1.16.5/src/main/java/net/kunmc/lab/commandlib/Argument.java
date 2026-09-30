package net.kunmc.lab.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import net.kunmc.lab.commandlib.CommonArgument;

public abstract class Argument<T, SELF extends Argument<T, SELF>> extends CommonArgument<T, CommandContext, SELF> {
    public Argument(String name, ArgumentType<?> type) {
        super(name, type);
    }
}
