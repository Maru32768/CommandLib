package dev.marulabs.commandlib;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a {@link FakeSender} parameter that {@link CommandLibExtension} injects. Without it, the parameter is a
 * player named {@code Player} that has every permission. Velocity has no operators, so unlike the other testing
 * artifacts there is no {@code op}.
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
     * Whether the sender is the console.
     */
    boolean console() default false;

    /**
     * Grants only these permission nodes and leaves the others unset, so only those whose default is
     * {@link DefaultPermission#ALL} are granted. Empty grants every permission.
     */
    String[] permissions() default {};

    /**
     * Denies these permission nodes.
     */
    String[] denyPermissions() default {};
}
