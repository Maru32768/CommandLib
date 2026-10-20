package dev.marulabs.commandlib.internal.nms.exception;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
