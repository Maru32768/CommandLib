package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

public class SoundArgument extends Argument<Sound, SoundArgument> {
    public SoundArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Registry.SOUNDS));
    }

    @Override
    public Sound cast(Object parsedArgument) {
        return (Sound) parsedArgument;
    }

    @Override
    protected Sound parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx, name(), Registry.SOUNDS, Caption.UNKNOWN_SOUND);
    }
}
