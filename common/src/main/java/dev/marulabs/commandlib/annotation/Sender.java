package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Resolves a parameter with the value registered for its type by {@link AnnotationParser#registerContextValue}, such
 * as the sender as a player, instead of as an argument of the same type. A parameter whose type has no argument needs
 * no annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {
}
