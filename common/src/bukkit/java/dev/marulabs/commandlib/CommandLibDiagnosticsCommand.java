package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

/**
 * An opt-in command that shows {@link CommandLib#diagnostics()} to server admins. CommandLib never registers it; a
 * plugin registers it like its other commands:
 *
 * <pre>{@code
 * CommandLib.register(this, new SpawnCommand(), new CommandLibDiagnosticsCommand("myplugin-diagnostics"));
 * }</pre>
 *
 * <p>{@code /<name>} shows the platform and the registrations, {@code /<name> commands} the command trees and
 * {@code /<name> permissions} the permission nodes with their defaults. Its own permission nodes are generated like
 * those of any command and default to {@link DefaultPermission#OP}.
 */
public final class CommandLibDiagnosticsCommand extends Command {
    public CommandLibDiagnosticsCommand(@NotNull String name) {
        super(name);
        CommandLibDiagnostics.configureCommand(this, Command::new, CommandLib::diagnostics);
    }
}
