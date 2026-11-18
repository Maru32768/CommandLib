package dev.marulabs.cadence.util.function;

public interface QuintFunction<A, B, C, D, E, R> {
    R apply(A a, B b, C c, D d, E e);
}
