package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.argument.*;
import net.minecraft.core.particles.ParticleTypes;
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

        commands.add(check("blockPosArgument",
                           new BlockPosArgument("a"),
                           "1 2 3",
                           x -> x.getX() + "," + x.getY() + "," + x.getZ(),
                           "1,2,3"));
        commands.add(check("blockStateArgument",
                           new BlockStateArgument("a"),
                           "minecraft:stone",
                           x -> x.getState()
                                 .toString(),
                           "Block{minecraft:stone}"));
        commands.add(check("booleanArgument", new BooleanArgument("a"), "true", String::valueOf, "true"));
        commands.add(check("doubleArgument", new DoubleArgument("a"), "1.5", String::valueOf, "1.5"));
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
        commands.add(check("enumArgument", new EnumArgument<>("a", TestEnum.class), "second", String::valueOf, "SECOND"));
        commands.add(check("floatArgument", new FloatArgument("a"), "1.5", String::valueOf, "1.5"));
        commands.add(check("gameProfileArgument",
                           new GameProfileArgument("a"),
                           playerName,
                           x -> x.getName() + "," + x.getId(),
                           playerName + "," + playerUuid));
        commands.add(check("integerArgument", new IntegerArgument("a"), "10", String::valueOf, "10"));
        commands.add(check("itemStackArgument",
                           new ItemStackArgument("a"),
                           "minecraft:diamond",
                           x -> (x.getItem() == Items.DIAMOND) + "," + x.getCount(),
                           "true,1"));
        commands.add(check("literalArgument", new LiteralArgument("a", List.of("first", "second")), "second", x -> x, "second"));
        commands.add(check("locationArgument",
                           new LocationArgument("a"),
                           "1.25 2.5 3.75",
                           x -> x.getX() + "," + x.getY() + "," + x.getZ(),
                           "1.25,2.5,3.75"));
        // MCProtocolLib 1.16.5 cannot decode brigadier:long in the command tree packet, and registering this
        // case disconnects the integration bot before tests can run.
        //? if >=1.17
        commands.add(check("longArgument", new LongArgument("a"), "100", String::valueOf, "100"));
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

    private static String entityNames(List<? extends Entity> entities) {
        return entities.stream()
                       .map(x -> x.getName()
                                  .getString())
                       .collect(Collectors.joining(","));
    }

    public enum TestEnum {
        FIRST, SECOND
    }
}
