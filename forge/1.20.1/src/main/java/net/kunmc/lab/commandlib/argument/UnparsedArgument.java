package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;

/**
 * An argument that captures a single whitespace-delimited token as a raw string,
 * without any entity-selector or other special-character validation.
 *
 * <p>This is useful when the value may contain characters that Brigadier's built-in
 * {@code StringArgumentType.word()} rejects, such as {@code @} (e.g. {@code @p},
 * {@code @e[type=zombie]}) or path separators (e.g. {@code @../config/value}).
 *
 * <p><b>Client-side behaviour:</b> the argument is advertised to clients as
 * {@code brigadier:string GREEDY_PHRASE} so that the client does not show a
 * validation error for the special characters above. As a consequence, the client
 * treats this argument as consuming all remaining input; any arguments declared
 * <em>after</em> this one will not appear in the client's tab-completion UI.
 * Server-side parsing is unaffected — subsequent arguments are parsed correctly.
 * For this reason, prefer placing {@code UnparsedArgument} as the last argument
 * in a command.
 */
public class UnparsedArgument extends Argument<String, UnparsedArgument> {
    public UnparsedArgument(String name) {
        super(name, RawWordArgumentType.rawWord());
        RawWordArgumentType.ensureRegistered();
    }

    @Override
    public String cast(Object parsedArgument) {
        return ((String) parsedArgument);
    }

    @Override
    protected String parseImpl(CommandContext ctx) {
        return ctx.getHandle()
                  .getArgument(name(), String.class);
    }
}
