package dev.marulabs.cadence.util.function;

public interface TetraFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}
