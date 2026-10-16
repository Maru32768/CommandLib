package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Adds the middlewares registered under these names with {@link AnnotationParser#registerMiddleware}, in this order,
 * as {@code addMiddleware(...)} does, on a {@link Command} class or a {@link Subcommand} method. Like a middleware,
 * one of the class applies to its subcommands.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Middleware {
    String[] value();
}
