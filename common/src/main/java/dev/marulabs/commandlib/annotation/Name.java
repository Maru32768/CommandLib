package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The name of an argument, shown in help and joined into its branch's permission node. Without it the parameter name
 * is used, which needs the class to be compiled with {@code -parameters}, or {@code -java-parameters} for Kotlin.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Name {
    String value();
}
