package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes an argument parameter optional. Without {@link Default}, an omitted argument gives the value registered for
 * its type with {@link AnnotationParser#registerContextValue}, such as the sender for a player, or else {@code 0},
 * {@code false}, an empty string or, for a {@code List}, an empty list. Only the last arguments of a method can be
 * optional.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {
}
