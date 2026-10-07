package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.argument.*;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public final class ArgumentTest extends TestBase {
    private final String playerName;

    public ArgumentTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(advancementArgument());
        commands.addAll(attributeArgument());
        commands.addAll(blockDataArgument());
        commands.addAll(biomeArgument());
        commands.addAll(booleanArgument());
        commands.addAll(chatColorArgument());
        commands.addAll(doubleArgument());
        commands.addAll(enchantmentArgument());
        commands.addAll(entitiesArgument());
        commands.addAll(entityArgument());
        commands.addAll(entityTypeArgument());
        commands.addAll(enumArgument());
        commands.addAll(floatArgument());
        commands.addAll(gameModeArgument());
        commands.addAll(integerArgument());
        commands.addAll(itemStackArgument());
        commands.addAll(literalArgument());
        commands.addAll(locationArgument());
        commands.addAll(lootTableArgument());
        if (supportsLongArgumentCommandTree()) {
            commands.addAll(longArgument());
        }
        commands.addAll(namespacedKeyArgument());
        commands.addAll(nameableObjectArgument());
        commands.addAll(objectArgument());
        commands.addAll(offlinePlayerArgument());
        commands.addAll(offlinePlayersArgument());
        commands.addAll(objectiveArgument());
        commands.addAll(particleArgument());
        commands.addAll(dustParticleArgument());
        commands.addAll(playerArgument());
        commands.addAll(playersArgument());
        commands.addAll(potionEffectArgument());
        commands.addAll(recipeArgument());
        commands.addAll(scoreboardDisplaySlotArgument());
        commands.addAll(soundArgument());
        commands.addAll(stringArgument());
        commands.addAll(teamArgument());
        commands.addAll(unparsedArgument());
        commands.addAll(uuidArgument());
        commands.addAll(uuidsArgument());
        commands.addAll(worldArgument());

        return commands;
    }

    public List<String> advancementArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new AdvancementArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          a.getKey()
                           .toString(),
                          "minecraft:story/mine_stone");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:story/mine_stone"));
    }

    public List<String> attributeArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new AttributeArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "GENERIC_MAX_HEALTH");
            });
        }});

        return List.of(buildCommand(command, name + " " + registryInput("generic_max_health", "generic.max_health")));
    }

    public List<String> blockDataArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new BlockDataArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getMaterial()
                           .name(), "STONE");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:stone"));
    }

    public List<String> biomeArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new BiomeArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "PLAINS");
            });
        }});

        return List.of(buildCommand(command, name + " plains"));
    }

    public List<String> booleanArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new BooleanArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "true");
            });
        }});

        return List.of(buildCommand(command, name + " true"));
    }

    public List<String> chatColorArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ChatColorArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.name(), "DARK_RED");
            });
        }});

        return List.of(buildCommand(command, name + " dark_red"));
    }

    public List<String> doubleArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new DoubleArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "1.0");
            });
        }});

        return List.of(buildCommand(command, name + " 1.0"));
    }

    public List<String> enchantmentArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EnchantmentArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getKey()
                           .toString(), "minecraft:flame");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:flame"));
    }

    /**
     * Dust takes options, so its particle options are not the particle type itself as they are for flame.
     */
    public List<String> dustParticleArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ParticleArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, String.valueOf(a.getDataType() == Particle.DustOptions.class), "true");
            });
        }});

        // Particle options are written as SNBT from 1.20.5.
        String input = isLessThan(BukkitUtil.getMinecraftVersion(), "1.20.5")
                       ? "minecraft:dust 1 0 0 1"
                       : "minecraft:dust{color:[1.0,0.0,0.0],scale:1.0}";
        return List.of(buildCommand(command, name + " " + input));
    }

    public List<String> entitiesArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EntitiesArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, Integer.toString(a.size()), "1");
            });
        }});

        return List.of(buildCommand(command, name + " @r"));
    }

    public List<String> entityArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EntityArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), playerName);
            });
        }});

        return List.of(buildCommand(command, name + " @r"));
    }

    public List<String> entityTypeArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EntityTypeArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "ZOMBIE");
            });
        }});

        return List.of(buildCommand(command, name + " zombie"));
    }

    public List<String> enumArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EnumArgument<>("a", Material.class).addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "AIR");
            });
        }});

        return List.of(buildCommand(command, name + " air"));
    }

    public List<String> floatArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new FloatArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "1.0");
            });
        }});

        return List.of(buildCommand(command, name + " 1.0"));
    }

    public List<String> gameModeArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new GameModeArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "CREATIVE");
            });
        }});

        return List.of(buildCommand(command, name + " creative"));
    }

    public List<String> integerArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "1");
            });
        }});

        return List.of(buildCommand(command, name + " 1"));
    }

    public List<String> itemStackArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ItemStackArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getType()
                           .name() + " x" + a.getAmount(), "DIRT x1");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:dirt"));
    }

    public List<String> literalArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new LiteralArgument("a", List.of("a")).addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a, "a");
            });
        }});

        return List.of(buildCommand(command, name + " a"));
    }

    public List<String> locationArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new LocationArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getX() + "," + a.getY() + "," + a.getZ() + "," + (a.getWorld() != null), "0.5,0.0,0.5,true");
            });
        }});

        return List.of(buildCommand(command, name + " 0 0 0"));
    }

    public List<String> lootTableArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new LootTableArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          a.getKey()
                           .toString(),
                          "minecraft:chests/simple_dungeon");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:chests/simple_dungeon"));
    }

    public List<String> longArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new LongArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "1");
            });
        }});

        return List.of(buildCommand(command, name + " 1"));
    }

    private boolean supportsLongArgumentCommandTree() {
        // MCProtocolLib 1.16.5 cannot decode brigadier:long in ServerDeclareCommandsPacket.
        // Registering this case disconnects the integration bot before tests can run.
        return !"1.16.5".equals(BukkitUtil.getMinecraftVersion());
    }

    private String registryInput(String legacyName, String registryName) {
        return isLessThan(BukkitUtil.getMinecraftVersion(), "1.21.0") ? legacyName : registryName;
    }

    private boolean isLessThan(String version, String other) {
        String[] versionUnits = normalizeVersion(version).split("\\.");
        String[] otherUnits = normalizeVersion(other).split("\\.");
        for (int i = 0; i < versionUnits.length; i++) {
            int versionUnit = Integer.parseInt(versionUnits[i]);
            int otherUnit = Integer.parseInt(otherUnits[i]);
            if (versionUnit < otherUnit) {
                return true;
            }
            if (versionUnit > otherUnit) {
                return false;
            }
        }
        return false;
    }

    private String normalizeVersion(String version) {
        return version.split("\\.").length == 2 ? version + ".0" : version;
    }

    public List<String> nameableObjectArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new NameableObjectArgument<>("a", List.of(() -> "a")).addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.tabCompleteName(), "a");
            });
        }});

        return List.of(buildCommand(command, name + " a"));
    }

    public List<String> namespacedKeyArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new NamespacedKeyArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "commandlib:example/path");
            });
        }});

        return List.of(buildCommand(command, name + " commandlib:example/path"));
    }

    public List<String> objectArgument() {
        String name = getMethodName();
        String key = getKey();

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ObjectArgument<>("a", map).addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "1");
            });
        }});

        return List.of(buildCommand(command, name + " a"));
    }

    public List<String> offlinePlayerArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new OfflinePlayerArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), playerName);
            });
        }});

        return List.of(buildCommand(command, name + " " + playerName));
    }

    public List<String> offlinePlayersArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new OfflinePlayersArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          a.stream()
                           .map(OfflinePlayer::getName)
                           .collect(java.util.stream.Collectors.joining(",")),
                          playerName);
            });
        }});

        return List.of(buildCommand(command, name + " " + playerName));
    }

    public List<String> objectiveArgument() {
        String name = getMethodName();
        String key = getKey();
        String objectiveName = "commandlib_it";
        Scoreboard scoreboard = Bukkit.getScoreboardManager()
                                      .getMainScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            scoreboard.registerNewObjective(objectiveName, "dummy");
        }

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ObjectiveArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), objectiveName);
            });
        }});

        return List.of(buildCommand(command, name + " " + objectiveName));
    }

    public List<String> particleArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ParticleArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.name(), "FLAME");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:flame"));
    }

    public List<String> playerArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new PlayerArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), playerName);
            });
        }});

        return List.of(buildCommand(command, name + " " + playerName));
    }

    public List<String> playersArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new PlayersArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          a.stream()
                           .map(x -> x.getName())
                           .collect(java.util.stream.Collectors.joining(",")),
                          playerName);
            });
        }});

        return List.of(buildCommand(command, name + " " + playerName));
    }

    public List<String> potionEffectArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new PotionEffectArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getType()
                           .equals(PotionEffectType.SPEED) + ":" + a.getDuration() + ":" + a.getAmplifier(), "true:1:0");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:speed"));
    }

    public List<String> recipeArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new RecipeArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          ((Keyed) a).getKey()
                                     .toString(),
                          "minecraft:stick");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:stick"));
    }

    public List<String> scoreboardDisplaySlotArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new ScoreboardDisplaySlotArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.name(), "SIDEBAR");
            });
        }});

        return List.of(buildCommand(command, name + " sidebar"));
    }

    public List<String> soundArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new SoundArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), "BLOCK_ANVIL_LAND");
            });
        }});

        return List.of(buildCommand(command, name + " " + registryInput("block_anvil_land", "block.anvil.land")));
    }

    public List<String> stringArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a, "a");
            });
        }});

        return List.of(buildCommand(command, name + " a"));
    }

    public List<String> teamArgument() {
        String name = getMethodName();
        String key = getKey();

        String teamName = "test";
        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new TeamArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), teamName);
            });
        }});

        return List.of(buildCommand(command, name + " " + teamName));
    }

    public List<String> unparsedArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new UnparsedArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a, "a");
            });
        }});

        return List.of(buildCommand(command, name + " a"));
    }

    public List<String> uuidArgument() {
        String name = getMethodName();
        String key = getKey();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new UUIDArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.toString(), uuid.toString());
            });
        }});

        return List.of(buildCommand(command, name + " " + uuid));
    }

    public List<String> worldArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new WorldArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key, a.getName(), "world");
            });
        }});

        return List.of(buildCommand(command, name + " minecraft:overworld"));
    }


    public List<String> uuidsArgument() {
        String name = getMethodName();
        String key = getKey();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new UUIDsArgument("a").addUncaughtExceptionHandler((e, ctx) -> {
                putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
            })).execute((a, ctx) -> {
                putResult(key,
                          a.toString(),
                          List.of(uuid)
                              .toString());
            });
        }});

        return List.of(buildCommand(command, name + " " + uuid));
    }
}
