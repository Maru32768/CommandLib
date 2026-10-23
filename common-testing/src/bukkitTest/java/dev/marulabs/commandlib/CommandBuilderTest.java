package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.DoubleArgument;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBuilderTest {
    private static final String PREFIX = "myplugin.command";
    private static final List<String> INPUTS = List.of("heal",
                                                       "heal Steve 5",
                                                       "heal -s Steve 5",
                                                       "heal all",
                                                       "heal help");

    @Test
    void builder_makes_the_tree_of_the_fluent_style() {
        try (CommandTester builder = new CommandTester(CommandBuilderTest::builderHeal, PREFIX)) {
            builder.assertCommandTreeMatchesSnapshot("command-tree-builder-heal.snap");
        }
        // The testers mock NMS statically per thread, so they run one after the other.
        assertThat(observe(CommandBuilderTest::builderHeal)).isEqualTo(observe(CommandBuilderTest::fluentHeal));
    }

    @Test
    void builder_branch_of_eight_arguments_reads_each_value_typed() {
        DoubleArgument x1 = new DoubleArgument("x1");
        DoubleArgument y1 = new DoubleArgument("y1");
        DoubleArgument z1 = new DoubleArgument("z1");
        DoubleArgument x2 = new DoubleArgument("x2");
        DoubleArgument y2 = new DoubleArgument("y2");
        DoubleArgument z2 = new DoubleArgument("z2");
        IntegerArgument priority = new IntegerArgument("priority");
        StringArgument name = new StringArgument("name");
        try (CommandTester tester = new CommandTester(() -> Command.builder("region")
                                                                   .argument(List.of(x1,
                                                                                     y1,
                                                                                     z1,
                                                                                     x2,
                                                                                     y2,
                                                                                     z2,
                                                                                     priority,
                                                                                     name),
                                                                             branch -> branch.execute(ctx -> {
                                                                                 double volume = (ctx.getArgument(x2)
                                                                                         - ctx.getArgument(x1)) * (ctx.getArgument(y2)
                                                                                                 - ctx.getArgument(y1)) * (ctx.getArgument(z2)
                                                                                                         - ctx.getArgument(z1));
                                                                                 int level = ctx.getArgument(priority);
                                                                                 String label = ctx.getArgument(name);
                                                                                 ctx.sendMessage(label + " " + volume
                                                                                         + " " + level);
                                                                             }))
                                                                   .build(),
                                                      PREFIX)) {
            FakeSender sender = FakeSender.console();
            tester.execute("region 0 0 0 2 3 4 7 spawn", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("spawn 24.0 7");
        }
    }

    // The tree, the permissions and what every input sends and returns.
    private static List<String> observe(Supplier<Command> factory) {
        List<Command> built = new ArrayList<>();
        List<String> observed = new ArrayList<>();
        try (CommandTester tester = new CommandTester(capture(factory, built), PREFIX)) {
            observed.add(tester.commandTree());
            for (PermissionConfig permission : CommandTree.permissionConfigs(built, PREFIX)) {
                observed.add(permission.node() + " " + permission.defaultPermission() + " " + permission.description());
            }
            for (String input : INPUTS) {
                FakeSender sender = FakeSender.player("Steve");
                tester.withFakePlayer((Player) sender.asSender());
                int result = tester.execute(input, sender);
                assertThat(sender.getSentMessageTexts()).as(input)
                                                        .isNotEmpty();
                observed.add(input + " -> " + result + " " + sender.getSentMessageTexts());
            }
        }
        return observed;
    }

    private static Supplier<Command> capture(Supplier<Command> factory, List<Command> built) {
        return () -> {
            Command command = factory.get();
            built.add(command);
            return command;
        };
    }

    private static Command fluentHeal() {
        return new Command("heal") {{
            description("Heal a player");
            permission(DefaultPermission.OP, "Heal players");
            addAliases("h");
            CommandOption<Boolean, CommandContext> silent = option(Options.<CommandContext>flag("silent", 's')
                                                                          .description("Do not tell the target"));
            require(Command.player()).execute((ctx, player) -> ctx.sendMessage("Healed " + player.getName()));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).description("Heal by an amount")
                                                                                 .permissionDescription("Heal others")
                                                                                 .execute((ctx, target, amount) -> {
                                                                                     ctx.sendMessage("Healed "
                                                                                             + target.getName()
                                                                                             + " by " + amount
                                                                                             + (ctx.getOption(silent)
                                                                                                     ? " silently"
                                                                                                     : ""));
                                                                                 });
            addChildren(new Command("all") {{
                description("Heal everyone");
                permission(DefaultPermission.ALL);
                execute(ctx -> ctx.sendMessage("Healed everyone"));
            }});
        }};
    }

    private static Command builderHeal() {
        CommandOption<Boolean, CommandContext> silent = Options.<CommandContext>flag("silent", 's')
                                                               .description("Do not tell the target");
        return Command.builder("heal")
                      .description("Heal a player")
                      .permission(DefaultPermission.OP, "Heal players")
                      .addAliases("h")
                      .option(silent)
                      .require(Command.player(),
                               branch -> branch.execute((ctx, player) -> ctx.sendMessage("Healed " + player.getName())))
                      .argument(new PlayerArgument("target"),
                                new IntegerArgument("amount"),
                                branch -> branch.description("Heal by an amount")
                                                .permissionDescription("Heal others")
                                                .execute((ctx, target, amount) -> {
                                                    ctx.sendMessage("Healed " + target.getName() + " by " + amount
                                                            + (ctx.getOption(silent) ? " silently" : ""));
                                                }))
                      .addChild(Command.builder("all")
                                       .description("Heal everyone")
                                       .permission(DefaultPermission.ALL)
                                       .execute(ctx -> ctx.sendMessage("Healed everyone")))
                      .build();
    }
}
