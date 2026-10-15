package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a parameter a value option, such as {@code --limit 10}, of type {@code boolean}, {@code int}, {@code long},
 * {@code float}, {@code double} or {@code String}. {@link Default} gives the value when the option is not given,
 * otherwise it is {@code false}, {@code 0} or an empty string. A {@code List} parameter takes the option up to
 * {@link #repeatable()} times.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Option {
    /**
     * The long name, or empty for the parameter name.
     */
    String value() default "";

    /**
     * The short name, or {@code '\0'} for none.
     */
    char shortName() default '\0';

    String[] aliases() default {};

    boolean required() default false;

    /**
     * How many times the option can be given; required for a {@code List} parameter and only allowed there.
     */
    int repeatable() default 0;
}
