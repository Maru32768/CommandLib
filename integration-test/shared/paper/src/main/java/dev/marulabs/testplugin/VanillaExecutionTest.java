package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandActorType;
import dev.marulabs.commandlib.argument.EntityArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scoreboard.Objective;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * What reaches a command run through {@code /execute} and from a command block: the sender, the executing entity, the
 * location and selectors relative to the executor.
 */
public final class VanillaExecutionTest extends TestBase {
    private static final String OBJECTIVE = "commandlibblock";
    private static final String SUCCESS_COUNT = "commandBlockSuccessCount";
    private final String playerName;
    private final World world;
    // Two command blocks over the world spawn, which is loaded and ticking near the bot.
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final AtomicBoolean failureBlockRan = new AtomicBoolean();

    public VanillaExecutionTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
        this.world = Bukkit.getWorlds()
                           .get(0);
        Location spawn = world.getSpawnLocation();
        this.blockX = spawn.getBlockX();
        this.blockY = spawn.getBlockY() + 3;
        this.blockZ = spawn.getBlockZ();
    }

    @Override
    public List<String> build() {
        return concat(executeAsKeepsSenderAndSetsEntity(),
                      executePositionedSetsLocation(),
                      selectorsResolveFromExecutor(),
                      commandBlockRunsAsBlockSender());
    }

    @Override
    public List<String> afterReload() {
        return List.of(storeSuccessCount("#success", blockX),
                       storeSuccessCount("#failure", blockX + 2),
                       buildCommand(command, SUCCESS_COUNT));
    }

    public List<String> executeAsKeepsSenderAndSetsEntity() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                Entity entity = ctx.getEntity();
                putResult(key,
                          ctx.getSender()
                             .getName() + ","
                                  + ctx.getActor()
                                       .getType()
                                  + "," + (entity == null ? "none" : entity.getName()),
                          Bukkit.getConsoleSender()
                                .getName() + "," + CommandActorType.CONSOLE + "," + playerName);
            });
        }});

        return List.of("execute as " + playerName + " run " + buildCommand(command, name));
    }

    public List<String> executePositionedSetsLocation() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                Location location = ctx.getLocation();
                putResult(key,
                          location == null ? "none" : describe(location),
                          world.getName() + ",10.25,70.5,-20.75,90.0,45.0");
            });
        }});

        return List.of("execute positioned 10.25 70.5 -20.75 rotated 90 45 run " + buildCommand(command, name));
    }

    /**
     * {@code @s} is the entity of {@code execute as}, and {@code distance} is measured from the position of
     * {@code execute at}.
     */
    public List<String> selectorsResolveFromExecutor() {
        String name = getMethodName();
        String key = getKey();
        StringBuilder resolved = new StringBuilder();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new EntityArgument("target")).execute((target, ctx) -> {
                resolved.append(resolved.length() == 0 ? "" : ",")
                        .append(target.getName());
                putResult(key, resolved.toString(), playerName + "," + playerName);
            });
        }});

        return List.of("execute as " + playerName + " run " + buildCommand(command, name + " @s"),
                       "execute as " + playerName + " at @s run "
                               + buildCommand(command, name + " @e[type=player,distance=..0.01,limit=1]"));
    }

    /**
     * Places two command blocks and powers them. They run on the next tick, so their SuccessCount is read after the
     * reload ({@link #afterReload()}).
     */
    public List<String> commandBlockRunsAsBlockSender() {
        String name = getMethodName();
        String key = getKey();
        String expected = blockX + "," + blockY + "," + blockZ;

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> {
                CommandSender sender = ctx.getSender();
                Location location = ctx.getLocation();
                String block = sender instanceof BlockCommandSender
                        ? describe(((BlockCommandSender) sender).getBlock()) : sender.getClass()
                                                                                     .getName();
                String position = location == null ? "none"
                        : location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
                putResult(key,
                          block + "," + ctx.getActor()
                                           .getType()
                                  + "," + position,
                          expected + "," + CommandActorType.COMMAND_BLOCK + "," + expected);
                ctx.setResult(42);
            });
        }}, new Command(name + "Failure") {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> {
                failureBlockRan.set(true);
                throw new CommandPrerequisiteException("command block failure probe");
            });
        }});
        commandBlockSuccessCount();

        return List.of("scoreboard objectives add " + OBJECTIVE + " dummy",
                       setCommandBlock(blockX, buildCommand(command, name)),
                       "setblock " + blockX + " " + (blockY + 1) + " " + blockZ + " minecraft:redstone_block",
                       setCommandBlock(blockX + 2, buildCommand(command, name + "Failure")),
                       "setblock " + (blockX + 2) + " " + (blockY + 1) + " " + blockZ + " minecraft:redstone_block");
    }

    /**
     * SuccessCount counts the successful runs of the command block's command, not the result the command sets. It is
     * dispatched after the reload, by when the blocks have run.
     */
    private void commandBlockSuccessCount() {
        String key = getClass().getSimpleName() + "." + SUCCESS_COUNT;

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(SUCCESS_COUNT) {{
            execute(ctx -> {
                Objective scores = Bukkit.getScoreboardManager()
                                         .getMainScoreboard()
                                         .getObjective(OBJECTIVE);
                String actual = scores == null ? "no objective" : scores.getScore("#success")
                                                                        .getScore()
                        + "," + scores.getScore("#failure")
                                      .getScore()
                        + "," + failureBlockRan.get();
                putResult(key, actual, "1,0,true");
            });
        }});
    }

    private String setCommandBlock(int x, String commandLine) {
        return "setblock " + x + " " + blockY + " " + blockZ + " minecraft:command_block{Command:\"" + commandLine
                + "\"}";
    }

    private String storeSuccessCount(String holder, int x) {
        return "execute store result score " + holder + " " + OBJECTIVE + " run data get block " + x + " " + blockY
                + " " + blockZ + " SuccessCount";
    }

    private static String describe(Location location) {
        World world = location.getWorld();
        return (world == null ? "none" : world.getName()) + "," + location.getX() + "," + location.getY() + ","
                + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private static String describe(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        return Arrays.stream(lists)
                     .flatMap(List::stream)
                     .collect(Collectors.toList());
    }
}
