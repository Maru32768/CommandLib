package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What one copy of CommandLib reports about the running platform and the registrations it made, from
 * {@code CommandLib.diagnostics()}. A plugin or mod that shades and relocates CommandLib has its own copy, which reports
 * only its own registrations.
 *
 * <p>{@link #summary()} lists the platform and one line per registration, {@link #commands()} the command trees and
 * {@link #permissions()} the permission nodes. {@link #report()} joins the three.
 */
public final class CommandLibDiagnostics {
    private final List<String> platform;
    private final List<Registration> registrations;

    CommandLibDiagnostics(List<String> platform, List<Registration> registrations) {
        this.platform = List.copyOf(platform);
        this.registrations = List.copyOf(registrations);
    }

    /**
     * Facts about the platform as {@code key: value} lines, such as the server version and how commands reach the
     * dispatcher.
     */
    public List<String> platform() {
        return platform;
    }

    public List<Registration> registrations() {
        return registrations;
    }

    /**
     * The platform facts and one line per registration with its owner, prefix, path, labels and permission count.
     */
    public String summary() {
        StringBuilder out = new StringBuilder("CommandLib diagnostics\n");
        out.append("Platform:\n");
        platform.forEach(x -> out.append("  ")
                                 .append(x)
                                 .append('\n'));
        out.append("Registrations: ")
           .append(registrations.size())
           .append('\n');
        for (Registration registration : registrations) {
            out.append("  ")
               .append(registration.owner)
               .append(": prefix ")
               .append(registration.docs.permissionPrefix())
               .append(", ")
               .append(registration.path)
               .append('\n');
            out.append("    labels: ")
               .append(registration.labels.isEmpty() ? "(none yet)" : String.join(", ", registration.labels))
               .append('\n');
            out.append("    permissions: ")
               .append(registration.docs.permissionCount())
               .append('\n');
        }
        return out.toString();
    }

    /**
     * The command tree of every registration ({@link CommandDocs#tree()}).
     */
    public String commands() {
        StringBuilder out = new StringBuilder();
        for (Registration registration : registrations) {
            out.append("Commands of ")
               .append(registration.owner)
               .append(":\n");
            indent(out, registration.docs.tree());
        }
        return out.toString();
    }

    /**
     * Every permission node of every registration with its default and route.
     */
    public String permissions() {
        StringBuilder out = new StringBuilder();
        for (Registration registration : registrations) {
            out.append("Permissions of ")
               .append(registration.owner)
               .append(":\n");
            indent(out, registration.docs.permissionLines());
        }
        return out.toString();
    }

    /**
     * {@link #summary()}, {@link #commands()} and {@link #permissions()}.
     */
    public String report() {
        return summary() + commands() + permissions();
    }

    @Override
    public String toString() {
        return report();
    }

    /**
     * Sets up each platform's {@code CommandLibDiagnosticsCommand}: {@code root} sends {@link #summary()}, and the
     * children it gets from {@code newCommand}, "commands" and "permissions", send {@link #commands()} and
     * {@link #permissions()}, of what {@code diagnostics} returns when they execute.
     */
    static <C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> void configureCommand(T root,
                                                                                                       Function<String, T> newCommand,
                                                                                                       Supplier<CommandLibDiagnostics> diagnostics) {
        root.description("Show the platform and the commands CommandLib registered");
        root.execute(ctx -> send(ctx,
                                 diagnostics.get()
                                            .summary()));
        T commands = newCommand.apply("commands");
        commands.description("Show the registered command trees");
        commands.execute(ctx -> send(ctx,
                                     diagnostics.get()
                                                .commands()));
        T permissions = newCommand.apply("permissions");
        permissions.description("Show the registered permission nodes and their defaults");
        permissions.execute(ctx -> send(ctx,
                                        diagnostics.get()
                                                   .permissions()));
        root.addChildren(commands, permissions);
    }

    private static void send(CommonCommandContext<?, ?> ctx, String report) {
        if (report.isEmpty()) {
            ctx.sendMessage("No registrations.");
            return;
        }
        report.lines()
              .forEach(ctx::sendMessage);
    }

    private static void indent(StringBuilder out, String lines) {
        if (lines.isBlank()) {
            out.append("  (none)\n");
            return;
        }
        lines.lines()
             .filter(x -> !x.isEmpty())
             .forEach(x -> out.append("  ")
                              .append(x)
                              .append('\n'));
    }

    /**
     * One call of {@code CommandLib.register} (or {@code bootstrap}) that is still registered.
     */
    public static final class Registration {
        private final String owner;
        private final String path;
        private final List<String> labels;
        private final CommandDocs docs;

        Registration(@NotNull String owner,
                     @NotNull String path,
                     @NotNull List<String> labels,
                     @NotNull CommandDocs docs) {
            this.owner = Objects.requireNonNull(owner);
            this.path = Objects.requireNonNull(path);
            this.labels = List.copyOf(labels);
            this.docs = Objects.requireNonNull(docs);
        }

        /**
         * The plugin or mod that registered the commands, or the permission prefix where the platform does not tell.
         */
        public String owner() {
            return owner;
        }

        /**
         * How the commands reach the server's dispatcher, such as {@code Paper lifecycle COMMANDS handler}.
         */
        public String path() {
            return path;
        }

        /**
         * The labels the commands have in the dispatcher, with namespaced ones, empty until they are added.
         */
        public List<String> labels() {
            return labels;
        }

        /**
         * The documentation of the registered commands, with the registration's permission prefix.
         */
        public CommandDocs docs() {
            return docs;
        }
    }
}
