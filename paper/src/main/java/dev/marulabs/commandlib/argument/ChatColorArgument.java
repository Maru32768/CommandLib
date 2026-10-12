package dev.marulabs.commandlib.argument;

import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public class ChatColorArgument extends EnumArgument<ChatColor> {
    public ChatColorArgument(String name) {
        super(name, ChatColor.class);
    }
}
