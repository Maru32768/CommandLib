package net.kunmc.lab.commandlib.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatColorUtilTest {
    @Test
    void color_rgb_excludes_alpha_channel() {
        assertThat(ChatColorUtil.RED.getRGB()).isEqualTo(0xFF5555);
        assertThat(ChatColorUtil.BLACK.getRGB()).isEqualTo(0x000000);
        assertThat(ChatColorUtil.WHITE.getRGB()).isEqualTo(0xFFFFFF);
    }

    @Test
    void formatting_codes_have_no_rgb() {
        assertThat(ChatColorUtil.BOLD.getRGB()).isNull();
        assertThat(ChatColorUtil.RESET.getRGB()).isNull();
    }

    @Test
    void to_string_is_legacy_color_code() {
        assertThat(ChatColorUtil.GRAY).hasToString("§7");
        assertThat(ChatColorUtil.UNDERLINE).hasToString("§n");
    }

    @Test
    void colors_with_same_code_are_equal() {
        assertThat(ChatColorUtil.RED).isEqualTo(ChatColorUtil.RED)
                                     .isNotEqualTo(ChatColorUtil.DARK_RED)
                                     .hasSameHashCodeAs(ChatColorUtil.RED);
    }
}
