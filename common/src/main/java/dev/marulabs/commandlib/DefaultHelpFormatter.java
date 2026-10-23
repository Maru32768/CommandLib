package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.ChatColorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link HelpFormatter#DEFAULT}.
 */
final class DefaultHelpFormatter implements HelpFormatter {
    private static final String PADDING = "  ";

    @Override
    public void format(@NotNull CommonCommandContext<?, ?> ctx, @NotNull CommandHelp help) {
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
