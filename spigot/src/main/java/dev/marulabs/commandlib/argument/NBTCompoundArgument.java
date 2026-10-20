package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNBTTag;

/**
 * A vanilla NBT compound in SNBT, such as {@code {CustomName:"Bob",Health:20.0f}}, checked by the server's parser and
 * returned as SNBT. Bukkit has no NBT type, so the value is the string that NBT libraries and commands read; the
 * server rewrites it in its own form (for example {@code {a:1b}} for {@code {a:true}}).
 */
public class NBTCompoundArgument extends Argument<String, NBTCompoundArgument> {
    public NBTCompoundArgument(String name) {
        super(name,
              NMSArgumentNBTTag.create()
                               .argument());
    }

    @Override
    public String cast(Object parsedArgument) {
        return (String) parsedArgument;
    }

    @Override
    protected String parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentNBTTag.create()
                                .parse(ctx.getHandle(), name());
    }
}
