package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandActor;
import dev.marulabs.commandlib.CommandActorType;
import dev.marulabs.commandlib.util.Location;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.List;

// Commands are dispatched from the server console source, some through /execute.
public final class ActorTest extends TestBase {
    public ActorTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return List.of(consoleActorType(),
                       consoleActorUnwrapsServer(),
                       executeAsPlayerChangesActor(),
                       executePositionedSetsLocation(),
                       commandBlockActorType());
    }

    // The command block runs the probe on a later tick, before the results are reported. Production class names
    // differ from development ones, so this only passes when the source is recognized by type.
    private String commandBlockActorType() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                CommandActor actor = ctx.getActor();
                putResult(key,
                          actor.getType() + "," + actor.isConsole(),
                          CommandActorType.COMMAND_BLOCK + ",false");
            });
        }});

        return commandBlock(buildCommand(name), 3);
    }

    // Places a command block above the console's position that runs the command once.
    static String commandBlock(String command, int height) {
        return "setblock ~ ~" + height + " ~ minecraft:command_block{auto:1b,Command:\"" + command + "\"}";
    }

    // Unlike Bukkit, whose sender stays the console, the actor follows the entity of execute as.
    private String executeAsPlayerChangesActor() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                Entity entity = ctx.getEntity();
                putResult(key,
                          ctx.getActor()
                             .getType() + ","
                                  + ctx.getActor()
                                       .getName()
                                  + "," + (entity == null ? "none" : entity.getName()
                                                                           .getString()),
                          CommandActorType.PLAYER + "," + TestMain.TEST_PLAYER_NAME + ","
                                  + TestMain.TEST_PLAYER_NAME);
            });
        }});

        return "execute as " + TestMain.TEST_PLAYER_NAME + " run " + buildCommand(name);
    }

    private String executePositionedSetsLocation() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                Location location = ctx.getLocation();
                putResult(key,
                          location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw()
                                  + "," + location.getPitch(),
                          "10.25,70.5,-20.75,90.0,45.0");
            });
        }});

        return "execute positioned 10.25 70.5 -20.75 rotated 90 45 run " + buildCommand(name);
    }

    private String consoleActorType() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                CommandActor actor = ctx.getActor();
                putResult(key, actor.getType() + "," + actor.isConsole(), CommandActorType.CONSOLE + ",true");
            });
        }});

        return buildCommand(name);
    }

    // The raw command source is a private field whose name differs between development and production,
    // so this only passes when unwrap works with the production (SRG) names.
    private String consoleActorUnwrapsServer() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                putResult(key,
                          String.valueOf(ctx.getActor()
                                            .unwrap(MinecraftServer.class)
                                            .isPresent()),
                          "true");
            });
        }});

        return buildCommand(name);
    }
}
