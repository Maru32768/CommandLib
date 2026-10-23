package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

/**
 * Sends the generated help of a command, set with {@link CommonCommand#helpFormatter(HelpFormatter)}. It runs where
 * the input ends at a command or argument without an executor, and for {@code /<command> help}.
 * <p>
 * For example, a help that lists only the usage lines:
 *
 * <pre>{@code
 * helpFormatter((ctx, help) -> help.usages()
 *                                  .forEach(usage -> ctx.sendMessage(usage.line())));
 * }</pre>
 */
@FunctionalInterface
public interface HelpFormatter {
    /**
     * CommandLib's help: a border, the description, the {@link Caption#HELP_USAGE} heading with the usage lines, the
     * options under {@link Caption#HELP_OPTIONS} and the examples under {@link Caption#HELP_EXAMPLES}.
     */
    HelpFormatter DEFAULT = new DefaultHelpFormatter();

    /**
     * Sends {@code help} to {@code ctx}.
     *
     * @param ctx  the context to send to; its captions are those of the command
     * @param help the command's help for the sender; each usage's {@link CommandHelp.Usage#line()} is already
     *             formatted by the command's {@link UsageFormatter}
     */
    void format(@NotNull CommonCommandContext<?, ?> ctx, @NotNull CommandHelp help);
}
