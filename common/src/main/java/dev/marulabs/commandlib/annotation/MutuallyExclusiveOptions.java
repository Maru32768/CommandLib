package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Allows at most one of the options with these long names in one input, as {@code mutuallyExclusiveOptions(...)} does, on a
 * {@link Command} class or a {@link Subcommand} method. Repeat it for several groups.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(MutuallyExclusiveOptions.List.class)
public @interface MutuallyExclusiveOptions {
    String[] value();

    /**
     * Holds repeated {@link MutuallyExclusiveOptions} annotations.
     */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        MutuallyExclusiveOptions[] value();
    }
}
