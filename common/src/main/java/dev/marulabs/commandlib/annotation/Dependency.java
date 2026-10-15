package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Sets a field of the command class, when the class is parsed, to the instance registered for the field's type with
 * {@link AnnotationParser#registerDependency}. The field must be neither static nor final; fields of superclasses are
 * set too. Passing the dependencies to the constructor does the same without reflection.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependency {
}
