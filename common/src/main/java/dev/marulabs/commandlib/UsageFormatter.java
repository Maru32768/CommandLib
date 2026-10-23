package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

/**
 * Formats one usage line, set with {@link CommonCommand#usageFormatter(UsageFormatter)}. The help lists the lines it
 * returns, and an error formatter can show the line of the argument branch that failed, see
 * {@link CommandError#usage()}.
 * <p>
 * For example, plain lines with the description after a dash:
 *
 * <pre>{@code
 * usageFormatter((ctx, usage) -> usage.description().isEmpty() ? usage.plainText()
 *         : usage.plainText() + " - " + usage.description());
 * }</pre>
 */
@FunctionalInterface
public interface UsageFormatter {
    /**
     * CommandLib's line: the usage in colors, indented by two spaces, and the description after a colon, such as
     * {@code   /scan [options] <target>: Scan the target}.
     */
    UsageFormatter DEFAULT = new DefaultUsageFormatter();

    /**
     * Returns the line for {@code usage}, a text that may hold {@code §} color codes.
     *
     * @param ctx   the context of the help or the error; its captions are those of the command
     * @param usage the usage
     */
    @NotNull
    String format(@NotNull CommonCommandContext<?, ?> ctx, @NotNull CommandHelp.Usage usage);
}
