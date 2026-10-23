package dev.marulabs.commandlib.annotation;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.argument.*;
import org.jetbrains.annotations.NotNull;


/**
 * Builds commands from classes annotated with {@link dev.marulabs.commandlib.annotation.Command}. See
 * {@link AnnotationParser} for the mapping.
 */
public final class AnnotatedCommands {
    private AnnotatedCommands() {
    }

    /**
     * Builds the command that {@code instance}'s class declares, with the parser of {@link #parser()}.
     *
     * @throws IllegalArgumentException if the class is not annotated or a method cannot be mapped
     */
    @NotNull
    public static Command parse(@NotNull Object instance) {
        return parser().parse(instance);
    }

    /**
     * Returns a new parser that maps the types of the built-in arguments, such as {@code int} to
     * {@link IntegerArgument}, {@link Player} to {@link PlayerArgument} and {@link RegisteredServer} to
     * {@link ServerArgument}, and resolves {@link CommandSource}, {@link ConsoleCommandSource} and a {@link Sender} or
     * omitted {@link Optional} {@link Player} from the sender. Register further types and annotations on it before
     * parsing.
     */
    @NotNull
    public static AnnotationParser<CommandContext, Command> parser() {
        return new AnnotationParser<CommandContext, Command>(CommandContext.class,
                                                             Command::new).registerStandardArguments(StringArgument::new,
                                                                                                     name -> new StringArgument(name,
                                                                                                                                StringArgument.Type.PHRASE),
                                                                                                     IntegerArgument::new,
                                                                                                     LongArgument::new,
                                                                                                     FloatArgument::new,
                                                                                                     DoubleArgument::new,
                                                                                                     BooleanArgument::new,
                                                                                                     EnumArgument::new,
                                                                                                     DurationArgument::new)
                                                                          .registerArgument(Player.class,
                                                                                            p -> new PlayerArgument(p.name()))
                                                                          .registerArgument(RegisteredServer.class,
                                                                                            p -> new ServerArgument(p.name()))
                                                                          .registerContextValue(CommandSource.class,
                                                                                                CommandContext::getSender)
                                                                          .registerContextValue(Player.class,
                                                                                                Command.player())
                                                                          .registerContextValue(ConsoleCommandSource.class,
                                                                                                Command.console());
    }
}
