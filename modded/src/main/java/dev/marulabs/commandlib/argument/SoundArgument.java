package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.core.Registry;*/
import net.minecraft.sounds.SoundEvent;

/**
 * A sound event of the server's registry by its key, such as {@code minecraft:entity.pig.ambient}. Input without a
 * namespace is in {@code minecraft}, and the registry's keys are suggested. An unknown key fails when the command runs.
 */
public class SoundArgument extends Argument<SoundEvent, SoundArgument> {
    public SoundArgument(String name) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        //? if >=1.19.3 {
        addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registries.SOUND_EVENT));
        //?} else
        /*addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registry.SOUND_EVENT_REGISTRY));*/
    }

    @Override
    public SoundEvent cast(Object parsedArgument) {
        return (SoundEvent) parsedArgument;
    }

    @Override
    protected SoundEvent parseImpl(CommandContext ctx) throws ArgumentParseException {
        //? if >=1.19.3 {
        return RegistryEntries.get(ctx, name(), Registries.SOUND_EVENT, Caption.UNKNOWN_SOUND);
        //?} else
        /*return RegistryEntries.get(ctx, name(), Registry.SOUND_EVENT_REGISTRY, Caption.UNKNOWN_SOUND);*/
    }
}
