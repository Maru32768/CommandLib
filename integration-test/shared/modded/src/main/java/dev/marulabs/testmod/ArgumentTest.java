package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.argument.*;
import dev.marulabs.commandlib.util.CurrentServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
//? if >=1.19.3
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;
//? if <1.20.2
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Commands are dispatched from the server console source while the test player is online.
public final class ArgumentTest extends TestBase {
    private final String playerName;
    private final String playerUuid;

    public ArgumentTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
        this.playerUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8))
                              .toString();
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.add(check("advancementArgument",
                           new AdvancementArgument("a"),
                           "minecraft:story/mine_stone",
                           //? if >=1.20.2 {
                           /*x -> String.valueOf(x.id()),
                           *///?} else
                           x -> String.valueOf(x.getId()),
                           "minecraft:story/mine_stone"));
        commands.add(check("attributeArgument",
                           new AttributeArgument("a"),
                           //? if >=1.21.2 {
                           /*"minecraft:max_health",
                           *///?} else
                           "minecraft:generic.max_health",
                           //? if >=1.20.5 {
                           /*x -> String.valueOf(x.value() == Attributes.MAX_HEALTH.value()),
                           *///?} else
                           x -> String.valueOf(x == Attributes.MAX_HEALTH),
                           "true"));
        commands.add(check("biomeArgument",
                           new BiomeArgument("a"),
                           "minecraft:plains",
                           ArgumentTest::biomeKey,
                           "minecraft:plains"));
        commands.add(check("blockPositionArgument",
                           new BlockPositionArgument("a"),
                           "1 2 3",
                           x -> x.getX() + "," + x.getY() + "," + x.getZ(),
                           "1,2,3"));
        commands.add(blockPredicateArgument());
        commands.add(check("blockStateArgument",
                           new BlockStateArgument("a"),
                           "minecraft:stone",
                           x -> x.getState()
                                 .toString(),
                           "Block{minecraft:stone}"));
        commands.add(check("booleanArgument", new BooleanArgument("a"), "true", String::valueOf, "true"));
        commands.add(check("chatColorArgument", new ChatColorArgument("a"), "dark_red", Enum::name, "DARK_RED"));
        // JSON, which the SNBT reader of 1.21.5 also accepts.
        commands.add(check("componentArgument",
                           new ComponentArgument("a"),
                           "{\"text\":\"hello\",\"color\":\"red\"}",
                           x -> x.getString() + " " + x.getStyle()
                                                       .getColor()
                                                       .serialize(),
                           "hello red"));
        commands.add(check("doubleArgument", new DoubleArgument("a"), "1.5", String::valueOf, "1.5"));
        commands.add(check("doubleRangeArgument",
                           new DoubleRangeArgument("a"),
                           "0.5..10",
                           String::valueOf,
                           "[0.5..10.0]"));
        commands.add(check("durationArgument", new DurationArgument("a"), "1h30m", String::valueOf, "PT1H30M"));
        commands.add(check("effectArgument",
                           new EffectArgument("a"),
                           "minecraft:speed",
                           x -> String.valueOf(x == MobEffects.MOVEMENT_SPEED),
                           "true"));
        commands.add(check("enchantmentArgument",
                           new EnchantmentArgument("a"),
                           "minecraft:sharpness",
                           //? if >=1.20.5 {
                           /*// Enchantments are data-driven, so Enchantments.SHARPNESS is the registry key of the holder.
                           x -> String.valueOf(x.is(Enchantments.SHARPNESS)),
                           *///?} else
                           x -> String.valueOf(x == Enchantments.SHARPNESS),
                           "true"));
        commands.add(check("entitiesArgument", new EntitiesArgument("a"), "@a", ArgumentTest::entityNames, playerName));
        commands.add(check("entityArgument",
                           new EntityArgument("a"),
                           playerName,
                           x -> x.getName()
                                 .getString(),
                           playerName));
        commands.add(check("entityTypeArgument",
                           new EntityTypeArgument("a"),
                           "minecraft:zombie",
                           x -> EntityType.getKey(x)
                                          .toString(),
                           "minecraft:zombie"));
        commands.add(check("enumArgument",
                           new EnumArgument<>("a", TestEnum.class),
                           "second",
                           String::valueOf,
                           "SECOND"));
        commands.add(check("floatArgument", new FloatArgument("a"), "1.5", String::valueOf, "1.5"));
        commands.add(check("gameModeArgument", new GameModeArgument("a"), "creative", Enum::name, "CREATIVE"));
        commands.add(check("gameProfileArgument",
                           new GameProfileArgument("a"),
                           playerName,
                           x -> x.getName() + "," + x.getId(),
                           playerName + "," + playerUuid));
        commands.add(check("integerArgument", new IntegerArgument("a"), "10", String::valueOf, "10"));
        commands.add(check("integerRangeArgument", new IntegerRangeArgument("a"), "1..10", String::valueOf, "[1..10]"));
        commands.add(check("itemPredicateArgument",
                           new ItemPredicateArgument("a"),
                           "minecraft:diamond",
                           x -> x.test(new ItemStack(Items.DIAMOND)) + "," + x.test(new ItemStack(Items.DIRT)),
                           "true,false"));
        commands.add(check("itemStackArgument",
                           new ItemStackArgument("a"),
                           "minecraft:diamond",
                           x -> (x.getItem() == Items.DIAMOND) + "," + x.getCount(),
                           "true,1"));
        commands.add(check("literalArgument",
                           new LiteralArgument("a", List.of("first", "second")),
                           "second",
                           x -> x,
                           "second"));
        commands.add(check("locationArgument",
                           new LocationArgument("a"),
                           "1.25 2.5 3.75",
                           x -> x.getX() + "," + x.getY() + "," + x.getZ(),
                           "1.25,2.5,3.75"));
        commands.add(check("uncenteredLocationArgument",
                           new LocationArgument("a", false),
                           "1 2 3",
                           x -> x.getX() + "," + x.getY() + "," + x.getZ(),
                           "1.0,2.0,3.0"));
        // MCProtocolLib 1.16.5 cannot decode brigadier:long in the command tree packet, and registering this
        // case disconnects the integration bot before tests can run.
        //? if >=1.17
        commands.add(check("longArgument", new LongArgument("a"), "100", String::valueOf, "100"));
        commands.add(check("lootTableArgument",
                           new LootTableArgument("a"),
                           "minecraft:chests/simple_dungeon",
                           x -> String.valueOf(x != LootTable.EMPTY),
                           "true"));
        commands.add(check("messageArgument",
                           new MessageArgument("a"),
                           "hello, world!",
                           x -> x.getString(),
                           "hello, world!"));
        commands.add(check("nbtCompoundArgument", new NBTCompoundArgument("a"), "{a:true}", String::valueOf, "{a:1b}"));
        commands.add(check("nameableObjectArgument",
                           new NameableObjectArgument<>("a", List.of(() -> "nameable")),
                           "nameable",
                           x -> x.tabCompleteName(),
                           "nameable"));
        commands.add(check("objectArgument", new ObjectArgument<>("a", Map.of("one", 1)), "one", String::valueOf, "1"));
        commands.add(check("objectiveArgument",
                           new ObjectiveArgument("a"),
                           TestMain.TEST_OBJECTIVE_NAME,
                           x -> x.getName(),
                           TestMain.TEST_OBJECTIVE_NAME));
        commands.add(check("particleArgument",
                           new ParticleArgument("a"),
                           "minecraft:flame",
                           x -> String.valueOf(x.getType() == ParticleTypes.FLAME),
                           "true"));
        commands.add(check("playerArgument",
                           new PlayerArgument("a"),
                           playerName,
                           x -> x.getGameProfile()
                                 .getName(),
                           playerName));
        commands.add(check("playersArgument", new PlayersArgument("a"), "@a", ArgumentTest::entityNames, playerName));
        commands.add(check("recipeArgument",
                           new RecipeArgument("a"),
                           "minecraft:oak_planks",
                           //? if >=1.20.2 {
                           /*x -> String.valueOf(x.id()),
                           *///?} else
                           x -> String.valueOf(x.getId()),
                           "minecraft:oak_planks"));
        commands.add(check("resourceLocationArgument",
                           new ResourceLocationArgument("a"),
                           "commandlib:example/path",
                           String::valueOf,
                           "commandlib:example/path"));
        commands.add(check("registryArgument",
                           //? if >=1.19.3 {
                           new RegistryArgument<>("a", Registries.ITEM),
                           //?} else
                           /*new RegistryArgument<>("a", net.minecraft.core.Registry.ITEM_REGISTRY),*/
                           "minecraft:diamond",
                           x -> String.valueOf(x == Items.DIAMOND),
                           "true"));
        commands.add(check("scoreboardDisplaySlotArgument",
                           new ScoreboardDisplaySlotArgument("a"),
                           "sidebar",
                           //? if >=1.20.2 {
                           /*x -> x.getSerializedName(),
                           *///?} else
                           x -> Scoreboard.getDisplaySlotName(x),
                           "sidebar"));
        commands.add(check("soundArgument",
                           new SoundArgument("a"),
                           "minecraft:entity.pig.ambient",
                           x -> String.valueOf(x == SoundEvents.PIG_AMBIENT),
                           "true"));
        commands.add(check("stringArgument", new StringArgument("a"), "abc", x -> x, "abc"));
        commands.add(check("teamArgument",
                           new TeamArgument("a"),
                           TestMain.TEST_TEAM_NAME,
                           x -> x.getName(),
                           TestMain.TEST_TEAM_NAME));
        commands.add(check("timeArgument", new TimeArgument("a", 1), "0.5d", String::valueOf, "12000"));
        commands.add(check("unparsedArgument", new UnparsedArgument("a"), "a", x -> x, "a"));
        commands.add(check("uuidArgument", new UUIDArgument("a"), playerName, String::valueOf, playerUuid));
        commands.add(check("uuidsArgument",
                           new UUIDsArgument("a"),
                           "@a",
                           x -> x.stream()
                                 .map(String::valueOf)
                                 .collect(Collectors.joining(",")),
                           playerUuid));
        commands.add(check("worldArgument",
                           new WorldArgument("a"),
                           "minecraft:overworld",
                           x -> x.dimension()
                                 .location()
                                 .toString(),
                           "minecraft:overworld"));

        return commands;
    }

    private <T, A extends CommonArgument<T, CommandContext, A>> String check(String name,
                                                                             A arg,
                                                                             String input,
                                                                             Function<? super T, String> actual,
                                                                             String expected) {
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        arg.addUncaughtExceptionHandler((e, ctx) -> putException(key, e));
        command.addChildren(new Command(name) {{
            argument(arg).execute((a, ctx) -> {
                putResult(key, actual.apply(a), expected);
            });
        }});

        return buildCommand(name + " " + input);
    }

    // The predicate tests a block that the command places in the source's level.
    private String blockPredicateArgument() {
        String name = "blockPredicateArgument";
        String key = getKey(name);
        BlockPredicateArgument arg = new BlockPredicateArgument("a");

        putCommandNotExecutedResult(key);
        arg.addUncaughtExceptionHandler((e, ctx) -> putException(key, e));
        command.addChildren(new Command(name) {{
            argument(arg).execute((a, ctx) -> {
                ServerLevel level = ctx.getWorld();
                BlockPos pos = new BlockPos(0, 200, 0);
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                boolean stone = a.test(new BlockInWorld(level, pos, true));
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                putResult(key, stone + "," + a.test(new BlockInWorld(level, pos, true)), "true,false");
            });
        }});

        return buildCommand(name + " minecraft:stone");
    }

    private static String biomeKey(Biome biome) {
        //? if >=1.21.2 {
        /*return String.valueOf(CurrentServer.get()
                                           .registryAccess()
                                           .lookupOrThrow(Registries.BIOME)
                                           .getKey(biome));
        *///?} elif >=1.19.3 {
        return String.valueOf(CurrentServer.get()
                                           .registryAccess()
                                           .registryOrThrow(Registries.BIOME)
                                           .getKey(biome));
        //?} else {
        /*return String.valueOf(CurrentServer.get()
                                           .registryAccess()
                                           .registryOrThrow(net.minecraft.core.Registry.BIOME_REGISTRY)
                                           .getKey(biome));
        *///?}
    }

    private static String entityNames(List<? extends Entity> entities) {
        return entities.stream()
                       .map(x -> x.getName()
                                  .getString())
                       .collect(Collectors.joining(","));
    }

    public enum TestEnum {
        FIRST,
        SECOND
    }
}
