package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Shows the command or branch only to senders for whom the predicate registered under this name with
 * {@link AnnotationParser#registerVisibility} holds, as {@code visibleWhen(...)} does: the command on a
 * {@link Command} class or a {@link Subcommand} method, and the argument branch on an {@link Execute} method with
 * arguments or a {@link Subcommand} method whose path ends in a placeholder.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface VisibleWhen {
    String value();
}
