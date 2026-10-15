package dev.marulabs.commandlib;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a {@link FakeSender} parameter that {@link CommandLibExtension} injects. Without it, the parameter is a
 * non-operator player named {@code Player} that has every permission.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {
    /**
     * The player's name. Ignored for the console.
     */
    String value() default "Player";

    /**
     * Whether the sender is the console, which is an operator.
     */
    boolean console() default false;

    /**
     * Whether a player is an operator.
     */
    boolean op() default false;

    /**
     * Grants only these permission nodes. Empty grants every permission.
     */
    String[] permissions() default {};

    /**
     * Denies these permission nodes, even to operators.
     */
    String[] denyPermissions() default {};
}
