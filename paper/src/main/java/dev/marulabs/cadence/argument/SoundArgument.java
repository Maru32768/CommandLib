package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
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
