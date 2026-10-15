package dev.marulabs.commandlib.util.nms.argument;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NMSArgumentTest {
    @Test
    void boundReadsTheDefaultMethodsOfTheBoundsInterface() {
        Doubles bounds = new Doubles(Optional.of(0.5), Optional.empty());

        assertThat(NMSArgument.bound(bounds, "min")).isEqualTo(0.5);
        assertThat(NMSArgument.bound(bounds, "max")).isNull();
        assertThat(NMSArgumentFloatRange.doubleRange((Number) NMSArgument.bound(bounds, "min"),
                                                     (Number) NMSArgument.bound(bounds, "max"))
                                        .toString()).isEqualTo("[0.5..+∞)");
    }

    /**
     * The shape of MinMaxBounds from 1.20.2: an interface whose min() and max() are default methods over its bounds.
     */
    public interface MinMaxBounds<T> {
        Optional<T> lower();

        Optional<T> upper();

        default Optional<T> min() {
            return lower();
        }

        default Optional<T> max() {
            return upper();
        }
    }

    public static final class Doubles implements MinMaxBounds<Double> {
        private final Optional<Double> lower;
        private final Optional<Double> upper;

        Doubles(Optional<Double> lower, Optional<Double> upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public Optional<Double> lower() {
            return lower;
        }

        @Override
        public Optional<Double> upper() {
            return upper;
        }
    }
}
