package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandActor;
import net.kunmc.lab.commandlib.CommandActorType;
import net.minecraft.server.MinecraftServer;

import java.util.List;

// Commands are dispatched from the server console source.
public final class ActorTest extends TestBase {
    public ActorTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return List.of(consoleActorType(), consoleActorUnwrapsServer());
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
