package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandHelp.Token;
import dev.marulabs.commandlib.CommandHelp.Usage;
import dev.marulabs.commandlib.command.CommandExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@link CommandHelp} of a command for the sender and sends it with the command's {@link HelpFormatter}.
 */
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
        ctx.captions(command.captions());
        command.helpFormatter()
               .format(ctx, help(ctx));
    }

    CommandHelp help(C ctx) {
        List<Token> path = path(command, ctx);
        UsageFormatter formatter = command.usageFormatter();
        List<Token> prefix = optionTokens(command);

        List<Usage> usages = new ArrayList<>();
        // Examples:
        //   "/game start: Start game"
        //   "/config <key> set <value>: Set config value"
        command.children()
               .stream()
               .filter(x -> platformAdapter.hasPermission(x, ctx, permissionPrefix))
               .forEach(x -> usages.add(new Usage(path, append(prefix, Token.literal(x.name())), x.description(ctx))));
        for (Arguments<C> arguments : command.argumentsList()) {
            if (!hasArgumentPermission(command, arguments, ctx)) {
                continue;
            }
            for (CommonCommand<C, ?> child : arguments.children()) {
                if (platformAdapter.hasPermission(castCommand(child), ctx, permissionPrefix)) {
                    addArgumentChildUsages(usages,
                                           path,
                                           append(appendTags(prefix, arguments), Token.literal(child.name())),
                                           child,
                                           ctx);
                }
            }
        }
        // Example: "/scan [options] <target>: Scan target"
        for (Arguments<C> arguments : command.argumentsList()) {
            // A branch that only leads to children is listed by its child usages above.
            boolean endsUsage = arguments.children()
                                         .isEmpty()
                    || arguments.isExecutable();
            if (arguments.size() > 0 && endsUsage && hasArgumentPermission(command, arguments, ctx)) {
                usages.add(new Usage(path, appendTags(prefix, arguments), arguments.description(ctx)));
            }
        }
        if (usages.isEmpty() && !command.options()
                                        .isEmpty()) {
            usages.add(new Usage(path, prefix, ""));
        }
        usages.forEach(x -> x.format(ctx, formatter));

        List<CommandOption<?, C>> options = new ArrayList<>();
        for (CommandOption<?, C> option : command.options()) {
            String permission = option.permissionName(command, permissionPrefix);
            if (permission == null || platformAdapter.hasPermission(ctx, permission)) {
                options.add(option);
            }
        }
        return new CommandHelp(path, command.description(ctx), usages, options, command.examples());
    }

    private void addArgumentChildUsages(List<Usage> usages,
                                        List<Token> path,
                                        List<Token> tokens,
                                        CommonCommand<C, ?> command,
                                        C ctx) {
        // Examples:
        //   "/config <key> get"
        //   "/config <key> set <value>"
        //   "/config <key> group <name> delete"
        if (command.executor() != null) {
            // A literal after arguments is only a valid usage by itself when it can execute at that point.
            usages.add(new Usage(path, tokens, command.description(ctx)));
        }
        for (Arguments<C> arguments : command.argumentsList()) {
            if (!hasArgumentPermission(command, arguments, ctx)) {
                continue;
            }
            List<Token> argumentTokens = appendTags(tokens, arguments);
            if (arguments.children()
                         .isEmpty()) {
                // Terminal argument branches have no command node of their own, so this is the only place where
                // their description can appear in parent-command help.
                usages.add(new Usage(path, argumentTokens, arguments.description(ctx)));
                continue;
            }
            for (CommonCommand<C, ?> child : arguments.children()) {
                if (platformAdapter.hasPermission(castCommand(child), ctx, permissionPrefix)) {
                    addArgumentChildUsages(usages,
                                           path,
                                           append(argumentTokens, Token.literal(child.name())),
                                           child,
                                           ctx);
                }
            }
        }
    }

    private boolean hasArgumentPermission(CommonCommand<C, ?> parent, Arguments<C> arguments, C ctx) {
        return platformAdapter.hasPermission(ctx, arguments.permissionName(parent, permissionPrefix));
    }

    /**
     * The usage of {@code arguments} for the error of one of its arguments: the path of the branch's command, its
     * {@code [options]} tag and the branch's arguments.
     */
    static <C extends CommonCommandContext<?, ?>> Usage branchUsage(Arguments<C> arguments, C ctx) {
        CommonCommand<C, ?> owner = arguments.owner();
        return new Usage(path(owner, ctx),
                         appendTags(optionTokens(owner), arguments),
                         arguments.description(ctx)).format(ctx, owner.usageFormatter());
    }

    /**
     * The path to {@code command}. Examples: {@code game} for the root help, {@code game start} for a child's help,
     * {@code config <key> set} for an argument child's help.
     */
    private static List<Token> path(CommonCommand<?, ?> command, CommonCommandContext<?, ?> ctx) {
        List<Token> reversed = new ArrayList<>();
        CommonCommand<?, ?> current = command;
        while (current.parent() != null) {
            reversed.add(Token.literal(current.name()));
            Arguments<?> parentArguments = current.parentArguments();
            if (parentArguments != null) {
                // Argument-child commands are attached below an argument chain, so the literal alone is not enough
                // to reconstruct the executed path. Include the parent arguments here to produce paths like
                // "/config <key> set" instead of "/config set".
                List<Token> tags = appendTags(List.of(), parentArguments);
                for (int i = tags.size() - 1; i >= 0; i--) {
                    reversed.add(tags.get(i));
                }
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
        reversed.add(Token.literal(space == -1 ? rawInput : rawInput.substring(0, space)));

        List<Token> path = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }
        return path;
    }

    private static List<Token> optionTokens(CommonCommand<?, ?> command) {
        return command.options()
                      .isEmpty() ? List.of() : List.of(Token.options());
    }

    private static List<Token> appendTags(List<Token> tokens, Arguments<?> arguments) {
        List<Token> result = new ArrayList<>(tokens);
        arguments.stream()
                 .forEach(x -> result.add(Token.argument(x)));
        return result;
    }

    private static List<Token> append(List<Token> tokens, Token token) {
        List<Token> result = new ArrayList<>(tokens);
        result.add(token);
        return result;
    }

    @SuppressWarnings("unchecked")
    private U castCommand(CommonCommand<C, ?> command) {
        return (U) command;
    }
}
