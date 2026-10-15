package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Declares the top-level command a class defines, for {@link AnnotationParser#parse(Object)}. Its methods annotated
 * {@link Execute} run the command itself, and those annotated {@link Subcommand} run its subcommands. Put
 * {@link Description}, {@link Permission}, {@link Confirm} and {@link Cooldown} on the class to configure the command.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * The name of the command.
     */
    String value();

    String[] aliases() default {};
}
