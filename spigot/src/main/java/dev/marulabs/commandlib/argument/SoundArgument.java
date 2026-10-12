package dev.marulabs.commandlib.argument;

import org.bukkit.Sound;

public class SoundArgument extends EnumArgument<Sound> {
    public SoundArgument(String name) {
        super(name, Sound.class);
    }
}
