package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ForgeCommandActorTest {
    private static CommandSourceStack source(CommandSource raw, int permissionLevel, MinecraftServer server) {
        return new CommandSourceStack(raw,
                                      Vec3.ZERO,
                                      Vec2.ZERO,
                                      null,
                                      permissionLevel,
                                      "Source",
                                      Component.nullToEmpty("Source"),
                                      server,
                                      null);
    }

    @Test
    void the_server_is_the_console_and_passes_every_check() {
        CommandTester.MinecraftBootstrap.ensureBootstrapped();
        ForgeCommandActor actor = new ForgeCommandActor(source(Mockito.mock(MinecraftServer.class), 0, null));

        assertThat(actor.getType()).isEqualTo(CommandActorType.CONSOLE);
        assertThat(actor.hasPermission("actortest.undeclared")).isTrue();
    }

    @Test
    void another_source_is_unknown_and_judged_by_its_permission_level() {
        CommandTester.MinecraftBootstrap.ensureBootstrapped();
        MinecraftServer server = Mockito.mock(MinecraftServer.class);
        ForgeCommandActor guest = new ForgeCommandActor(source(CommandSource.NULL, 0, server));
        ForgeCommandActor admin = new ForgeCommandActor(source(CommandSource.NULL, 4, server));

        assertThat(guest.getType()).isEqualTo(CommandActorType.UNKNOWN);
        assertThat(guest.isConsole()).isFalse();
        assertThat(guest.hasPermission("actortest.undeclared")).isFalse();
        assertThat(admin.hasPermission("actortest.undeclared")).isTrue();
        assertThat(new PlatformAdapterImpl().hasPermission(source(CommandSource.NULL, 0, server),
                                                           "actortest.undeclared")).isFalse();
    }

    @Test
    void data_pack_functions_compile_commands_of_every_default() {
        // Vanilla's ServerFunctionLibrary compiles functions with this source: no server, function-permission-level.
        CommandSourceStack compiler = source(CommandSource.NULL, 2, null);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        new CommandNodeCreator<>(List.of(new Command("fn") {{
            addChildren(new Command("hidden") {{
                permission(DefaultPermission.NONE);
                execute(ctx -> {
                });
            }});
        }}), "actortest", ServerCommandScheduler.INSTANCE).build()
                                                          .forEach(dispatcher.getRoot()::addChild);

        ParseResults<CommandSourceStack> parse = dispatcher.parse("fn hidden", compiler);

        assertThat(parse.getReader()
                        .canRead()).isFalse();
        assertThat(parse.getContext()
                        .getCommand()).isNotNull();
        assertThat(new ForgeCommandActor(compiler).getType()).isEqualTo(CommandActorType.UNKNOWN);
    }

    @Test
    void the_console_actor_agrees_with_the_command_check() {
        AtomicReference<String> result = new AtomicReference<>();
        try (CommandTester tester = new CommandTester(new Command("whoami") {{
            execute(ctx -> result.set(ctx.getActor()
                                         .getType()
                    + "," + ctx.getActor()
                               .hasPermission("actortest.whoami")));
        }}, "actortest")) {
            tester.execute("whoami", FakeSender.console());
        }
        assertThat(result.get()).isEqualTo("CONSOLE,true");
    }

    @Test
    void require_console_lets_only_the_console_run() {
        FakeSender console = FakeSender.console();
        FakeSender steve = FakeSender.player("Steve");
        try (CommandTester tester = new CommandTester(new Command("stop") {{
            addPrerequisite(Command.console());
            execute(ctx -> ctx.sendMessage("stopped"));
        }}, "actortest")) {
            tester.execute("stop", console);
            tester.execute("stop", steve);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("stopped");
        assertThat(steve.getSentMessageTexts()).containsExactly("This command can only be executed from the console.");
    }
}
