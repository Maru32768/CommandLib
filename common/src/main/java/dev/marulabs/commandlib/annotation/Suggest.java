package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Replaces the suggestions of an argument with fixed values, the values of a method of the annotated class, or both.
 * The method, named by {@link #method()}, either takes no parameters and returns an {@code Iterable<String>}, or takes
 * the {@link dev.marulabs.commandlib.suggestion.SuggestionBuilder} and returns nothing.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Suggest {
    String[] value() default {};

    String method() default "";
}
