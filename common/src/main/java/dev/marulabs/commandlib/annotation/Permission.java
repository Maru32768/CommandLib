package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.DefaultPermission;

import java.lang.annotation.*;

/**
 * Overrides the permission of the command on a {@link Command} class, of the subcommand on a {@link Subcommand}
 * method, or of the argument branch on an {@link Execute} method with arguments. Each attribute left out keeps the
 * generated or inherited value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
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
