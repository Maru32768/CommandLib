package dev.marulabs.commandlib.annotation;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.argument.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

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
                                                             Command::new).registerArgument(String.class,
                                                                                            p -> p.annotation(Greedy.class) == null
                                                                                                    ? new StringArgument(p.name())
                                                                                                    : new StringArgument(p.name(),
                                                                                                                         StringArgument.Type.PHRASE))
                                                                          .registerArgument(Integer.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new IntegerArgument(p.name())
                                                                                      : new IntegerArgument(p.name(),
                                                                                                            (int) range.min(),
                                                                                                            (int) range.max());
                                                                          })
                                                                          .registerArgument(Long.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new LongArgument(p.name())
                                                                                      : new LongArgument(p.name(),
                                                                                                         (long) range.min(),
                                                                                                         (long) range.max());
                                                                          })
                                                                          .registerArgument(Float.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new FloatArgument(p.name())
                                                                                      : new FloatArgument(p.name(),
                                                                                                          (float) Math.max(range.min(),
                                                                                                                           -Float.MAX_VALUE),
                                                                                                          (float) Math.min(range.max(),
                                                                                                                           Float.MAX_VALUE));
                                                                          })
                                                                          .registerArgument(Double.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new DoubleArgument(p.name())
                                                                                      : new DoubleArgument(p.name(),
                                                                                                           range.min(),
                                                                                                           range.max());
                                                                          })
                                                                          .registerArgument(Boolean.class,
                                                                                            p -> new BooleanArgument(p.name()))
                                                                          .registerArgument(Enum.class,
                                                                                            AnnotatedCommands::enumArgument)
                                                                          .registerArgument(Duration.class,
                                                                                            p -> new DurationArgument(p.name()))
                                                                          .registerArgument(Player.class,
                                                                                            p -> new PlayerArgument(p.name()))
                                                                          .registerArgument(RegisteredServer.class,
                                                                                            p -> new ServerArgument(p.name()))
                                                                          .registerContextValue(CommandSource.class,
                                                                                                CommandContext::getSender)
                                                                          .registerContextValue(Player.class,
                                                                                                Command.playerRequirer())
                                                                          .registerContextValue(ConsoleCommandSource.class,
                                                                                                Command.consoleRequirer());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommonArgument<?, CommandContext, ?> enumArgument(AnnotatedParameter parameter) {
        return new EnumArgument(parameter.name(), parameter.type());
    }
}
