package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Checks the prerequisites registered under these names with {@link AnnotationParser#registerCondition} before the
 * command executes, as {@code addPrerequisite(...)} does, on a {@link Command} class or a {@link Subcommand} method.
 * Like a prerequisite, a condition of the class applies to its subcommands.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Condition {
    String[] value();
}
