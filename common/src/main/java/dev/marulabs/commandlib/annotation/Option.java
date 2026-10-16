package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a parameter a value option, such as {@code --limit 10}. A {@code boolean}, {@code int}, {@code long},
 * {@code float}, {@code double} or {@code String} parameter is a value option of that type, whose value when the option
 * is not given is {@link Default} or else {@code false}, {@code 0} or an empty string. A parameter of any other type
 * takes the argument registered for the type as its value, as {@code Options.of(name, argument)} does, with
 * {@link Suggest} and the registered annotations; it is required unless {@link Default} or {@link Optional} makes the
 * argument optional. A {@code List} parameter takes the option up to {@link #repeatable()} times.
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
