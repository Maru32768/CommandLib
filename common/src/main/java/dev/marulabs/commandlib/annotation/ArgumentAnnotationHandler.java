package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

/**
 * Applies a custom annotation of an argument parameter, registered with {@link AnnotationParser#registerAnnotation}.
 * It returns the argument to use: the one it gets, configured with its fluent methods such as {@code validator}, or a
 * new argument of the same name.
 *
 * <pre>{@code
 * parser.registerAnnotation(Online.class, Player.class, (online, argument) -> argument.validator(Player::isOnline));
 * }</pre>
 */
@FunctionalInterface
public interface ArgumentAnnotationHandler<A extends Annotation, V, C extends CommonCommandContext<?, ?>> {
    @NotNull
    CommonArgument<V, C, ?> apply(@NotNull A annotation, @NotNull CommonArgument<V, C, ?> argument);
}
