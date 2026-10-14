package dev.marulabs.commandlib;

import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.util.ChatColorUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class HelpMessageAction<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> implements CommandExecutor<C> {
    private final PlatformAdapter<S, T, C, U> platformAdapter;
    private final U command;
    private final String permissionPrefix;

    HelpMessageAction(PlatformAdapter<S, T, C, U> platformAdapter, U command, String permissionPrefix) {
        this.platformAdapter = platformAdapter;
        this.command = command;
        this.permissionPrefix = permissionPrefix;
    }

    @Override
    public void accept(C ctx) {
        String border = ChatColorUtil.GRAY + "-".repeat(50);
        String padding = " ".repeat(2);
        String usagePrefix = createUsagePrefix(command, ctx);

        ctx.sendMessage(border);

        if (!command.description(ctx)
                    .isEmpty()) {
            ctx.sendMessage(command.description(ctx));
        }
        ctx.sendMessage(ChatColorUtil.RED + "Usage:");

        List<String> childUsages = createChildUsages(command, ctx, padding, usagePrefix);
        childUsages.forEach(ctx::sendMessage);

        List<String> argumentUsages = command.argumentsList()
                                             .stream()
                                             .filter(arguments -> hasArgumentPermission(command, arguments, ctx))
                                             .map(arguments -> formatArgumentUsage(usagePrefix,
                                                                                   command,
                                                                                   arguments,
                                                                                   padding,
                                                                                   ctx))
                                             .filter(Predicate.not(String::isEmpty))
                                             .collect(Collectors.toList());
        argumentUsages.forEach(ctx::sendMessage);

        List<String> optionDescriptions = command.options()
                                                 .stream()
                                                 .map(x -> formatOptionDescription(padding, x))
                                                 .collect(Collectors.toList());
        if (childUsages.isEmpty() && argumentUsages.isEmpty() && !optionDescriptions.isEmpty()) {
            ctx.sendMessage(ChatColorUtil.AQUA + padding + "/" + usagePrefix + concatOptionUsage(command));
        }
        if (!optionDescriptions.isEmpty()) {
            if (!childUsages.isEmpty() || !argumentUsages.isEmpty()) {
                ctx.sendMessage("");
            }
            ctx.sendMessage(ChatColorUtil.RED + "Options:");
            optionDescriptions.forEach(ctx::sendMessage);
        }

        ctx.sendMessage(border);
    }

    private String formatArgumentUsage(String usagePrefix, U command, Arguments<C> arguments, String padding, C ctx) {
        String tagNames = arguments.concatTagNames();
        if (tagNames.isEmpty()) {
            return "";
        }

        // Example: "  /scan [options] <target>: Scan target"
        String s = padding + ChatColorUtil.AQUA + "/" + usagePrefix + concatOptionUsage(command) + " " + tagNames;
        if (arguments.description(ctx)
                     .isEmpty()) {
            return s;
        }
        return s + ChatColorUtil.GRAY + ": " + arguments.description(ctx);
    }

    private List<String> createChildUsages(U command, C ctx, String padding, String usagePrefix) {
        // Examples:
        //   "/game start: Start game"
        //   "/config <key> set <value>: Set config value"
        String commandPrefix = ChatColorUtil.AQUA + padding + "/" + usagePrefix + concatOptionUsage(command);
        List<String> childUsages = command.children()
                                          .stream()
                                          .filter(x -> platformAdapter.hasPermission(x, ctx, permissionPrefix))
                                          .map(x -> formatCommandChildUsage(commandPrefix, x, ctx))
                                          .collect(Collectors.toList());
        command.argumentsList()
               .stream()
               .filter(arguments -> hasArgumentPermission(command, arguments, ctx))
               .flatMap(arguments -> arguments.children()
                                              .stream()
                                              .filter(x -> platformAdapter.hasPermission(castCommand(x),
                                                                                         ctx,
                                                                                         permissionPrefix))
                                              .flatMap(child -> createArgumentChildUsages(arguments,
                                                                                          child,
                                                                                          ctx,
                                                                                          commandPrefix).stream()))
               .forEach(childUsages::add);
        return childUsages;
    }

    private String formatCommandChildUsage(String commandPrefix, U child, C ctx) {
        // Example: "/game start: Start game"
        String s = commandPrefix + " " + ChatColorUtil.YELLOW + child.name();
        if (child.description(ctx)
                 .isEmpty()) {
            return s;
        }
        return s + ChatColorUtil.GRAY + ": " + child.description(ctx);
    }

    private List<String> createArgumentChildUsages(Arguments<C> arguments,
                                                   CommonCommand<C, ?> child,
                                                   C ctx,
                                                   String commandPrefix) {
        // Example: "/config <key> get"
        return createArgumentChildUsages(commandPrefix + " " + arguments.concatTagNames() + " " + ChatColorUtil.YELLOW
                + child.name(), child, ctx);
    }

    private List<String> createArgumentChildUsages(String prefix, CommonCommand<C, ?> command, C ctx) {
        // Examples:
        //   "/config <key> get"
        //   "/config <key> set <value>"
        //   "/config <key> group <name> delete"
        List<String> usages = new ArrayList<>();
        if (command.executor() != null) {
            // A literal after arguments is only a valid usage by itself when it can execute at that point.
            usages.add(formatArgumentChildUsage(prefix, command, ctx));
        }

        command.argumentsList()
               .stream()
               .filter(arguments -> hasArgumentPermission(command, arguments, ctx))
               .forEach(arguments -> {
                   String argumentPrefix = prefix + " " + arguments.concatTagNames();
                   if (arguments.children()
                                .isEmpty()) {
                       // Terminal argument branches have no command node of their own, so this is the only place
                       // where their description can appear in parent-command help.
                       usages.add(formatArgumentChildArgumentUsage(argumentPrefix, arguments, ctx));
                       return;
                   }

                   arguments.children()
                            .stream()
                            .filter(x -> platformAdapter.hasPermission(castCommand(x), ctx, permissionPrefix))
                            .flatMap(child -> createArgumentChildUsages(argumentPrefix + " " + ChatColorUtil.YELLOW
                                    + child.name(), child, ctx).stream())
                            .forEach(usages::add);
               });
        return usages;
    }

    private boolean hasArgumentPermission(CommonCommand<C, ?> parent, Arguments<C> arguments, C ctx) {
        return platformAdapter.hasPermission(ctx, arguments.permissionName(parent, permissionPrefix));
    }

    private String formatArgumentChildUsage(String prefix, CommonCommand<C, ?> command, C ctx) {
        // Example: "/config <key> get: Get config value"
        if (command.description(ctx)
                   .isEmpty()) {
            return prefix;
        }
        return prefix + ChatColorUtil.GRAY + ": " + command.description(ctx);
    }

    private String formatArgumentChildArgumentUsage(String prefix, Arguments<C> arguments, C ctx) {
        // Example: "/config <key> set <value>: Set config value"
        if (arguments.description(ctx)
                     .isEmpty()) {
            return prefix;
        }
        return prefix + ChatColorUtil.GRAY + ": " + arguments.description(ctx);
    }

    private String createUsagePrefix(U command, C ctx) {
        // Examples:
        //   root help: "game"
        //   child help: "game start"
        //   argument-child help: "config <key> set"
        // The root name uses ctx.getInput(0), so aliases are preserved in help output.
        Deque<String> parts = new ArrayDeque<>();
        U current = command;
        while (current != null) {
            Arguments<C> parentArguments = current.parentArguments();
            if (parentArguments != null) {
                // Argument-child commands are attached below an argument chain, so the literal alone is not enough
                // to reconstruct the executed path. Include the parent arguments here to produce paths like
                // "/config <key> set" instead of "/config set".
                parts.addFirst(ChatColorUtil.YELLOW + current.name());
                parts.addFirst(parentArguments.concatTagNames());
            } else if (current.parent() != null) {
                // Normal child commands do not have parent arguments. They still need their own literal in the
                // prefix; otherwise child help such as "/manage start" would be rendered as just "/manage".
                parts.addFirst(current.name());
            }
            current = current.parent();
        }

        // The parsed input may use an alias for the root command; help should echo what the sender actually typed.
        // Use the raw handle input rather than ctx.getInput(0): some platforms (Paper 1.21+) omit the root
        // literal from CommandContext.getNodes(), so the map-based lookup would return the wrong token.
        String rawInput = ctx.getHandle()
                             .getInput();
        if (rawInput.startsWith("/")) {
            rawInput = rawInput.substring(1);
        }
        int space = rawInput.indexOf(' ');
        parts.addFirst(space == -1 ? rawInput : rawInput.substring(0, space));
        return parts.stream()
                    .filter(Predicate.not(String::isEmpty))
                    .collect(Collectors.joining(" "));
    }

    private String concatOptionUsage(U command) {
        // Example: " [options]"
        if (command.options()
                   .isEmpty()) {
            return "";
        }

        return " " + ChatColorUtil.GRAY + "[" + ChatColorUtil.YELLOW + "options" + ChatColorUtil.GRAY + "]";
    }

    private String formatOptionDescription(String padding, CommandOption<?, C> option) {
        // Examples:
        //   "  --force: Force execution"
        //   "  -n <limit>, --limit <limit>: Maximum count"
        //   "  -t <tag>, --tag <tag> (required, up to 3 times): Tags"
        String s = ChatColorUtil.YELLOW + padding + formatOptionNames(option) + formatOptionNotes(option);
        if (option.description()
                  .isEmpty()) {
            return s;
        }

        return s + ChatColorUtil.GRAY + ": " + option.description();
    }

    private String formatOptionNames(CommandOption<?, C> option) {
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

    private String formatOptionNotes(CommandOption<?, C> option) {
        // Examples: "", " (required)", " (up to 3 times)"
        List<String> notes = new ArrayList<>();
        if (option.isRequired()) {
            notes.add("required");
        }
        if (option.maxOccurrences() > 1) {
            notes.add("up to " + option.maxOccurrences() + " times");
        }
        if (notes.isEmpty()) {
            return "";
        }
        return ChatColorUtil.GRAY + " (" + String.join(", ", notes) + ")";
    }

    @SuppressWarnings("unchecked")
    private U castCommand(CommonCommand<C, ?> command) {
        return (U) command;
    }
}
