package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a method run the class's {@link Command} itself. A method without argument parameters is the command's
 * executor; one with them adds an argument branch, which {@link Description} and {@link Permission} on the method
 * configure.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Execute {
}
