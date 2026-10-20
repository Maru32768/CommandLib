package dev.marulabs.commandlib.internal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilTest {
    @Test
    void returns_true_for_exact_match() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("Hello", "Hello"))
                  .isTrue();
    }

    @Test
    void is_case_insensitive() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("Hello World", "hello"))
                  .isTrue();
        Assertions.assertThat(StringUtil.containsIgnoreCase("hello world", "HELLO"))
                  .isTrue();
    }

    @Test
    void returns_true_when_found_in_middle() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("abcdef", "cde"))
                  .isTrue();
    }

    @Test
    void returns_false_when_not_found() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("Hello", "World"))
                  .isFalse();
    }

    @Test
    void returns_false_when_s_is_null() {
        Assertions.assertThat(StringUtil.containsIgnoreCase(null, "Hello"))
                  .isFalse();
    }

    @Test
    void returns_false_when_search_is_null() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("Hello", null))
                  .isFalse();
    }

    @Test
    void returns_true_for_empty_search() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("Hello", ""))
                  .isTrue();
    }

    @Test
    void returns_true_when_both_are_empty() {
        Assertions.assertThat(StringUtil.containsIgnoreCase("", ""))
                  .isTrue();
    }
}
