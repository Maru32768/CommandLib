package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a method run a subcommand of the class's {@link Command}. The value is the path below the command, separated
 * by spaces, such as {@code "start"} or {@code "arena create"}; missing commands on the path are created. Several
 * methods may share a path: each with argument parameters adds an argument branch to it. The command annotations,
 * such as {@link Description}, {@link Permission}, {@link Confirm}, {@link Cooldown} and {@link #aliases()}, configure
 * the subcommand, and only one method of a path may give them.
 * <p>
 * A placeholder such as {@code <team>} in the path is an argument followed by the rest of the path, as
 * {@code argument(team).child(...)} does: {@code "<team> add"} runs {@code /command <team> add}. The method takes the
 * placeholder's value as its argument parameter of the same name, which every method of the placeholder must declare
 * alike; adjacent placeholders form one argument branch. A path that ends in a placeholder executes that branch: the
 * method then takes no other arguments, and {@link Description}, {@link Permission} and {@link VisibleWhen} describe
 * the branch.
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
