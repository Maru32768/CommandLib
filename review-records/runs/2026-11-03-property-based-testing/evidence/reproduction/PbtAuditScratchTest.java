package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Temporary property-based audit tests (2026-11-03). Each property generates inputs from a seed, shrinks a failure to
 * a smaller case, and reports the seed and the minimal input.
 */
class PbtAuditScratchTest {
    private static final int CASES = Integer.getInteger("pbt.cases", 3000);
    private static final long SEED = Long.getLong("pbt.seed", 20261103L);

    static final class Word extends CommonStringArgument<TestCommandContext, Word> {
        Word(String name) {
            super(name, Type.WORD);
        }
    }

    static final class Quoted extends CommonStringArgument<TestCommandContext, Quoted> {
        Quoted(String name) {
            super(name);
        }
    }

    static final class Phrase extends CommonStringArgument<TestCommandContext, Phrase> {
        Phrase(String name) {
            super(name, Type.PHRASE);
        }
    }

    static final class Int extends CommonIntegerArgument<TestCommandContext, Int> {
        Int(String name) {
            super(name);
        }
    }

    // ---------------------------------------------------------------- P1: options round trip

    static final class OptionCase {
        int shape;
        boolean anywhere;
        String prefix = "";
        String label = "scan";
        boolean force;
        boolean quiet;
        int verbose;
        Integer limit;
        List<String> tags = new ArrayList<>();
        String who;
        String target = "a";
        Integer amount;
        long renderSeed;

        OptionCase copy() {
            OptionCase c = new OptionCase();
            c.shape = shape;
            c.anywhere = anywhere;
            c.prefix = prefix;
            c.label = label;
            c.force = force;
            c.quiet = quiet;
            c.verbose = verbose;
            c.limit = limit;
            c.tags = new ArrayList<>(tags);
            c.who = who;
            c.target = target;
            c.amount = amount;
            c.renderSeed = renderSeed;
            return c;
        }
    }

    private static TestCommand optionCommand(int shape, boolean anywhere, List<String> sink) {
        TestCommand scan = new TestCommand("scan") {{
            addAliases("sc");
            if (anywhere) {
                optionsAnywhere();
            }
            CommandOption<Boolean, TestCommandContext> force = option(Options.<TestCommandContext>flag("force", 'f')
                                                                             .aliases("yes"));
            CommandOption<Boolean, TestCommandContext> quiet = option(Options.flag("quiet", 'q'));
            CommandOption<Integer, TestCommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            CommandOption<List<String>, TestCommandContext> tags = option(Options.<TestCommandContext>string("tag",
                                                                                                             't',
                                                                                                             "")
                                                                                 .repeatable(3));
            CommandOption<String, TestCommandContext> who = option(Options.of("who",
                                                                             'w',
                                                                             new Word("who").optional("nobody")));
            argument(new Quoted("target"), new Int("amount").optional(1)).execute((ctx, target, amount) -> {
                sink.add("target=" + target + " amount=" + amount + " f=" + ctx.getOption(force) + " q="
                        + ctx.getOption(quiet) + " v=" + ctx.getOption(verbose) + " n=" + ctx.getOption(limit)
                        + " t=" + ctx.getOption(tags) + " w=" + ctx.getOption(who) + " inputs=" + ctx.getInputs());
            });
        }};
        if (shape == 0) {
            return scan;
        }
        TestCommand parent = new TestCommand(shape == 1 ? "tool" : "scan") {{
            addAliases(shape == 1 ? "tl" : "sc");
            execute(ctx -> sink.add("parent"));
        }};
        parent.addChildren(scan);
        return parent;
    }

    private static TestCommandRunner withRedirects(TestCommandRunner runner) {
        runner.redirectToRoot("run");
        runner.root()
              .addChild(LiteralArgumentBuilder.<TestCommandSource>literal("as")
                                              .then(RequiredArgumentBuilder.<TestCommandSource, String>argument("name",
                                                                                                                StringArgumentType.word())
                                                                           .then(LiteralArgumentBuilder.<TestCommandSource>literal("go")
                                                                                                       .redirect(runner.root())))
                                              .build());
        return runner;
    }

    private static final String[] WORDS = {"a", "bob", "scan", "sc", "tool", "x1", "run", "as"};
    private static final char[] CHARS = {'a', 'b', 'Z', '0', ' ', '"', '\\', '\'', '-', 'f', ':', '.', '+', '_', 'é'};

    private static String randomString(Random r) {
        if (r.nextInt(4) == 0) {
            return new String[]{"scan", "-f", "--force", "-n", "sc", "tool", "", " ", "-", "--", "run", "a b"}[r.nextInt(12)];
        }
        int length = r.nextInt(6);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[r.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private static boolean unquotable(String s) {
        return !s.isEmpty() && s.chars()
                                .allMatch(x -> StringReader.isAllowedInUnquotedString((char) x));
    }

    // A token that StringArgumentType.string() reads back as s.
    private static String stringToken(String s, Random r, boolean mayBeOption) {
        if (unquotable(s) && !(mayBeOption && s.startsWith("-")) && r.nextBoolean()) {
            return s;
        }
        if (s.indexOf('\'') < 0 && r.nextInt(3) == 0) {
            return "'" + s.replace("\\", "\\\\") + "'";
        }
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"") + "\"";
    }

    private static OptionCase randomOptionCase(Random r) {
        OptionCase c = new OptionCase();
        c.shape = r.nextInt(3);
        c.anywhere = r.nextBoolean();
        switch (r.nextInt(4)) {
            case 0:
                c.prefix = "run ";
                break;
            case 1:
                c.prefix = "as " + WORDS[r.nextInt(WORDS.length)] + " go ";
                break;
            default:
                c.prefix = "";
        }
        c.label = r.nextBoolean() ? "scan" : "sc";
        c.force = r.nextBoolean();
        c.quiet = r.nextBoolean();
        c.verbose = r.nextInt(4);
        c.limit = r.nextBoolean() ? 1 + r.nextInt(100) : null;
        int tags = r.nextInt(4);
        for (int i = 0; i < tags; i++) {
            c.tags.add(randomString(r));
        }
        c.who = r.nextBoolean() ? WORDS[r.nextInt(WORDS.length)] : null;
        c.target = randomString(r);
        c.amount = r.nextBoolean() ? r.nextInt(2001) - 1000 : null;
        c.renderSeed = r.nextLong();
        return c;
    }

    private static String pathPrefix(OptionCase c) {
        switch (c.shape) {
            case 0:
                return "";
            case 1:
                return "tool ";
            default:
                return "scan ";
        }
    }

    // Renders the case into an input and the message the executor must send.
    private static Rendered render(OptionCase c) {
        Random r = new Random(c.renderSeed);
        List<Character> units = new ArrayList<>();
        if (c.force) {
            units.add('f');
        }
        if (c.quiet) {
            units.add('q');
        }
        for (int i = 0; i < c.verbose; i++) {
            units.add('v');
        }
        List<List<String>> atoms = new ArrayList<>();
        while (!units.isEmpty()) {
            int choice = r.nextInt(3);
            long vs = units.stream()
                           .filter(x -> x == 'v')
                           .count();
            List<Character> kinds = new ArrayList<>(new java.util.LinkedHashSet<>(units));
            if (choice == 1 && vs >= 2) {
                int k = 2 + r.nextInt((int) vs - 1);
                for (int i = 0; i < k; i++) {
                    units.remove(Character.valueOf('v'));
                }
                atoms.add(List.of(HEAD + "-" + "v".repeat(k)));
            } else if (choice == 2 && kinds.size() >= 2) {
                Collections.shuffle(kinds, r);
                int size = 2 + r.nextInt(kinds.size() - 1);
                StringBuilder token = new StringBuilder("-");
                for (Character kind : kinds.subList(0, size)) {
                    units.remove(kind);
                    token.append(kind.charValue());
                }
                atoms.add(List.of(HEAD + token));
            } else {
                Character unit = units.remove(r.nextInt(units.size()));
                atoms.add(List.of(HEAD + single(unit, r)));
            }
        }
        if (c.limit != null) {
            atoms.add(List.of(HEAD + (r.nextBoolean() ? "--limit" : "-n"), c.limit.toString()));
        }
        for (int i = 0; i < c.tags.size(); i++) {
            atoms.add(List.of(r.nextBoolean() ? "--tag" : "-t", "\u0000TAG"));
        }
        if (c.who != null) {
            atoms.add(List.of(HEAD + (r.nextBoolean() ? "--who" : "-w"), c.who));
        }
        Collections.shuffle(atoms, r);
        List<List<String>> slot0 = new ArrayList<>();
        List<List<String>> slot1 = new ArrayList<>();
        List<List<String>> slot2 = new ArrayList<>();
        for (List<String> atom : atoms) {
            int slot = c.anywhere ? r.nextInt(c.amount != null ? 3 : 2) : 0;
            (slot == 0 ? slot0 : slot == 1 ? slot1 : slot2).add(atom);
        }
        String targetToken = stringToken(c.target, r, true);
        List<String> tokens = new ArrayList<>();
        slot0.forEach(tokens::addAll);
        tokens.add(targetToken);
        slot1.forEach(tokens::addAll);
        if (c.amount != null) {
            tokens.add(c.amount.toString());
        }
        slot2.forEach(tokens::addAll);
        int tag = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i)
                      .equals("\u0000TAG")) {
                tokens.set(i, stringToken(c.tags.get(tag++), r, false));
            }
        }
        StringBuilder text = new StringBuilder(c.prefix + pathPrefix(c) + c.label);
        List<int[]> optionTokens = new ArrayList<>();
        for (String token : tokens) {
            text.append(' ');
            if (token.startsWith(HEAD)) {
                token = token.substring(HEAD.length());
                optionTokens.add(new int[]{text.length(), text.length() + token.length()});
            }
            text.append(token);
        }
        String input = text.toString();
        List<String> inputs = new ArrayList<>();
        // F-INPUTS (reported separately): getInputs() keys literals by node name, so "scan scan" keeps one of them.
        if (c.shape == 1 || (c.shape == 2 && !c.label.equals("scan"))) {
            inputs.add(c.shape == 1 ? "tool" : "scan");
        }
        inputs.add(c.label);
        inputs.add(targetToken);
        if (c.amount != null) {
            inputs.add(c.amount.toString());
        }
        String expected = "target=" + c.target + " amount=" + (c.amount != null ? c.amount : 1) + " f=" + c.force
                + " q=" + c.quiet + " v=" + c.verbose + " n=" + (c.limit != null ? c.limit : 10) + " t=" + c.tags
                + " w=" + (c.who != null ? c.who : "nobody") + " inputs=" + inputs;
        return new Rendered(input, expected, optionTokens);
    }

    private static final String HEAD = "\u0001";

    static final class Rendered {
        final String input;
        final String expected;
        final List<int[]> optionTokens;

        Rendered(String input, String expected, List<int[]> optionTokens) {
            this.input = input;
            this.expected = expected;
            this.optionTokens = optionTokens;
        }
    }

    private static String single(char unit, Random r) {
        switch (unit) {
            case 'f':
                return new String[]{"-f", "--force", "--yes"}[r.nextInt(3)];
            case 'q':
                return r.nextBoolean() ? "-q" : "--quiet";
            default:
                return r.nextBoolean() ? "-v" : "--verbose";
        }
    }

    private static final List<String> SINK = new ArrayList<>();
    private static final TestCommandRunner[][] OPTION_RUNNERS = new TestCommandRunner[3][2];

    private static TestCommandRunner optionRunner(OptionCase c) {
        int anywhere = c.anywhere ? 1 : 0;
        if (OPTION_RUNNERS[c.shape][anywhere] == null) {
            OPTION_RUNNERS[c.shape][anywhere] = withRedirects(new TestCommandRunner(optionCommand(c.shape,
                                                                                                 c.anywhere,
                                                                                                 SINK)));
        }
        return OPTION_RUNNERS[c.shape][anywhere];
    }

    // Null when the case holds, else the observed outcome.
    private static String checkOptionCase(OptionCase c) {
        Rendered rendered = render(c);
        SINK.clear();
        String actual;
        try {
            TestCommandContext ctx = optionRunner(c).execute(rendered.input);
            actual = SINK.isEmpty() ? "no executor; messages=" + (ctx == null ? null : ctx.messages())
                    : String.join("|", SINK);
        } catch (CommandSyntaxException e) {
            actual = "syntax error: " + e.getMessage();
        } catch (RuntimeException e) {
            actual = "exception: " + e;
        }
        return actual.equals(rendered.expected) ? null : actual;
    }

    private static List<OptionCase> shrinkCandidates(OptionCase c) {
        List<OptionCase> out = new ArrayList<>();
        java.util.function.Consumer<java.util.function.Consumer<OptionCase>> add = f -> {
            OptionCase x = c.copy();
            f.accept(x);
            out.add(x);
        };
        if (!c.prefix.isEmpty()) {
            add.accept(x -> x.prefix = "");
        }
        if (c.shape != 0) {
            add.accept(x -> x.shape = 0);
        }
        if (c.anywhere) {
            add.accept(x -> x.anywhere = false);
        }
        if (!c.label.equals("scan")) {
            add.accept(x -> x.label = "scan");
        }
        if (c.force) {
            add.accept(x -> x.force = false);
        }
        if (c.quiet) {
            add.accept(x -> x.quiet = false);
        }
        if (c.verbose > 0) {
            add.accept(x -> x.verbose--);
        }
        if (c.limit != null) {
            add.accept(x -> x.limit = null);
        }
        for (int i = 0; i < c.tags.size(); i++) {
            int index = i;
            add.accept(x -> x.tags.remove(index));
            if (!c.tags.get(i)
                       .equals("a")) {
                add.accept(x -> x.tags.set(index, "a"));
            }
        }
        if (c.who != null) {
            add.accept(x -> x.who = null);
        }
        if (c.amount != null) {
            add.accept(x -> x.amount = null);
        }
        if (!c.target.equals("a")) {
            add.accept(x -> x.target = "a");
        }
        for (long s = 0; s < 8; s++) {
            long seed = s;
            add.accept(x -> x.renderSeed = seed);
        }
        return out;
    }

    // Fields that differ from the simplest case, so that shrinking always makes progress.
    private static int weight(OptionCase c) {
        return (c.prefix.isEmpty() ? 0 : 1) + c.shape + (c.anywhere ? 1 : 0) + (c.label.equals("scan") ? 0 : 1)
                + (c.force ? 1 : 0) + (c.quiet ? 1 : 0) + c.verbose + (c.limit != null ? 1 : 0) + 2 * c.tags.size()
                - (int) c.tags.stream()
                              .filter("a"::equals)
                              .count()
                + (c.who != null ? 1 : 0) + (c.amount != null ? 1 : 0) + (c.target.equals("a") ? 0 : 1);
    }

    private static OptionCase shrink(OptionCase c) {
        String before = render(c).input;
        boolean progress = true;
        while (progress) {
            progress = false;
            for (OptionCase candidate : shrinkCandidates(c)) {
                String input = render(candidate).input;
                boolean smaller = input.length() < before.length()
                        || (input.length() == before.length() && weight(candidate) < weight(c));
                if (smaller && checkOptionCase(candidate) != null) {
                    c = candidate;
                    before = input;
                    progress = true;
                    break;
                }
            }
        }
        return c;
    }

    @Test
    void p1_options_round_trip_in_any_order_and_spelling() {
        Random r = new Random(SEED);
        List<String> failures = new ArrayList<>();
        java.util.Set<String> distinct = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES && distinct.size() < 12; i++) {
            OptionCase c = randomOptionCase(r);
            if (checkOptionCase(c) != null) {
                OptionCase min = shrink(c);
                Rendered rendered = render(min);
                if (distinct.add(rendered.input)) {
                    failures.add("case " + i + " (anywhere=" + min.anywhere + ")\n  input:    " + rendered.input
                            + "\n  expected: " + rendered.expected + "\n  actual:   " + checkOptionCase(min));
                }
            }
        }
        assertThat(failures).as("P1 minimal counterexamples").isEmpty();
    }

    // ---------------------------------------------------------------- P2: suggestions of option tokens

    private static final TestCommandSource ALL = new TestCommandSource() {
        @Override
        public boolean hasPermission(String permission) {
            return true;
        }
    };

    private static com.mojang.brigadier.suggestion.Suggestions suggestions(TestCommandRunner runner, String input) {
        com.mojang.brigadier.CommandDispatcher<TestCommandSource> dispatcher = new com.mojang.brigadier.CommandDispatcher<>(runner.root());
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, ALL))
                         .join();
    }

    private static boolean parsesCompletely(TestCommandRunner runner, String input) {
        com.mojang.brigadier.CommandDispatcher<TestCommandSource> dispatcher = new com.mojang.brigadier.CommandDispatcher<>(runner.root());
        com.mojang.brigadier.ParseResults<TestCommandSource> parse = dispatcher.parse(input, ALL);
        return !parse.getReader()
                     .canRead() && parse.getExceptions()
                                        .isEmpty();
    }

    @Test
    void p2_every_typed_option_token_is_suggested_and_every_suggestion_parses() {
        Random r = new Random(SEED + 2);
        java.util.Set<String> failures = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES / 3 && failures.size() < 12; i++) {
            OptionCase c = randomOptionCase(r);
            if (checkOptionCase(c) != null) {
                continue;
            }
            Rendered rendered = render(c);
            TestCommandRunner runner = optionRunner(c);
            for (int[] token : rendered.optionTokens) {
                String text = rendered.input.substring(token[0], token[1]);
                // Brigadier's SuggestionsBuilder leaves out a suggestion equal to the typed text, so the cut stays
                // inside the token.
                for (int cut : new int[]{token[0] + 1, Math.max(token[0] + 1, token[1] - 1)}) {
                    String prefix = rendered.input.substring(0, cut);
                    com.mojang.brigadier.suggestion.Suggestions suggestions = suggestions(runner, prefix);
                    List<String> texts = new ArrayList<>();
                    suggestions.getList()
                               .forEach(x -> texts.add(x.getText()));
                    if (!texts.contains(text)) {
                        failures.add("completeness: '" + prefix + "' does not suggest '" + text + "' but got "
                                + texts);
                    }
                    for (com.mojang.brigadier.suggestion.Suggestion suggestion : suggestions.getList()) {
                        String completed = suggestion.apply(prefix);
                        if (!parsesCompletely(runner, completed)) {
                            failures.add("soundness: '" + prefix + "' suggests '" + suggestion.getText()
                                    + "', and '" + completed + "' does not parse");
                        }
                    }
                }
            }
        }
        assertThat(failures).as("P2 counterexamples").isEmpty();
    }

    // ---------------------------------------------------------------- P1b: options around an argument child

    private static TestCommandRunner childRunner(List<String> sink) {
        TestCommand config = new TestCommand("cfg") {{
            option(Options.flag("alpha", 'a'));
            option(Options.flag("beta", 'b'));
            option(Options.count("count", 'c', 2));
            argument(new Quoted("key")).child(new TestCommand("set") {{
                addAliases("put");
                CommandOption<Boolean, TestCommandContext> x = option(Options.flag("xray", 'x'));
                CommandOption<Boolean, TestCommandContext> y = option(Options.flag("yank", 'y'));
                CommandOption<Integer, TestCommandContext> n = option(Options.integer("num", 'n', 0));
                argument(new Quoted("value").optional("none")).execute((ctx, value) -> sink.add("key="
                        + ctx.getArgument("key") + " value=" + value + " x=" + ctx.getOption(x) + " y="
                        + ctx.getOption(y) + " n=" + ctx.getOption(n) + " inputs=" + ctx.getInputs()));
            }});
        }};
        return withRedirects(new TestCommandRunner(config));
    }

    @Test
    void p1b_options_of_a_command_and_of_its_argument_child_in_any_order() {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = childRunner(sink);
        Random r = new Random(SEED + 1);
        java.util.Set<String> failures = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES && failures.size() < 12; i++) {
            List<String> parent = new ArrayList<>();
            if (r.nextBoolean()) {
                parent.add(r.nextBoolean() ? "-a" : "--alpha");
            }
            if (r.nextBoolean()) {
                parent.add(r.nextBoolean() ? "-b" : "--beta");
            }
            int counts = r.nextInt(3);
            for (int k = 0; k < counts; k++) {
                parent.add(r.nextBoolean() ? "-c" : "--count");
            }
            Collections.shuffle(parent, r);
            boolean x = r.nextBoolean();
            boolean y = r.nextBoolean();
            Integer n = r.nextBoolean() ? r.nextInt(50) : null;
            List<List<String>> child = new ArrayList<>();
            if (x) {
                child.add(List.of(r.nextBoolean() ? "-x" : "--xray"));
            }
            if (y) {
                child.add(List.of(r.nextBoolean() ? "-y" : "--yank"));
            }
            if (x && y && r.nextBoolean()) {
                child.clear();
                child.add(List.of(r.nextBoolean() ? "-xy" : "-yx"));
            }
            if (n != null) {
                child.add(List.of(r.nextBoolean() ? "-n" : "--num", n.toString()));
            }
            Collections.shuffle(child, r);
            String key = randomString(r);
            String keyToken = stringToken(key, r, true);
            String value = r.nextBoolean() ? randomString(r) : null;
            String valueToken = value == null ? null : stringToken(value, r, true);
            String label = r.nextBoolean() ? "set" : "put";
            String prefix = r.nextInt(3) == 0 ? "run " : "";
            StringBuilder input = new StringBuilder(prefix + "cfg");
            parent.forEach(t -> input.append(' ')
                                     .append(t));
            input.append(' ')
                 .append(keyToken)
                 .append(' ')
                 .append(label);
            child.forEach(atom -> atom.forEach(t -> input.append(' ')
                                                         .append(t)));
            if (valueToken != null) {
                input.append(' ')
                     .append(valueToken);
            }
            List<String> inputs = new ArrayList<>(List.of("cfg", keyToken, label));
            if (valueToken != null) {
                inputs.add(valueToken);
            }
            String expected = "key=" + key + " value=" + (value == null ? "none" : value) + " x=" + x + " y=" + y
                    + " n=" + (n == null ? 0 : n) + " inputs=" + inputs;
            sink.clear();
            String actual;
            try {
                runner.execute(input.toString());
                actual = String.join("|", sink);
            } catch (CommandSyntaxException e) {
                actual = "syntax error: " + e.getMessage();
            } catch (RuntimeException e) {
                actual = "exception: " + e;
            }
            if (!actual.equals(expected)) {
                failures.add("input: " + input + "\n  expected: " + expected + "\n  actual:   " + actual);
            }
        }
        assertThat(failures).as("P1b counterexamples").isEmpty();
    }

    // ---------------------------------------------------------------- P3: variadic values

    @Test
    void p3_variadic_values_round_trip_with_options_before_them() {
        List<String> sink = new ArrayList<>();
        TestCommandRunner[] runners = new TestCommandRunner[2];
        for (int anywhere = 0; anywhere < 2; anywhere++) {
            boolean optionsAnywhere = anywhere == 1;
            runners[anywhere] = withRedirects(new TestCommandRunner(new TestCommand("give") {{
                if (optionsAnywhere) {
                    optionsAnywhere();
                }
                CommandOption<Boolean, TestCommandContext> f = option(Options.flag("force", 'f'));
                CommandOption<Boolean, TestCommandContext> q = option(Options.flag("quiet", 'q'));
                argument(new Word("item"), new Quoted("names").transformer(x -> "<" + x + ">")
                                                              .variadic(4)).execute((ctx, item, names) -> sink.add("item="
                        + item + " names=" + names + " f=" + ctx.getOption(f) + " q=" + ctx.getOption(q)
                        + " input=" + ctx.getInput("names") + " inputs=" + ctx.getInputs()));
            }}));
        }
        Random r = new Random(SEED + 3);
        java.util.Set<String> failures = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES && failures.size() < 12; i++) {
            boolean anywhere = r.nextBoolean();
            List<String> options = new ArrayList<>();
            boolean f = r.nextBoolean();
            boolean q = r.nextBoolean();
            if (f) {
                options.add(r.nextBoolean() ? "-f" : "--force");
            }
            if (q) {
                options.add(r.nextBoolean() ? "-q" : "--quiet");
            }
            Collections.shuffle(options, r);
            int count = 1 + r.nextInt(5);
            List<String> names = new ArrayList<>();
            List<String> tokens = new ArrayList<>();
            for (int k = 0; k < count; k++) {
                String name = randomString(r);
                names.add("<" + name + ">");
                tokens.add(stringToken(name, r, true));
            }
            String item = WORDS[r.nextInt(WORDS.length)];
            List<String> head = new ArrayList<>();
            List<String> middle = new ArrayList<>();
            for (String option : options) {
                (anywhere && r.nextBoolean() ? middle : head).add(option);
            }
            String input = (r.nextInt(3) == 0 ? "run " : "") + "give" + (head.isEmpty() ? "" : " " + String.join(" ",
                                                                                                                  head))
                    + " " + item + (middle.isEmpty() ? "" : " " + String.join(" ", middle)) + " "
                    + String.join(" ", tokens);
            String expected = count > 4 ? "rejected"
                    : "item=" + item + " names=" + names + " f=" + f + " q=" + q + " input=" + String.join(" ", tokens)
                            + " inputs=[give, " + item + ", " + String.join(" ", tokens) + "]";
            sink.clear();
            String actual;
            try {
                runners[anywhere ? 1 : 0].execute(input);
                actual = sink.isEmpty() ? "no executor" : String.join("|", sink);
            } catch (CommandSyntaxException e) {
                actual = count > 4 ? "rejected" : "syntax error: " + e.getMessage();
            } catch (RuntimeException e) {
                actual = "exception: " + e;
            }
            if (!actual.equals(expected)) {
                failures.add("input: " + input + "\n  expected: " + expected + "\n  actual:   " + actual);
            }
        }
        assertThat(failures).as("P3 counterexamples").isEmpty();
    }

    // ---------------------------------------------------------------- P4: duration parsing against an oracle

    static final class DurationArg extends dev.marulabs.commandlib.argument.CommonDurationArgument<TestCommandContext, DurationArg> {
        DurationArg(String name) {
            super(name);
        }
    }

    // Independent reading of the CommonDurationArgument Javadoc.
    private static java.time.Duration durationOracle(String s) {
        String units = "dhmst";
        long[] nanos = {86_400_000_000_000L, 3_600_000_000_000L, 60_000_000_000L, 1_000_000_000L, 50_000_000L};
        java.math.BigInteger total = java.math.BigInteger.ZERO;
        int last = -1;
        int i = 0;
        if (s.isEmpty()) {
            return null;
        }
        while (i < s.length()) {
            int start = i;
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
            }
            if (i == start || i == s.length()) {
                return null;
            }
            int unit = units.indexOf(s.charAt(i));
            if (unit <= last) {
                return null;
            }
            last = unit;
            total = total.add(new java.math.BigInteger(s.substring(start, i)).multiply(java.math.BigInteger.valueOf(nanos[unit])));
            i++;
        }
        java.math.BigInteger[] parts = total.divideAndRemainder(java.math.BigInteger.valueOf(1_000_000_000L));
        if (parts[0].bitLength() > 63) {
            return null;
        }
        return java.time.Duration.ofSeconds(parts[0].longValueExact(), parts[1].longValueExact());
    }

    @Test
    void p4_duration_matches_its_documented_grammar() {
        List<java.time.Duration> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("wait") {{
            argument(new DurationArg("time")).execute((ctx, time) -> sink.add(time));
        }});
        Random r = new Random(SEED + 4);
        String alphabet = "0123456789dhmstx";
        java.util.Set<String> failures = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES * 3 && failures.size() < 12; i++) {
            StringBuilder sb = new StringBuilder();
            int length = 1 + r.nextInt(12);
            for (int k = 0; k < length; k++) {
                sb.append(r.nextInt(3) == 0 ? alphabet.charAt(10 + r.nextInt(6)) : alphabet.charAt(r.nextInt(10)));
            }
            if (r.nextInt(10) == 0) {
                sb.insert(0, "9".repeat(15 + r.nextInt(6)));
            }
            String text = sb.toString();
            java.time.Duration expected = durationOracle(text);
            sink.clear();
            java.time.Duration actual;
            try {
                runner.execute("wait " + text);
                actual = sink.isEmpty() ? null : sink.get(0);
            } catch (CommandSyntaxException e) {
                actual = null;
            }
            if (!java.util.Objects.equals(expected, actual)) {
                failures.add(text + ": expected " + expected + ", actual " + actual);
            }
            if (actual != null) {
                String prefix = "wait " + text.replaceAll("[a-z]+$", "");
                if (Character.isDigit(prefix.charAt(prefix.length() - 1))) {
                    for (com.mojang.brigadier.suggestion.Suggestion s : suggestions(runner, prefix).getList()) {
                        if (durationOracle(s.getText()) == null) {
                            failures.add("'" + prefix + "' suggests '" + s.getText() + "', which does not parse");
                        }
                    }
                }
            }
        }
        assertThat(failures).as("P4 counterexamples").isEmpty();
    }

    // ---------------------------------------------------------------- targeted hypotheses

    @Test
    void h1_the_value_help_of_a_first_argument_depends_on_the_options_before_it() throws Exception {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("msg") {{
            CommandOption<Boolean, TestCommandContext> f = option(Options.flag("force", 'f'));
            argument(new Word("target"), new Phrase("text")).execute((ctx, target, text) -> sink.add("target=" + target + " text=" + text + " f="
                    + ctx.getOption(f)));
        }});
        List<String> observed = new ArrayList<>();
        for (String input : List.of("msg bob hi", "msg -f bob hi", "msg -f help hi", "msg help hi", "msg help")) {
            sink.clear();
            String outcome;
            try {
                TestCommandContext ctx = runner.execute(input);
                outcome = sink.isEmpty() ? "help shown: " + (ctx == null ? "" : ctx.messages()) : sink.get(0);
            } catch (CommandSyntaxException e) {
                outcome = "syntax error: " + e.getMessage();
            }
            observed.add(input + " -> " + outcome);
        }
        assertThat(observed).as("H1 observed")
                            .containsExactly("msg bob hi -> target=bob text=hi f=false",
                                             "msg -f bob hi -> target=bob text=hi f=true",
                                             "msg -f help hi -> target=help text=hi f=true",
                                             "msg help hi -> target=help text=hi f=false",
                                             "msg help -> help shown or target=help");
    }

    @Test
    void h2_options_after_the_first_argument_of_a_branch_that_shares_it_with_a_variadic_branch() throws Exception {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("tp") {{
            optionsAnywhere();
            CommandOption<Boolean, TestCommandContext> f = option(Options.flag("force", 'f'));
            argument(new Word("who").variadic(3)).execute((ctx, who) -> sink.add("who=" + who + " f="
                    + ctx.getOption(f)));
            argument(new Word("who"), new Int("level")).execute((ctx, who, level) -> sink.add("who=" + who
                    + " level=" + level + " f=" + ctx.getOption(f)));
        }});
        List<String> observed = new ArrayList<>();
        for (String input : List.of("tp bob 5 -f", "tp bob -f 5", "tp -f bob 5")) {
            sink.clear();
            try {
                runner.execute(input);
                observed.add(input + " -> " + sink);
            } catch (CommandSyntaxException e) {
                observed.add(input + " -> syntax error: " + e.getMessage());
            }
        }
        assertThat(observed).as("H2 observed")
                            .containsExactly("tp bob 5 -f -> [who=bob level=5 f=true]",
                                             "tp bob -f 5 -> [who=bob level=5 f=true]",
                                             "tp -f bob 5 -> [who=bob level=5 f=true]");
    }

    @Test
    void h3_a_top_level_name_with_a_colon_and_redirected_options() throws Exception {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("ns:scan") {{
            CommandOption<Boolean, TestCommandContext> a = option(Options.flag("alpha", 'a'));
            CommandOption<Boolean, TestCommandContext> b = option(Options.flag("beta", 'b'));
            argument(new Word("target")).execute((ctx, target) -> sink.add(target + " a=" + ctx.getOption(a) + " b="
                    + ctx.getOption(b)));
        }});
        List<String> observed = new ArrayList<>();
        for (String input : List.of("ns:scan -a -b x", "ns:scan -b -a x")) {
            sink.clear();
            try {
                runner.execute(input);
                observed.add(input + " -> " + sink);
            } catch (CommandSyntaxException | RuntimeException e) {
                observed.add(input + " -> " + e);
            }
        }
        assertThat(observed).as("H3 observed")
                            .containsExactly("ns:scan -a -b x -> [x a=true b=true]",
                                             "ns:scan -b -a x -> [x a=true b=true]");
    }

    @Test
    void h4_inputs_of_a_literal_named_like_an_argument_before_it() throws Exception {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new Word("key")).child(new TestCommand("key") {{
                execute(ctx -> sink.add("key=" + ctx.getArgument("key") + " input=" + ctx.getInput("key")
                        + " inputs=" + ctx.getInputs()));
            }});
        }});
        runner.execute("config motd key");
        assertThat(sink).as("H4 observed")
                        .containsExactly("key=motd input=motd inputs=[config, motd, key]");
    }

    // ---------------------------------------------------------------- P5: quotable suggestions of object arguments

    static final class Obj extends dev.marulabs.commandlib.argument.CommonObjectArgument<String, TestCommandContext, Obj> {
        Obj(String name, java.util.function.Supplier<java.util.Map<String, ? extends String>> map) {
            super(name, map);
        }
    }

    private static String unescape(String token) {
        if (token.isEmpty() || !StringReader.isQuotedStringStart(token.charAt(0))) {
            return token;
        }
        try {
            return new StringReader(token).readQuotedString();
        } catch (CommandSyntaxException e) {
            return "<unreadable " + token + ">";
        }
    }

    @Test
    void p5_object_suggestions_parse_back_to_their_key() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            option(Options.flag("force", 'f'));
            argument(new Obj("obj", () -> map)).execute((ctx, value) -> sink.add(value));
        }});
        Random r = new Random(SEED + 5);
        java.util.Set<String> failures = new java.util.LinkedHashSet<>();
        for (int i = 0; i < CASES && failures.size() < 12; i++) {
            map.clear();
            int size = 1 + r.nextInt(5);
            for (int k = 0; k < size; k++) {
                String name = randomString(r);
                if (!name.isEmpty()) {
                    map.put(name, name);
                }
            }
            if (map.isEmpty()) {
                continue;
            }
            List<String> names = new ArrayList<>(map.keySet());
            String name = names.get(r.nextInt(names.size()));
            int from = r.nextInt(name.length() + 1);
            String typed = name.substring(from, from + r.nextInt(name.length() - from + 1));
            String typedToken = typed.isEmpty() || unquotable(typed) ? typed
                    : "\"" + typed.replace("\\", "\\\\")
                                  .replace("\"", "\\\"");
            String prefix = "pick " + typedToken;
            List<String> texts = new ArrayList<>();
            suggestions(runner, prefix).getList()
                                       .forEach(x -> texts.add(x.getText()));
            for (String candidate : names) {
                if (candidate.toLowerCase(java.util.Locale.ROOT)
                                                                            .contains(typed.toLowerCase(java.util.Locale.ROOT))
                        && !texts.contains(StringArgumentType.escapeIfRequired(candidate))
                        && !StringArgumentType.escapeIfRequired(candidate)
                                              .equals(typedToken)) {
                    failures.add("completeness: '" + prefix + "' does not suggest " + candidate + " of " + names
                            + ", got " + texts);
                }
            }
            for (String text : texts) {
                String key = unescape(text);
                if (!map.containsKey(key)) {
                    continue;
                }
                sink.clear();
                String outcome;
                try {
                    runner.execute("pick " + text);
                    outcome = sink.isEmpty() ? "no executor" : sink.get(0);
                } catch (CommandSyntaxException e) {
                    outcome = "syntax error: " + e.getMessage();
                }
                if (!outcome.equals(key)) {
                    failures.add("soundness: suggestion '" + text + "' for key '" + key + "' runs as " + outcome);
                }
            }
        }
        assertThat(failures).as("P5 counterexamples").isEmpty();
    }

    // Paper 1.21+ registers <plugin>:<label> and a <label> literal that redirects to it (modelled, not Paper itself).
    @Test
    void h6_inputs_under_a_paper_style_label_redirect_depend_on_how_options_are_spelled() throws Exception {
        List<String> sink = new ArrayList<>();
        TestCommand game = new TestCommand("game") {{
            option(Options.flag("alpha", 'a'));
            option(Options.flag("beta", 'b'));
            argument(new Word("arena")).execute((ctx, arena) -> sink.add("inputs=" + ctx.getInputs()));
        }};
        com.mojang.brigadier.tree.LiteralCommandNode<TestCommandSource> built = new CommandNodeCreator<>(List.of(game),
                                                                                                        "test.command",
                                                                                                        TestCommandRunner.INLINE).build()
                                                                                                                                 .get(0);
        com.mojang.brigadier.CommandDispatcher<TestCommandSource> dispatcher = new com.mojang.brigadier.CommandDispatcher<>();
        com.mojang.brigadier.tree.LiteralCommandNode<TestCommandSource> namespaced = LiteralArgumentBuilder.<TestCommandSource>literal("plugin:game")
                                                                                                           .requires(built.getRequirement())
                                                                                                           .executes(built.getCommand())
                                                                                                           .build();
        built.getChildren()
             .forEach(namespaced::addChild);
        dispatcher.getRoot()
                  .addChild(namespaced);
        dispatcher.getRoot()
                  .addChild(LiteralArgumentBuilder.<TestCommandSource>literal("game")
                                                  .requires(built.getRequirement())
                                                  .executes(built.getCommand())
                                                  .redirect(namespaced)
                                                  .build());
        List<String> observed = new ArrayList<>();
        for (String input : List.of("game x", "game --alpha --beta x", "game -b -a x", "plugin:game x")) {
            sink.clear();
            dispatcher.execute(input, ALL);
            observed.add(input + " -> " + sink);
        }
        assertThat(observed).as("H6 observed")
                            .containsExactly("game x -> [inputs=[game, x]]",
                                             "game --alpha --beta x -> [inputs=[game, x]]",
                                             "game -b -a x -> [inputs=[game, x]]",
                                             "plugin:game x -> [inputs=[plugin:game, x]]");
    }

    // A constant named in Japanese, as Java identifiers allow.
    @SuppressWarnings("UnicodeInCode")
    enum Speed {
        NORMAL,
        高速
    }

    static final class SpeedArg extends dev.marulabs.commandlib.argument.CommonEnumArgument<Speed, TestCommandContext, SpeedArg> {
        SpeedArg(String name) {
            super(name, Speed.class);
        }
    }

    @Test
    void h7_every_suggested_enum_constant_can_be_typed() {
        List<String> sink = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("speed") {{
            argument(new SpeedArg("mode")).execute((ctx, mode) -> sink.add(mode.name()));
        }});
        List<String> observed = new ArrayList<>();
        for (com.mojang.brigadier.suggestion.Suggestion suggestion : suggestions(runner, "speed ").getList()) {
            sink.clear();
            String outcome;
            try {
                runner.execute("speed " + suggestion.getText());
                outcome = sink.isEmpty() ? "no executor" : sink.get(0);
            } catch (CommandSyntaxException e) {
                outcome = "syntax error: " + e.getMessage();
            }
            observed.add(suggestion.getText() + " -> " + outcome);
        }
        assertThat(observed).as("H7 observed")
                            .containsExactlyInAnyOrder("normal -> NORMAL", "高速 -> 高速");
    }
}
