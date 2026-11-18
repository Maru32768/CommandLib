package dev.marulabs.cadence;

import org.jetbrains.annotations.NotNull;

/**
 * An opt-in command that shows {@link Cadence#diagnostics()} to server admins. Cadence never registers it; a mod
 * registers it like its other commands:
 *
 * <pre>{@code
 * Cadence.register("mymod.command", new SpawnCommand(), new CadenceDiagnosticsCommand("mymod-diagnostics"));
 * }</pre>
 *
 * <p>{@code /<name>} shows the platform and the registrations, {@code /<name> commands} the command trees and
 * {@code /<name> permissions} the permission nodes with their defaults. Its own permission nodes are generated like
 * those of any command and default to {@link DefaultPermission#OP}.
 */
public final class CadenceDiagnosticsCommand extends Command {
    public CadenceDiagnosticsCommand(@NotNull String name) {
        super(name);
        CadenceDiagnostics.configureCommand(this, Command::new, Cadence::diagnostics);
    }
}
