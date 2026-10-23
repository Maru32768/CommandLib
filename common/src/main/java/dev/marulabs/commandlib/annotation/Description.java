package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The description shown in the generated help: of the command on a {@link Command} class, of the subcommand on a
 * {@link Subcommand} method, of the argument branch on an {@link Execute} method with arguments or a
 * {@link Subcommand} method whose path ends in a placeholder, and of the option on a {@link Flag} or {@link Option}
 * parameter.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Description {
    /**
     * The text, or with {@link #caption()} its English default.
     */
    String value();

    /**
     * The key of a caption that gives the text in the sender's locale, as
     * {@code description(Caption.of(caption, value))} does, or empty for the fixed text. Options
     * have no localized description.
     */
    String caption() default "";
}
