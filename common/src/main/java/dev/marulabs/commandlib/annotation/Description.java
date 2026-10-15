package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The description shown in the generated help: of the command on a {@link Command} class, of the subcommand on a
 * {@link Subcommand} method, of the argument branch on an {@link Execute} method with arguments, and of the option on
 * a {@link Flag} or {@link Option} parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Description {
    String value();
}
