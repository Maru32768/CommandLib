package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandHelp.Token;
import dev.marulabs.commandlib.CommandHelp.Usage;
import dev.marulabs.commandlib.util.ChatColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link HelpFormatter#DEFAULT} and {@link UsageFormatter#DEFAULT}.
 */
final class DefaultHelpFormatter {
    private static final String PADDING = "  ";

    private DefaultHelpFormatter() {
    }

    static void help(CommonCommandContext<?, ?> ctx, CommandHelp help) {
        String border = ChatColorUtil.GRAY + "-".repeat(50);
        ctx.sendMessage(border);
        if (!help.description()
                 .isEmpty()) {
            ctx.sendMessage(help.description());
        }
        ctx.sendMessage(ChatColorUtil.RED + ctx.caption(Caption.HELP_USAGE));
        help.usages()
            .forEach(x -> ctx.sendMessage(x.line()));

        List<CommandOption<?, ?>> options = help.options();
        if (!options.isEmpty()) {
            // The line of the options alone is the only one without another token; it needs no gap before them.
            boolean onlyOptionsLine = help.usages()
                                          .size() == 1
                    && help.usages()
                           .get(0)
                           .tokens()
                           .size() == 1;
            if (!onlyOptionsLine) {
                ctx.sendMessage("");
            }
            ctx.sendMessage(ChatColorUtil.RED + ctx.caption(Caption.HELP_OPTIONS));
            options.forEach(x -> ctx.sendMessage(formatOption(x, ctx)));
        }

        if (!help.examples()
                 .isEmpty()) {
            ctx.sendMessage("");
            ctx.sendMessage(ChatColorUtil.RED + ctx.caption(Caption.HELP_EXAMPLES));
            help.examples()
                .forEach(x -> ctx.sendMessage(ChatColorUtil.AQUA + PADDING + (x.startsWith("/") ? x : "/" + x)));
        }

        ctx.sendMessage(border);
    }

    static String usage(CommonCommandContext<?, ?> ctx, Usage usage) {
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
            default:
                String name = token.name() + (token.variadic() ? "..." : "");
                String open = token.optional() ? "[" : "<";
                String close = token.optional() ? "]" : ">";
                return ChatColorUtil.GRAY + open + ChatColorUtil.YELLOW + name + ChatColorUtil.GRAY + close;
        }
    }

    private static String formatOption(CommandOption<?, ?> option, CommonCommandContext<?, ?> ctx) {
        // Examples:
        //   "  --force: Force execution"
        //   "  -n <limit>, --limit <limit>: Maximum count"
        //   "  -t <tag>, --tag <tag> (required, up to 3 times): Tags"
        String s = ChatColorUtil.YELLOW + PADDING + formatOptionNames(option) + formatOptionNotes(option, ctx);
        if (option.description()
                  .isEmpty()) {
            return s;
        }
        return s + ChatColorUtil.GRAY + ": " + option.description();
    }

    private static String formatOptionNames(CommandOption<?, ?> option) {
        // Examples:
        //   "--force"
        //   "-n <limit>, --limit <limit>"
        //   "-f, --force, --yes"
        String valueTag = option.hasValue()
                ? " " + ChatColorUtil.GRAY + "<" + ChatColorUtil.YELLOW + option.name() + ChatColorUtil.GRAY + ">" : "";

        List<String> names = new ArrayList<>();
        if (option.shortName() != null) {
            names.add("-" + option.shortName() + valueTag);
        }
        for (String name : option.names()) {
            names.add("--" + name + valueTag);
        }
        return String.join(ChatColorUtil.GRAY + ", " + ChatColorUtil.YELLOW, names);
    }

    private static String formatOptionNotes(CommandOption<?, ?> option, CommonCommandContext<?, ?> ctx) {
        // Examples: "", " (required)", " (up to 3 times)"
        List<String> notes = new ArrayList<>();
        if (option.isRequired()) {
            notes.add(ctx.caption(Caption.HELP_REQUIRED));
        }
        if (option.maxOccurrences() > 1) {
            notes.add(ctx.caption(Caption.HELP_MAX_OCCURRENCES, Map.of("count", option.maxOccurrences())));
        }
        if (notes.isEmpty()) {
            return "";
        }
        return ChatColorUtil.GRAY + " (" + String.join(", ", notes) + ")";
    }
}
