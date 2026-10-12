package dev.marulabs.commandlib.util.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RichTextTest {
    @Test
    void changes_return_new_instances_and_keep_the_original() {
        RichText original = RichText.text("a");

        RichText changed = original.color(0x123456)
                                   .bold()
                                   .hover(RichText.text("h"))
                                   .click(ClickAction.runCommand("/x"))
                                   .append(RichText.text("b"));

        assertThat(original).isEqualTo(RichText.text("a"));
        assertThat(original.color()).isNull();
        assertThat(original.isBold()).isNull();
        assertThat(original.hover()).isNull();
        assertThat(original.click()).isNull();
        assertThat(original.children()).isEmpty();
        assertThat(changed.color()).isEqualTo(0x123456);
        assertThat(changed.isBold()).isTrue();
        assertThat(changed.hover()).isEqualTo(RichText.text("h"));
        assertThat(changed.click()).isEqualTo(ClickAction.runCommand("/x"));
        assertThat(changed.children()).containsExactly(RichText.text("b"));
    }

    @Test
    void decorations_are_tri_state() {
        RichText text = RichText.text("a")
                                .bold()
                                .italic(false)
                                .underlined()
                                .strikethrough(true)
                                .obfuscated(false);

        assertThat(text.isBold()).isTrue();
        assertThat(text.isItalic()).isFalse();
        assertThat(text.isUnderlined()).isTrue();
        assertThat(text.isStrikethrough()).isTrue();
        assertThat(text.isObfuscated()).isFalse();
        assertThat(RichText.text("a")
                           .isItalic()).isNull();
        assertThat(RichText.text("a")
                           .italic(false)).isNotEqualTo(RichText.text("a"));
    }

    @Test
    void values_are_compared_by_content() {
        RichText a = RichText.translatable("key", RichText.text("arg"))
                             .color(0xFF5555)
                             .italic()
                             .append(RichText.text("child"));
        RichText b = RichText.translatable("key", RichText.text("arg"))
                             .color(0xFF5555)
                             .italic()
                             .append(RichText.text("child"));

        assertThat(a).isEqualTo(b)
                     .hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(RichText.translatable("key", RichText.text("other"))
                                           .color(0xFF5555)
                                           .italic()
                                           .append(RichText.text("child")));
        assertThat(RichText.text("key")).isNotEqualTo(RichText.translatable("key"));
        assertThat(a.toString()).isEqualTo(
                "RichText{translatable=\"key\", arguments=[RichText{text=\"arg\"}], color=#ff5555, italic=true, " + "children=[RichText{text=\"child\"}]}");
    }

    @Test
    void plain_text_joins_children_and_uses_translation_keys() {
        RichText text = RichText.text("a")
                                .append(RichText.translatable("key", RichText.text("ignored")),
                                        RichText.text("b")
                                                .append(RichText.text("c")));

        assertThat(text.plainText()).isEqualTo("akeybc");
        assertThat(text.text()).isEqualTo("a");
        assertThat(text.translationKey()).isNull();
        assertThat(text.children()
                       .get(0)
                       .arguments()).containsExactly(RichText.text("ignored"));
    }

    @Test
    void empty_or_null_hover_is_removed() {
        RichText hovered = RichText.text("a")
                                   .hover(RichText.text("h"));

        assertThat(hovered.hover(RichText.text(""))
                          .hover()).isNull();
        assertThat(hovered.hover(null)
                          .hover()).isNull();
        assertThat(RichText.text("a")
                           .hover(RichText.text("")
                                          .append(RichText.text("x")))
                           .hover()).isEqualTo(RichText.text("")
                                                       .append(RichText.text("x")));
    }

    @Test
    void click_and_color_can_be_replaced() {
        RichText text = RichText.text("a")
                                .color(0x111111)
                                .color(0x222222)
                                .click(ClickAction.openUrl("https://example.com"))
                                .click(null);

        assertThat(text.color()).isEqualTo(0x222222);
        assertThat(text.click()).isNull();
        assertThat(RichText.text("a")
                           .color(0xFF123456)
                           .color()).isEqualTo(0x123456);
    }

    @Test
    void click_actions_keep_type_and_value() {
        assertThat(ClickAction.runCommand("/a")
                              .type()).isEqualTo(ClickAction.Type.RUN_COMMAND);
        assertThat(ClickAction.suggestCommand("/b")
                              .type()).isEqualTo(ClickAction.Type.SUGGEST_COMMAND);
        assertThat(ClickAction.copyToClipboard("c")
                              .type()).isEqualTo(ClickAction.Type.COPY_TO_CLIPBOARD);
        assertThat(ClickAction.openUrl("https://example.com")
                              .value()).isEqualTo("https://example.com");
        assertThat(ClickAction.runCommand("/a")).isEqualTo(ClickAction.runCommand("/a"))
                                                .isNotEqualTo(ClickAction.suggestCommand("/a"));
    }

    @Test
    void null_content_is_rejected() {
        assertThatThrownBy(() -> RichText.text(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RichText.translatable("key", (RichText) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ClickAction.runCommand(null)).isInstanceOf(NullPointerException.class);
    }
}
