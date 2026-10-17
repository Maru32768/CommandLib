package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Allows at most one of the options with these long names in one input, as {@code mutuallyExclusive(...)} does, on a
 * {@link Command} class or a {@link Subcommand} method. Repeat it for several groups.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(MutuallyExclusive.List.class)
public @interface MutuallyExclusive {
    String[] value();

    /**
     * Holds repeated {@link MutuallyExclusive} annotations.
     */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        MutuallyExclusive[] value();
    }
}
