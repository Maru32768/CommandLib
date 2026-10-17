package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Gives the command the captions of {@code Captions.create().bundle(value, classLoader)}, with the class loader of the
 * parsed class, as {@code captions(...)} does, on a {@link Command} class or a {@link Subcommand} method. The value is
 * the base name of the properties files, such as {@code "lang/homes"} for {@code lang/homes_ja.properties}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CaptionBundle {
    String value();
}
