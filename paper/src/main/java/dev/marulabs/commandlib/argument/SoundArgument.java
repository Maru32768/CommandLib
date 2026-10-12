package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import org.bukkit.Sound;

@SuppressWarnings("UnstableApiUsage")
public class SoundArgument extends Argument<Sound, SoundArgument> {
    public SoundArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.SOUND_EVENT));
    }

    @Override
    public Sound cast(Object parsedArgument) {
        return (Sound) parsedArgument;
    }

    @Override
    protected Sound parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Sound.class);
    }
}
