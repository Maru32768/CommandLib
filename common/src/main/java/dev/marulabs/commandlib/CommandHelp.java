package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The generated help of one command, as one sender sees it, for a {@link HelpFormatter}: the command's path, its
 * description, the usage lines the sender has the permissions for, the options and the examples.
 */
public final class CommandHelp {
    private final List<Token> path;
    private final String description;
    private final List<Usage> usages;
    private final List<CommandOption<?, ?>> options;
    private final List<String> examples;

    CommandHelp(List<Token> path,
                String description,
                List<Usage> usages,
                List<? extends CommandOption<?, ?>> options,
                List<String> examples) {
        this.path = List.copyOf(path);
        this.description = description;
        this.usages = List.copyOf(usages);
        this.options = List.copyOf(options);
        this.examples = List.copyOf(examples);
    }

    /**
     * The path from the root command to this one: the root literal as the sender typed it, so an alias stays, the
     * child literals and, before a command that follows an argument branch, that branch's arguments, such as
     * {@code config <key> set}.
     */
    @NotNull
    public List<Token> path() {
        return path;
    }

    /**
     * The command's description for the sender, empty when it has none.
     */
    @NotNull
    public String description() {
        return description;
    }

    /**
     * The usage lines in the order the default help lists them: the child commands, the routes through the argument
     * branches to their child commands, then the command's own argument branches. Only the routes the sender has the
     * permissions for are listed. A command with options but no other route has one line for its options.
     */
    @NotNull
    public List<Usage> usages() {
        return usages;
    }

    /**
     * The command's options.
     */
    @NotNull
    public List<CommandOption<?, ?>> options() {
        return options;
    }

    /**
     * The examples set with {@link CommonCommand#examples(String...)}.
     */
    @NotNull
    public List<String> examples() {
        return examples;
    }

    /**
     * One usage line: the command's path, then the tokens of the route, such as the {@code [options]} tag, argument
     * tags and child literals.
     */
    public static final class Usage {
        private final List<Token> path;
        private final List<Token> tokens;
        private final String description;
        private @Nullable String line;

        Usage(List<Token> path, List<Token> tokens, String description) {
            this.path = List.copyOf(path);
            this.tokens = List.copyOf(tokens);
            this.description = description;
        }

        /**
         * The path of the command whose help or error shows this line, see {@link CommandHelp#path()}.
         */
        @NotNull
        public List<Token> path() {
            return path;
        }

        /**
         * The tokens after the path.
         */
        @NotNull
        public List<Token> tokens() {
            return tokens;
        }

        /**
         * The description of the route's command or argument branch, empty when it has none.
         */
        @NotNull
        public String description() {
            return description;
        }

        /**
         * The line as the command's {@link UsageFormatter} formats it.
         */
        @NotNull
        public String line() {
            return Objects.requireNonNull(line, "line");
        }

        /**
         * The usage without colors and description, such as {@code /config <key> set [options] <value>}.
         */
        @NotNull
        public String plainText() {
            List<Token> all = new ArrayList<>(path);
            all.addAll(tokens);
            return all.stream()
                      .map(Token::plainText)
                      .collect(Collectors.joining(" ", "/", ""));
        }

        Usage format(CommonCommandContext<?, ?> ctx, UsageFormatter formatter) {
            line = Objects.requireNonNull(formatter.format(ctx, this), "A usage formatter returned null");
            return this;
        }

        @Override
        public String toString() {
            return plainText();
        }
    }

    /**
     * A token of a usage line.
     */
    public static final class Token {
        /**
         * The kind of a token.
         */
        public enum Kind {
            /** A command literal. */
            LITERAL,
            /** An argument. */
            ARGUMENT,
            /** The {@code [options]} tag of a command with options. */
            OPTIONS
        }

        private final Kind kind;
        private final String name;
        private final boolean optional;
        private final boolean variadic;

        private Token(Kind kind, String name, boolean optional, boolean variadic) {
            this.kind = kind;
            this.name = name;
            this.optional = optional;
            this.variadic = variadic;
        }

        static Token literal(String name) {
            return new Token(Kind.LITERAL, name, false, false);
        }

        static Token argument(CommonArgument<?, ?, ?> argument) {
            return new Token(Kind.ARGUMENT,
                             argument.name(),
                             argument.isOptional(),
                             argument instanceof VariadicArgument);
        }

        static Token options() {
            return new Token(Kind.OPTIONS, "options", false, false);
        }

        @NotNull
        public Kind kind() {
            return kind;
        }

        /**
         * The literal, the argument's name, or {@code options}.
         */
        @NotNull
        public String name() {
            return name;
        }

        /**
         * Whether the argument may be omitted.
         */
        public boolean optional() {
            return optional;
        }

        /**
         * Whether the argument takes several values.
         */
        public boolean variadic() {
            return variadic;
        }

        /**
         * The token as usages show it: the literal, {@code <name>}, {@code [name]}, {@code <name...>} or
         * {@code [options]}.
         */
        @NotNull
        public String plainText() {
            if (kind == Kind.LITERAL) {
                return name;
            }
            String text = name + (variadic ? "..." : "");
            return optional || kind == Kind.OPTIONS ? "[" + text + "]" : "<" + text + ">";
        }

        @Override
        public String toString() {
            return plainText();
        }
    }
}
