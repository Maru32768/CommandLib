package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;

/**
 * A vanilla NBT compound in SNBT, such as {@code {CustomName:"Bob",Health:20.0f}}.
 */
public class NBTCompoundArgument extends Argument<CompoundTag, NBTCompoundArgument> {
    public NBTCompoundArgument(String name) {
        super(name, CompoundTagArgument.compoundTag());
    }

    @Override
    public CompoundTag cast(Object parsedArgument) {
        return (CompoundTag) parsedArgument;
    }

    @Override
    protected CompoundTag parseImpl(CommandContext ctx) throws ArgumentParseException {
        return CompoundTagArgument.getCompoundTag(ctx.getHandle(), name());
    }
}
