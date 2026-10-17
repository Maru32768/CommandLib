package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LightweightArgumentExecutionTest {
    @Test
    void primitiveAndStringArgumentsExecute() {
        CommandTester tester = new CommandTester(new Command("args") {{
            argument(new BooleanArgument("bool"),
                     new IntegerArgument("int"),
                     new LongArgument("long"),
                     new FloatArgument("float"),
                     new DoubleArgument("double"),
                     new StringArgument("word")).execute((bool, integer, longValue, floatValue, doubleValue, word, ctx) -> {
                         ctx.sendMessage(bool + ":" + integer + ":" + longValue + ":" + floatValue + ":"
                                 + doubleValue + ":" + word);
                     });
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args true 3 4 1.5 2.5 hello", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("true:3:4:1.5:2.5:hello");
    }

    @Test
    void objectNameableEnumLiteralAndUnparsedArgumentsExecute() {
        CommandTester tester = new CommandTester(new Command("args") {{
            argument(new ObjectArgument<>("object", Map.of("alpha", "mapped")),
                     new NameableObjectArgument<>("nameable", List.of(new SampleNameable("beta"))),
                     new EnumArgument<>("mode", SampleMode.class),
                     new LiteralArgument("literal", List.of("literal")),
                     new UnparsedArgument("tail")).execute((object, nameable, mode, literal, tail, ctx) -> {
                         ctx.sendMessage(object + ":" + nameable.tabCompleteName() + ":" + mode + ":" + literal
                                 + ":" + tail);
                     });
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args alpha beta ALPHA literal raw@p", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("mapped:beta:ALPHA:literal:raw@p");
    }

    @Test
    void bukkitStaticBackedArgumentsExecuteWithMocks() {
        try (MockedStatic<Bukkit> ignored = mockBukkitStatics()) {
            CommandTester tester = new CommandTester(new Command("args") {{
                argument(new OfflinePlayerArgument("offline"),
                         new UUIDArgument("uuid"),
                         new TeamArgument("team")).execute((offline, uuid, team, ctx) -> {
                             ctx.sendMessage(offline.getName() + ":" + uuid + ":" + team.getName());
                         });
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("args Alex 00000000-0000-0000-0000-000000000001 red", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Alex:00000000-0000-0000-0000-000000000001:red");
        }
    }

    @Test
    void paperSelectorArgumentsExecuteWithFakes() {
        Player steve = Mockito.mock(Player.class);
        Mockito.when(steve.getName())
               .thenReturn("Steve");
        Entity zombie = Mockito.mock(Entity.class);
        Mockito.when(zombie.getName())
               .thenReturn("Zombie");
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new PlayerArgument("player"),
                     new PlayersArgument("players"),
                     new EntityArgument("entity"),
                     new EntitiesArgument("entities")).execute((player, players, entity, entities, ctx) -> {
                         ctx.sendMessage(player.getName() + ":" + players.size() + ":" + entity.getName() + ":"
                                 + entities.size());
                     });
        }}, "test.command").withFakePlayer(steve)
                           .withFakeEntity("Zombie", zombie);
        FakeSender sender = FakeSender.player("Sender");

        tester.execute("args Steve @a Zombie @a", sender);

        // @a selects the players only, also for an entities argument.
        assertThat(sender.getSentMessageTexts()).containsExactly("Steve:1:Zombie:1");
    }

    @Test
    void paperRegistryAndValueArgumentsExecuteWithFakes() {
        BlockData blockData = Mockito.mock(BlockData.class);
        ItemStack itemStack = Mockito.mock(ItemStack.class);
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new BlockDataArgument("block"),
                     new ItemStackArgument("item"),
                     new ParticleArgument("particle")).execute((block, item, particle, ctx) -> {
                         ctx.sendMessage((block == blockData) + ":" + (item == itemStack) + ":" + particle.name());
                     });
        }}, "test.command").withFakeBlockData(blockData)
                           .withFakeItemStack(itemStack)
                           .withFakeRegistryValue(RegistryKey.PARTICLE_TYPE, "minecraft:poof", Particle.POOF);
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args minecraft:stone minecraft:stone minecraft:poof", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("true:true:POOF");
    }

    @Test
    void registryArgumentsWithoutFakeValuesRejectEveryKey() {
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new ParticleArgument("particle")).execute((particle, ctx) -> ctx.sendMessage(particle.name()));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        assertThatThrownBy(() -> tester.execute("args minecraft:poof",
                                                sender)).hasCauseInstanceOf(com.mojang.brigadier.exceptions.CommandSyntaxException.class);
        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void gameModeArgumentExecutes() {
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new GameModeArgument("mode")).execute((mode, ctx) -> ctx.sendMessage(mode.name()));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args creative", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("CREATIVE");
        // Vanilla takes the lower-case names only.
        assertThatThrownBy(() -> tester.execute("args CREATIVE", sender)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void displaySlotAndColorArgumentsReadVanillaNames() {
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new ScoreboardDisplaySlotArgument("slot"),
                     new ChatColorArgument("color")).execute((slot, color, ctx) -> ctx.sendMessage(slot.name() + ":" + color));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args sidebar.team.red dark_red", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("SIDEBAR_TEAM_RED:dark_red");
    }

    @Test
    void colorArgumentRejectsResetAndFormats() {
        CommandTester tester = new CommandTester(() -> new Command("color") {{
            argument(new ChatColorArgument("color")).execute((color, ctx) -> ctx.sendMessage(color.toString()));
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        assertThatThrownBy(() -> tester.execute("color bold", sender)).hasMessageContaining("Invalid color 'bold'");
        tester.execute("color reset", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("reset is not a color");
    }

    @Test
    void paperLocationArgumentExecutes() {
        CommandTester tester = new CommandTester(() -> new Command("args") {{
            argument(new LocationArgument("location")).execute((location, ctx) -> {
                Location loc = location;
                ctx.sendMessage(loc.getX() + ":" + loc.getY() + ":" + loc.getZ());
            });
        }}, "test.command");
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("args 1.5 2.0 3.25", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("1.5:2.0:3.25");
    }

    private MockedStatic<Bukkit> mockBukkitStatics() {
        OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offlinePlayer.getName())
               .thenReturn("Alex");
        Mockito.when(offlinePlayer.getUniqueId())
               .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        Team team = Mockito.mock(Team.class);
        Mockito.when(team.getName())
               .thenReturn("red");
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getTeams())
               .thenReturn(Set.of(team));
        Mockito.when(scoreboard.getTeam("red"))
               .thenReturn(team);
        ScoreboardManager scoreboardManager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(scoreboardManager.getMainScoreboard())
               .thenReturn(scoreboard);

        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getOfflinePlayers)
              .thenReturn(new OfflinePlayer[]{offlinePlayer});
        bukkit.when(Bukkit::getScoreboardManager)
              .thenReturn(scoreboardManager);
        return bukkit;
    }

    private enum SampleMode {
        ALPHA
    }

    private static final class SampleNameable implements Nameable {
        private final String name;

        private SampleNameable(String name) {
            this.name = name;
        }

        @Override
        public String tabCompleteName() {
            return name;
        }
    }
}
