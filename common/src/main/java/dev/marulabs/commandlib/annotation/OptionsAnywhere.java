package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Lets the options of the command follow its arguments too, as {@code optionsAnywhere()} does, on a {@link Command}
 * class or a {@link Subcommand} method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OptionsAnywhere {
}
