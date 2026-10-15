package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a method run a subcommand of the class's {@link Command}. The value is the path of literals below the command,
 * separated by spaces, such as {@code "start"} or {@code "arena create"}; missing commands on the path are created.
 * Several methods may share a path: each with argument parameters adds an argument branch to it. {@link Description},
 * {@link Permission}, {@link Confirm}, {@link Cooldown} and {@link #aliases()} configure the subcommand, and only one
 * method of a path may give them.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    String value();

    /**
     * Aliases of the last literal of the path.
     */
    String[] aliases() default {};
}
