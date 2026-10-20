package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.brigadier.VanillaArgumentTypes;

/**
 * A vanilla NBT compound in SNBT, such as {@code {CustomName:"Bob",Health:20.0f}}, checked by the server's parser and
 * returned as SNBT. Paper has no NBT type or NBT argument, so this uses the vanilla {@code nbt_compound_tag} argument,
 * and the value is the string that NBT libraries and commands read; the server rewrites it in its own form (for
 * example {@code {a:1b}} for {@code {a:true}}).
 */
public class NBTCompoundArgument extends Argument<String, NBTCompoundArgument> {
    public NBTCompoundArgument(String name) {
        super(name, VanillaArgumentTypes.compoundTag());
    }

    @Override
    public String cast(Object parsedArgument) {
        return (String) parsedArgument;
    }

    @Override
    protected String parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Object.class)
                  .toString();
    }
}
