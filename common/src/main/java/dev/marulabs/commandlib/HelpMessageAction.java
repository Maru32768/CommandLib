package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandHelp.Token;
import dev.marulabs.commandlib.CommandHelp.Usage;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.internal.ContextState;
import dev.marulabs.commandlib.internal.PlatformAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@link CommandHelp} of a command for the sender and sends it with the command's {@link HelpFormatter}.
 */
final class HelpMessageAction<S, T, C extends CommonCommandContext<S, T>> implements CommandExecutor<C> {
    private final PlatformAdapter<S, T, C> platformAdapter;
    private final CommonCommand<C, ?> command;
    private final String permissionPrefix;

    HelpMessageAction(PlatformAdapter<S, T, C> platformAdapter, CommonCommand<C, ?> command, String permissionPrefix) {
        this.platformAdapter = platformAdapter;
        this.command = command;
        this.permissionPrefix = permissionPrefix;
    }

    @Override
    public void accept(C ctx) {
        command.helpFormatter()
               .format(ctx, help(ctx));
    }

    CommandHelp help(C ctx) {
        List<Token> path = path(command, ctx);
        UsageFormatter formatter = command.usageFormatter();
        List<CommandOption<?, C>> options = usableOptions(platformAdapter, command, permissionPrefix, ctx);
        List<Token> prefix = options.isEmpty() ? List.of() : List.of(Token.options());

        List<Usage> usages = new ArrayList<>();
        // Examples:
        //   "/game start: Start game"
        //   "/config <key> set <value>: Set config value"
        command.children()
               .stream()
               .filter(x -> isUsable(x, ctx))
               .forEach(x -> usages.add(new Usage(path, append(prefix, Token.literal(x.name())), x.description(ctx))));
        for (Arguments<C> arguments : command.argumentsList()) {
            if (!hasArgumentPermission(arguments, ctx)) {
                continue;
            }
            for (CommonCommand<C, ?> child : arguments.children()) {
                if (isUsable(child, ctx)) {
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
            if (arguments.size() > 0 && arguments.endsUsage() && hasArgumentPermission(arguments, ctx)) {
                usages.add(new Usage(path, appendTags(prefix, arguments), arguments.description(ctx)));
            }
        }
        if (usages.isEmpty() && !options.isEmpty()) {
            usages.add(new Usage(path, prefix, ""));
        }
        usages.forEach(x -> x.format(ctx, formatter));
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
            if (!hasArgumentPermission(arguments, ctx)) {
                continue;
            }
            List<Token> argumentTokens = appendTags(tokens, arguments);
            if (arguments.endsUsage()) {
                // An argument branch has no command node of its own, so this is the only place where its description
                // can appear in parent-command help.
                usages.add(new Usage(path, argumentTokens, arguments.description(ctx)));
            }
            for (CommonCommand<C, ?> child : arguments.children()) {
                if (isUsable(child, ctx)) {
                    addArgumentChildUsages(usages,
                                           path,
                                           append(argumentTokens, Token.literal(child.name())),
                                           child,
                                           ctx);
                }
            }
        }
    }

    private boolean hasArgumentPermission(Arguments<C> arguments, C ctx) {
        return platformAdapter.hasPermission(ctx, arguments.permissionName(permissionPrefix))
                && (!arguments.hasVisibility() || arguments.isVisibleTo(state -> sourceContext(ctx, state)));
    }

    // The sender has the command's permission and the command is visible to the sender.
    private boolean isUsable(CommonCommand<C, ?> child, C ctx) {
        return platformAdapter.hasPermission(ctx, child.permissionName(permissionPrefix))
                && (!child.hasVisibility() || child.isVisibleTo(state -> sourceContext(ctx, state)));
    }

    // The predicates get the context the command tree gives them, which has the providers of the command or branch
    // they belong to, without changing the providers of the help's own context.
    private C sourceContext(C ctx, ContextState state) {
        return platformAdapter.createSourceContext(ctx.getHandle()
                                                      .getSource(),
                                                   state);
    }

    /**
     * The options of {@code owner} the sender has the permission of. The help and the usage of an argument error both
     * show an {@code [options]} tag for them.
     */
    static <S, C extends CommonCommandContext<S, ?>> List<CommandOption<?, C>> usableOptions(PlatformAdapter<S, ?, C> platformAdapter,
                                                                                             CommonCommand<C, ?> owner,
                                                                                             String permissionPrefix,
                                                                                             C ctx) {
        List<CommandOption<?, C>> options = new ArrayList<>();
        for (CommandOption<?, C> option : owner.options()) {
            String permission = option.permissionName(owner, permissionPrefix);
            if (permission == null || platformAdapter.hasPermission(ctx, permission)) {
                options.add(option);
            }
        }
        return options;
    }

    /**
     * The usage of {@code arguments} for the error of one of its arguments: the path of the branch's command, its
     * {@code [options]} tag when the sender may use one of its options, as in the help, and the branch's arguments.
     */
    static <C extends CommonCommandContext<?, ?>> Usage branchUsage(Arguments<C> arguments, C ctx, boolean options) {
        CommonCommand<C, ?> owner = arguments.owner();
        return new Usage(path(owner, ctx),
                         appendTags(options ? List.of(Token.options()) : List.of(), arguments),
                         arguments.description(ctx)).format(ctx, owner.usageFormatter());
    }

    /**
     * The path to {@code command}. Examples: {@code game} for the root help, {@code game start} for a child's help,
     * {@code config <key> set} for an argument child's help.
     */
    // The token the root command was typed as. Through a redirect such as /execute ... run game, the input starts with
    // another command, so it is the last token at the start or after "run" that names the root command or an alias,
    // with or without a namespace.
    private static String rootLiteral(String input, CommonCommand<?, ?> root) {
        String[] tokens = (input.startsWith("/") ? input.substring(1) : input).split(" ", -1);
        Set<String> names = new HashSet<>(root.aliases());
        names.add(root.name());
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            String name = token.substring(token.indexOf(':') + 1);
            if ((i == 0 || tokens[i - 1].equals("run")) && names.contains(name)) {
                return token;
            }
        }
        return tokens[0];
    }

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
        reversed.add(Token.literal(rootLiteral(ctx.getHandle()
                                                  .getInput(),
                                               current)));

        List<Token> path = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }
        return path;
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
}
