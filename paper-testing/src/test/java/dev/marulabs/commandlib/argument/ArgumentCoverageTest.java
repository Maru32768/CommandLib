package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.CommonArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies Paper argument coverage at the command-testing layer.
 *
 * <p>Paper API-backed arguments are expected to be fully exercised by integration-test
 * targets on real Paper servers. This test keeps paper-testing honest by ensuring every
 * public argument can at least be wired into a command tree here.
 */
@SuppressWarnings({"rawtypes"})
class ArgumentCoverageTest {
    private static final String ARGUMENT_PACKAGE = "dev.marulabs.commandlib.argument";

    private static final Map<Class<?>, Supplier<? extends CommonArgument<?, ?, ?>>> ARGUMENT_FACTORIES = Map.ofEntries(Map.entry(AdvancementArgument.class,
                                                                                                                                 () -> new AdvancementArgument("value")),
                                                                                                                       Map.entry(AttributeArgument.class,
                                                                                                                                 () -> new AttributeArgument("value")),
                                                                                                                       Map.entry(BiomeArgument.class,
                                                                                                                                 () -> new BiomeArgument("value")),
                                                                                                                       Map.entry(BlockDataArgument.class,
                                                                                                                                 () -> new BlockDataArgument("value")),
                                                                                                                       Map.entry(BlockPositionArgument.class,
                                                                                                                                 () -> new BlockPositionArgument("value")),
                                                                                                                       Map.entry(BlockPredicateArgument.class,
                                                                                                                                 () -> new BlockPredicateArgument("value")),
                                                                                                                       Map.entry(ChatColorArgument.class,
                                                                                                                                 () -> new ChatColorArgument("value")),
                                                                                                                       Map.entry(ComponentArgument.class,
                                                                                                                                 () -> new ComponentArgument("value")),
                                                                                                                       Map.entry(GameModeArgument.class,
                                                                                                                                 () -> new GameModeArgument("value")),
                                                                                                                       Map.entry(BooleanArgument.class,
                                                                                                                                 () -> new BooleanArgument("value")),
                                                                                                                       Map.entry(DoubleArgument.class,
                                                                                                                                 () -> new DoubleArgument("value")),
                                                                                                                       Map.entry(DoubleRangeArgument.class,
                                                                                                                                 () -> new DoubleRangeArgument("value")),
                                                                                                                       Map.entry(DurationArgument.class,
                                                                                                                                 () -> new DurationArgument("value")),
                                                                                                                       Map.entry(EnchantmentArgument.class,
                                                                                                                                 () -> new EnchantmentArgument("value")),
                                                                                                                       Map.entry(EntitiesArgument.class,
                                                                                                                                 () -> new EntitiesArgument("value")),
                                                                                                                       Map.entry(EntityArgument.class,
                                                                                                                                 () -> new EntityArgument("value")),
                                                                                                                       Map.entry(EntityTypeArgument.class,
                                                                                                                                 () -> new EntityTypeArgument("value")),
                                                                                                                       Map.entry(EnumArgument.class,
                                                                                                                                 () -> new EnumArgument<>("value",
                                                                                                                                                          SampleMode.class)),
                                                                                                                       Map.entry(FloatArgument.class,
                                                                                                                                 () -> new FloatArgument("value")),
                                                                                                                       Map.entry(IntegerArgument.class,
                                                                                                                                 () -> new IntegerArgument("value")),
                                                                                                                       Map.entry(IntegerRangeArgument.class,
                                                                                                                                 () -> new IntegerRangeArgument("value")),
                                                                                                                       Map.entry(ItemPredicateArgument.class,
                                                                                                                                 () -> new ItemPredicateArgument("value")),
                                                                                                                       Map.entry(ItemStackArgument.class,
                                                                                                                                 () -> new ItemStackArgument("value")),
                                                                                                                       Map.entry(LiteralArgument.class,
                                                                                                                                 () -> new LiteralArgument("value",
                                                                                                                                                           List.of("literal"))),
                                                                                                                       Map.entry(LocationArgument.class,
                                                                                                                                 () -> new LocationArgument("value")),
                                                                                                                       Map.entry(LootTableArgument.class,
                                                                                                                                 () -> new LootTableArgument("value")),
                                                                                                                       Map.entry(MessageArgument.class,
                                                                                                                                 () -> new MessageArgument("value")),
                                                                                                                       Map.entry(LongArgument.class,
                                                                                                                                 () -> new LongArgument("value")),
                                                                                                                       Map.entry(NamespacedKeyArgument.class,
                                                                                                                                 () -> new NamespacedKeyArgument("value")),
                                                                                                                       Map.entry(NBTCompoundArgument.class,
                                                                                                                                 () -> new NBTCompoundArgument("value")),
                                                                                                                       Map.entry(NameableObjectArgument.class,
                                                                                                                                 () -> new NameableObjectArgument<>("value",
                                                                                                                                                                    List.of(new SampleNameable("alpha")))),
                                                                                                                       Map.entry(ObjectArgument.class,
                                                                                                                                 () -> new ObjectArgument<>("value",
                                                                                                                                                            Map.of("alpha",
                                                                                                                                                                   "mapped"))),
                                                                                                                       Map.entry(ObjectiveArgument.class,
                                                                                                                                 () -> new ObjectiveArgument("value")),
                                                                                                                       Map.entry(OfflinePlayerArgument.class,
                                                                                                                                 () -> new OfflinePlayerArgument("value")),
                                                                                                                       Map.entry(OfflinePlayersArgument.class,
                                                                                                                                 () -> new OfflinePlayersArgument("value")),
                                                                                                                       Map.entry(ParticleArgument.class,
                                                                                                                                 () -> new ParticleArgument("value")),
                                                                                                                       Map.entry(PlayerArgument.class,
                                                                                                                                 () -> new PlayerArgument("value")),
                                                                                                                       Map.entry(PlayersArgument.class,
                                                                                                                                 () -> new PlayersArgument("value")),
                                                                                                                       Map.entry(PotionEffectArgument.class,
                                                                                                                                 () -> new PotionEffectArgument("value")),
                                                                                                                       Map.entry(RecipeArgument.class,
                                                                                                                                 () -> new RecipeArgument("value")),
                                                                                                                       Map.entry(ScoreboardDisplaySlotArgument.class,
                                                                                                                                 () -> new ScoreboardDisplaySlotArgument("value")),
                                                                                                                       Map.entry(SoundArgument.class,
                                                                                                                                 () -> new SoundArgument("value")),
                                                                                                                       Map.entry(StringArgument.class,
                                                                                                                                 () -> new StringArgument("value")),
                                                                                                                       Map.entry(TeamArgument.class,
                                                                                                                                 () -> new TeamArgument("value")),
                                                                                                                       Map.entry(UnparsedArgument.class,
                                                                                                                                 () -> new UnparsedArgument("value")),
                                                                                                                       Map.entry(WorldArgument.class,
                                                                                                                                 () -> new WorldArgument("value")),
                                                                                                                       Map.entry(UUIDArgument.class,
                                                                                                                                 () -> new UUIDArgument("value")),
                                                                                                                       Map.entry(UUIDsArgument.class,
                                                                                                                                 () -> new UUIDsArgument("value")));

    @Test
    void allArgumentClassesHaveFactories() throws Exception {
        assertThat(ARGUMENT_FACTORIES.keySet()).containsExactlyInAnyOrderElementsOf(findConcreteArgumentClasses());
    }

    @Test
    void allArgumentsCanBeWiredIntoCommandTester() {
        ARGUMENT_FACTORIES.forEach((type, factory) -> assertThatCode(() -> {
            try (MockedStatic<Bukkit> ignored = mockBukkitStatics()) {
                new CommandTester(() -> new Command("cmd") {{
                    argument((CommonArgument) factory.get()).execute(ctx -> ctx.sendSuccess("ok"));
                }}, "test.command");
            }
        }).as(type.getSimpleName())
          .doesNotThrowAnyException());
    }

    private List<Class<?>> findConcreteArgumentClasses() throws Exception {
        String packagePath = ARGUMENT_PACKAGE.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread()
                                        .getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        List<Class<?>> result = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File dir = new File(resource.toURI());
                File[] files = dir.listFiles();
                if (files == null) {
                    continue;
                }
                for (File file : files) {
                    addIfConcreteArgument(file.getName(), classLoader, result);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                try (JarFile jarFile = jarConn.getJarFile()) {
                    jarFile.stream()
                           .map(JarEntry::getName)
                           .filter(name -> name.startsWith(packagePath + "/") && name.endsWith(".class")
                                   && !name.substring(packagePath.length() + 1)
                                           .contains("/"))
                           .map(name -> name.substring(packagePath.length() + 1))
                           .forEach(name -> addIfConcreteArgument(name, classLoader, result));
                }
            }
        }
        return result;
    }

    private void addIfConcreteArgument(String filename, ClassLoader classLoader, List<Class<?>> result) {
        if (!filename.endsWith(".class") || filename.contains("$")) {
            return;
        }

        if (filename.startsWith("Common")) {
            return;
        }

        try {
            Class<?> cls = Class.forName(ARGUMENT_PACKAGE + "." + filename.replace(".class", ""), false, classLoader);
            if (!Modifier.isAbstract(cls.getModifiers()) && CommonArgument.class.isAssignableFrom(cls)) {
                result.add(cls);
            }
        } catch (ClassNotFoundException ignored) {
            // A listed name that does not load is not an argument class to cover.
        }
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
