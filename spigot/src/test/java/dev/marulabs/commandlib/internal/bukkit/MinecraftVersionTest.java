package dev.marulabs.commandlib.internal.bukkit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MinecraftVersionTest {
    @Nested
    class CompareMinecraftVersion {
        @Test
        void returns_zero_with_same_values() {
            int res = new MinecraftVersion("1.16.5").compareTo(new MinecraftVersion("1.16.5"));
            Assertions.assertThat(res)
                      .isEqualTo(0);
        }

        @Test
        void returns_negative_when_x_is_lower() {
            int res = new MinecraftVersion("1.16.5").compareTo(new MinecraftVersion("1.20.4"));
            Assertions.assertThat(res)
                      .isLessThan(0);
        }

        @Test
        void returns_positive_when_x_is_greater() {
            int res = new MinecraftVersion("1.20.4").compareTo(new MinecraftVersion("1.16.5"));
            Assertions.assertThat(res)
                      .isGreaterThan(0);
        }

        @Test
        void without_patch_version() {
            int res = new MinecraftVersion("1.16").compareTo(new MinecraftVersion("1.16"));
            Assertions.assertThat(res)
                      .isEqualTo(0);
        }
    }

    @Test
    void isWithin() {
        Assertions.assertThat(new MinecraftVersion("1.16.5").isWithin(new MinecraftVersion("1.16.4"),
                                                                      new MinecraftVersion("1.16.6")))
                  .isEqualTo(true);

        Assertions.assertThat(new MinecraftVersion("1.16.5").isWithin(new MinecraftVersion("1.16.5"),
                                                                      new MinecraftVersion("1.16.5")))
                  .isEqualTo(true);

        Assertions.assertThat(new MinecraftVersion("1.16.3").isWithin(new MinecraftVersion("1.16.4"),
                                                                      new MinecraftVersion("1.16.6")))
                  .isEqualTo(false);

        Assertions.assertThat(new MinecraftVersion("1.16.7").isWithin(new MinecraftVersion("1.16.4"),
                                                                      new MinecraftVersion("1.16.6")))
                  .isEqualTo(false);
    }
}
