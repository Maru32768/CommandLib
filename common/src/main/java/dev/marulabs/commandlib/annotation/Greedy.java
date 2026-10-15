package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a string argument take the rest of the input, spaces included, as {@code StringArgument.Type.PHRASE} does.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Greedy {
}
