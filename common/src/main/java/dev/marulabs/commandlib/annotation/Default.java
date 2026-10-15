package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The value of an omitted argument, which this makes optional, or of an option that is not given. It is converted to
 * the parameter's type when the class is parsed, and must be a string, a number, {@code true} or {@code false}, or the
 * name of an enum constant.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Default {
    String value();
}
