package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.DefaultPermission;

import java.lang.annotation.*;

/**
 * Overrides the permission of the command on a {@link Command} class, of the subcommand on a {@link Subcommand}
 * method, of the argument branch on an {@link Execute} method with arguments or a {@link Subcommand} method whose path
 * ends in a placeholder. Each attribute left out keeps the generated or inherited value.
 * <p>
 * On a {@link Flag} or {@link Option} parameter, it gives the option its own node, as the option's
 * {@code permission(...)} does: the node {@link #value()}, or the generated {@code <command node>.option.<name>}, with
 * {@link #defaultPermission()} or the command's default. It needs a node or a default there, and takes no description.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Permission {
    /**
     * The permission node, or empty for the generated one.
     */
    String value() default "";

    /**
     * Who has the permission by default: one value, or none to inherit it.
     */
    DefaultPermission[] defaultPermission() default {};

    /**
     * The description of the permission node.
     */
    String description() default "";
}
