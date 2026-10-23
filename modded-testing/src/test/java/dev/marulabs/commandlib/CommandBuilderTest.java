package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBuilderTest {
    private static final String PREFIX = "mymod.command";
    private static final List<String> INPUTS = List.of("heal",
                                                       "heal Steve 5",
                                                       "heal -s Steve 5",
                                                       "heal all",
                                                       "heal help");

    @Test
    void builder_makes_the_tree_of_the_fluent_style() {
        List<String> builder = observe(CommandBuilderTest::builderHeal);

        assertThat(builder).isEqualTo(observe(CommandBuilderTest::fluentHeal));
        assertThat(builder.get(0)).contains("heal (aliases: h) [mymod.command.heal, OP]", "--silent, -s", "all");
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
                tester.withPlayer(sender);
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
            require(Command.player()).execute((ctx, player) -> ctx.sendMessage("Healed " + player.getName()
                                                                                                 .getString()));
            argument(new PlayerArgument("target"), new IntegerArgument("amount")).description("Heal by an amount")
                                                                                 .permissionDescription("Heal others")
                                                                                 .execute((ctx, target, amount) -> {
                                                                                     ctx.sendMessage("Healed "
                                                                                             + target.getName()
                                                                                                     .getString()
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
                               branch -> branch.execute((ctx, player) -> ctx.sendMessage("Healed " + player.getName()
                                                                                                           .getString())))
                      .argument(new PlayerArgument("target"),
                                new IntegerArgument("amount"),
                                branch -> branch.description("Heal by an amount")
                                                .permissionDescription("Heal others")
                                                .execute((ctx, target, amount) -> {
                                                    ctx.sendMessage("Healed " + target.getName()
                                                                                      .getString()
                                                            + " by " + amount
                                                            + (ctx.getOption(silent) ? " silently" : ""));
                                                }))
                      .addChild(Command.builder("all")
                                       .description("Heal everyone")
                                       .permission(DefaultPermission.ALL)
                                       .execute(ctx -> ctx.sendMessage("Healed everyone")))
                      .build();
    }
}
