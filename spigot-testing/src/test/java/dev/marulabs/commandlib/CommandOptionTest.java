package dev.marulabs.commandlib;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandOptionTest {
    @Test
    void command_tester_executes_command_with_options() {
        try (CommandTester tester = new CommandTester(new Command("scan") {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force) + ":" + ctx.getOption(limit));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("scan -f -n 20 Alex", sender);
            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:true:20");
        }
    }

    @Test
    void command_tester_executes_repeatable_required_and_exclusive_options() {
        try (CommandTester tester = new CommandTester(new Command("tag") {{
            CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                             .repeatable(3)
                                                                             .required());
            CommandOption<Boolean, CommandContext> json = option(Options.flag("json", 'j')
                                                                        .aliases("machine"));
            CommandOption<Boolean, CommandContext> yaml = option(Options.flag("yaml", 'y'));
            mutuallyExclusive(json, yaml);
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(verbose) + ":" + ctx.getOption(tags) + ":"
                        + ctx.getOption(json));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("tag -vv -t red --machine --tag blue Alex", sender);
            tester.execute("tag Alex", sender);
            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:2:[red, blue]:true",
                                                                     "Missing required option --tag.");
            assertThat(tester.suggestions("tag -j -", sender)
                             .join()
                             .getList()).extracting(Suggestion::getText)
                                        .doesNotContain("-y", "--yaml")
                                        .contains("-t", "--tag");
        }
    }

    @Test
    void an_option_takes_a_player_argument() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            CommandOption<Player, CommandContext> target = option(Options.of("target",
                                                                             't',
                                                                             new PlayerArgument("target").optional(playerRequirer())));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new IntegerArgument("amount")).execute((amount, ctx) -> {
                ctx.sendMessage(ctx.getOption(target)
                                   .getName()
                        + ":" + amount + ":" + ctx.getOption(force));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("heal -t Steve -f 5", admin);
            tester.execute("heal -f --target Steve 6", admin);
            tester.execute("heal 7", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Steve:5:true", "Steve:6:true", "Admin:7:false");
    }

    @Test
    void options_follow_the_arguments_when_the_command_allows_it() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("heal") {{
            optionsAnywhere();
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> times = option(Options.integer("times", 't', 1));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).execute((target, amount, ctx) -> {
                ctx.sendMessage(target.getName() + ":" + amount + ":" + ctx.getOption(force) + ":"
                        + ctx.getOption(times));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("heal Steve -f 5", admin);
            tester.execute("heal Steve 5 -t 3 -f", admin);
            tester.execute("heal -t 2 Steve -f 5", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Steve:5:true:1", "Steve:5:true:3", "Steve:5:true:2");
    }

    @Test
    void an_option_with_a_permission_is_hidden_and_rejected_without_it() {
        try (CommandTester tester = new CommandTester(() -> new Command("ban") {{
            CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's')
                                                                          .permission(DefaultPermission.OP));
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            argument(new StringArgument("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(silent) + ":" + ctx.getOption(force));
            });
        }}, "test.command")) {
            FakeSender admin = FakeSender.player("Admin");
            FakeSender mod = FakeSender.player("Mod")
                                       .denyPermissions("test.command.ban.option.silent");

            tester.execute("ban -fs Steve", admin);
            tester.execute("ban -f Steve", mod);
            assertThatThrownBy(() -> tester.execute("ban -f -s Steve",
                                                    mod)).hasMessageContaining("Command syntax error");
            assertThatThrownBy(() -> tester.execute("ban -fs Steve", mod)).hasMessageContaining("Command syntax error");
            assertThat(admin.getSentMessageTexts()).containsExactly("Steve:true:true");
            assertThat(mod.getSentMessageTexts()).containsExactly("Steve:false:true");
            assertThat(tester.commandTree()).contains("--silent, -s [test.command.ban.option.silent, OP]");
        }
    }

    @Test
    void a_command_is_shown_and_run_only_while_its_predicate_holds() {
        java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean();
        try (CommandTester tester = new CommandTester(() -> new Command("game") {{
            addChildren(new Command("start") {{
                visibleWhen(ctx -> !running.get() && ctx.getSender()
                                                        .getName()
                                                        .equals("Steve"));
                execute(ctx -> running.set(true));
            }});
        }}, "test.command")) {
            FakeSender steve = FakeSender.player("Steve");
            FakeSender alex = FakeSender.player("Alex");

            assertThatThrownBy(() -> tester.execute("game start", alex)).hasMessageContaining("Command syntax error");
            tester.execute("game start", steve);
            assertThat(running).isTrue();
            assertThatThrownBy(() -> tester.execute("game start", steve)).hasMessageContaining("Command syntax error");
        }
    }

    @Test
    void help_message_includes_options() {
        try (CommandTester tester = new CommandTester(new Command("scan") {{
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));

            argument(new StringArgument("target")).execute((target, ctx) -> {
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");
            tester.execute("scan", sender);
            assertThat(sender.getSentMessageTexts()).anyMatch(x -> x.contains("Usage:"))
                                                    .anyMatch(x -> x.contains("/scan") && x.contains("options")
                                                            && x.contains("target"))
                                                    .anyMatch(x -> x.contains("Options:"))
                                                    .anyMatch(x -> x.contains("-f") && x.contains("--force")
                                                            && x.contains("Force execution"))
                                                    .anyMatch(x -> x.contains("-n") && x.contains("--limit")
                                                            && x.contains("Maximum count"));
        }
    }
}
