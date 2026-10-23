package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandHelp.Token;
import dev.marulabs.commandlib.CommandHelp.Usage;
import dev.marulabs.commandlib.util.ChatColorUtil;
import org.jetbrains.annotations.NotNull;

/**
 * {@link UsageFormatter#DEFAULT}.
 */
final class DefaultUsageFormatter implements UsageFormatter {
    private static final String PADDING = "  ";

    @NotNull
    @Override
    public String format(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Usage usage) {
        // Examples:
        //   "  /scan [options] <target>: Scan target"
        //   "  /config <key> set <value>: Set config value"
        StringBuilder sb = new StringBuilder("/");
        Token previous = null;
        for (Token token : usage.path()) {
            if (previous != null) {
                sb.append(' ');
            }
            // A literal after an argument is an argument-child command, which stands out as a literal does in the
            // routes; the literals before it keep the color of the line.
            if (token.kind() == Token.Kind.LITERAL && previous != null && previous.kind() == Token.Kind.ARGUMENT) {
                sb.append(ChatColorUtil.YELLOW);
            }
            sb.append(token.kind() == Token.Kind.LITERAL ? token.name() : format(token, ctx));
            previous = token;
        }
        for (Token token : usage.tokens()) {
            sb.append(' ')
              .append(format(token, ctx));
        }
        String line = ChatColorUtil.AQUA + PADDING + sb;
        if (usage.description()
                 .isEmpty()) {
            return line;
        }
        return line + ChatColorUtil.GRAY + ": " + usage.description();
    }

    private static String format(Token token, CommonCommandContext<?, ?> ctx) {
        switch (token.kind()) {
            case LITERAL:
                return ChatColorUtil.YELLOW + token.name();
            case OPTIONS:
                return ChatColorUtil.GRAY + "[" + ChatColorUtil.YELLOW + ctx.caption(Caption.HELP_OPTIONS_TAG)
                        + ChatColorUtil.GRAY + "]";
            case ARGUMENT:
                String name = token.name() + (token.variadic() ? "..." : "");
                String open = token.optional() ? "[" : "<";
                String close = token.optional() ? "]" : ">";
                return ChatColorUtil.GRAY + open + ChatColorUtil.YELLOW + name + ChatColorUtil.GRAY + close;
        }
        throw new AssertionError(token.kind());
    }
}
