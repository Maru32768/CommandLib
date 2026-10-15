package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

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
        description("Show the platform and the commands CommandLib registered");
        execute(ctx -> send(ctx, CommandLibDiagnostics::summary));
        addChildren(section("commands", "Show the registered command trees", CommandLibDiagnostics::commands),
                    section("permissions",
                            "Show the registered permission nodes and their defaults",
                            CommandLibDiagnostics::permissions));
    }

    private static Command section(String name, String description, Function<CommandLibDiagnostics, String> text) {
        Command command = new Command(name);
        command.description(description);
        command.execute(ctx -> send(ctx, text));
        return command;
    }

    private static void send(CommandContext ctx, Function<CommandLibDiagnostics, String> text) {
        String report = text.apply(CommandLib.diagnostics());
        if (report.isEmpty()) {
            ctx.sendMessage("No registrations.");
            return;
        }
        report.lines()
              .forEach(ctx::sendMessage);
    }
}
