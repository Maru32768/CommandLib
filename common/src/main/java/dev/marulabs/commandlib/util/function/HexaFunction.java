package dev.marulabs.commandlib.util.function;

public interface HexaFunction<A, B, C, D, E, F, R> {
    R apply(A a, B b, C c, D d, E e, F f);
}
