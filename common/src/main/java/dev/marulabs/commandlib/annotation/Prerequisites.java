package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Checks the prerequisites registered under these names with {@link AnnotationParser#registerPrerequisite} before the
 * command executes, as {@code addPrerequisite(...)} does, on a {@link Command} class or a {@link Subcommand} method.
 * A prerequisite of the class applies to its subcommands, as one added to a command applies to its children.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Prerequisites {
    String[] value();
}
