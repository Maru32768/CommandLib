package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the argument for a method parameter of the type it is registered for with
 * {@link AnnotationParser#registerArgument}. The argument's value must be of {@link AnnotatedParameter#type()}.
 */
@FunctionalInterface
public interface ArgumentFactory<C extends CommonCommandContext<?, ?>> {
    @NotNull
    CommonArgument<?, C, ?> create(@NotNull AnnotatedParameter parameter);
}
