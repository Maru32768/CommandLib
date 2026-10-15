package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.argument.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
        commands.add(check("enumArgument",
                           new EnumArgument<>("a", TestEnum.class),
                           "second",
                           String::valueOf,
                           "SECOND"));
        commands.add(check("floatArgument", new FloatArgument("a"), "1.5", String::valueOf, "1.5"));
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
        commands.add(check("resourceLocationArgument",
                           new ResourceLocationArgument("a"),
                           "commandlib:example/path",
                           String::valueOf,
                           "commandlib:example/path"));
        commands.add(check("stringArgument", new StringArgument("a"), "abc", x -> x, "abc"));
        commands.add(check("teamArgument",
                           new TeamArgument("a"),
                           TestMain.TEST_TEAM_NAME,
                           x -> x.getName(),
                           TestMain.TEST_TEAM_NAME));
        commands.add(check("unparsedArgument", new UnparsedArgument("a"), "a", x -> x, "a"));
        commands.add(check("uuidArgument", new UUIDArgument("a"), playerName, String::valueOf, playerUuid));
        commands.add(check("uuidsArgument",
                           new UUIDsArgument("a"),
                           "@a",
                           x -> x.stream()
                                 .map(String::valueOf)
                                 .collect(Collectors.joining(",")),
                           playerUuid));

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
