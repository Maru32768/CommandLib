package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a {@link Flag} or {@link Option} parameter need another option of the command when it is given, as
 * {@code requires(other)} does, or need the other option's value to be {@link #equalTo()}, as
 * {@code requires(other, value)} does. Repeat it for several requirements.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Repeatable(Requires.List.class)
public @interface Requires {
    /**
     * The long name of the other option.
     */
    String value();

    /**
     * The value the other option must have, converted like {@link Default}: one value, or none to need the other
     * option to be given.
     */
    String[] equalTo() default {};

    /**
     * Holds repeated {@link Requires} annotations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface List {
        Requires[] value();
    }
}
