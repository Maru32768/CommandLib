package dev.marulabs.commandlib.util.nms.exception;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class UncheckedCommandSyntaxException extends RuntimeException {
    private final CommandSyntaxException commandSyntaxException;

    public UncheckedCommandSyntaxException(CommandSyntaxException commandSyntaxException) {
        super(commandSyntaxException);
        this.commandSyntaxException = commandSyntaxException;
    }

    public CommandSyntaxException unwrap() {
        return commandSyntaxException;
    }
}
