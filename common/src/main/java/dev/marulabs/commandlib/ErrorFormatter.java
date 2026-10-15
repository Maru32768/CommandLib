package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

/**
 * Sends the messages of a failure that CommandLib reports, set with
 * {@link CommonCommand#errorFormatter(ErrorFormatter)}. What it sends is the failure's message, as
 * {@code ctx.sendFailure} inside a failing command is: a silent source does not get it and
 * {@code /execute store success} stores 0. A failure for which it sends nothing only returns 0.
 * <p>
 * For example, a prefix line before every error:
 *
 * <pre>{@code
 * errorFormatter((ctx, error) -> {
 *     ctx.sendFailure("[Homes] The command failed:");
 *     error.sendDefault();
 * });
 * }</pre>
 *
 * Errors of Brigadier's parsing, such as an unknown subcommand or an integer out of range, never reach it; the
 * platform reports them. Neither do the cooldown and confirmation messages and the messages of a preprocess, which
 * have their own hooks.
 */
@FunctionalInterface
public interface ErrorFormatter {
    /**
     * Sends the messages CommandLib sends by default, see {@link CommandError#sendDefault()}.
     */
    ErrorFormatter DEFAULT = (ctx, error) -> error.sendDefault();

    /**
     * Sends the default messages and, for an error with a {@link CommandError#usage()}, the {@link Caption#HELP_USAGE}
     * heading and the usage line of the argument branch that failed.
     */
    ErrorFormatter SYNTAX_ADVICE = (ctx, error) -> {
        error.sendDefault();
        CommandHelp.Usage usage = error.usage();
        if (usage != null) {
            ctx.sendFailure(Caption.HELP_USAGE);
            ctx.sendMessage(usage.line());
        }
    };

    /**
     * Sends the messages for {@code error} to {@code ctx}.
     *
     * @param ctx   the context of the failed command; its captions are those of the command
     * @param error the failure
     */
    void format(@NotNull CommonCommandContext<?, ?> ctx, @NotNull CommandError error);
}
