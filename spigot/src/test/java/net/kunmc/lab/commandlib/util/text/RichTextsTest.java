package net.kunmc.lab.commandlib.util.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

class RichTextsTest {
    @Test
    void legacy_colors_become_named_colors_and_others_stay_hex() {
        assertThat(RichTexts.toBungee(RichText.text("a")
                                              .color(0xFF5555))
                            .getColorRaw()).isEqualTo(ChatColor.RED);
        assertThat(RichTexts.toBungee(RichText.text("a")
                                              .color(0x123456))
                            .getColorRaw()).isEqualTo(ChatColor.of(new Color(0x123456)));
        assertThat(RichTexts.toBungee(RichText.text("a"))
                            .getColorRaw()).isNull();
    }

    @Test
    void decorations_keep_tri_state() {
        BaseComponent component = RichTexts.toBungee(RichText.text("a")
                                                             .bold()
                                                             .italic(false)
                                                             .underlined()
                                                             .strikethrough(false)
                                                             .obfuscated());

        assertThat(component.isBoldRaw()).isTrue();
        assertThat(component.isItalicRaw()).isFalse();
        assertThat(component.isUnderlinedRaw()).isTrue();
        assertThat(component.isStrikethroughRaw()).isFalse();
        assertThat(component.isObfuscatedRaw()).isTrue();
        assertThat(RichTexts.toBungee(RichText.text("a"))
                            .isBoldRaw()).isNull();
    }

    @Test
    void hover_text_is_converted() {
        BaseComponent component = RichTexts.toBungee(RichText.text("a")
                                                             .hover(RichText.text("details")
                                                                            .color(0x55FF55)));

        assertThat(component.getHoverEvent()
                            .getAction()).isEqualTo(HoverEvent.Action.SHOW_TEXT);
        BaseComponent[] hover = (BaseComponent[]) ((Text) component.getHoverEvent()
                                                                   .getContents()
                                                                   .get(0)).getValue();
        assertThat(hover[0].toPlainText()).isEqualTo("details");
        assertThat(hover[0].getColorRaw()).isEqualTo(ChatColor.GREEN);
        assertThat(RichTexts.toBungee(RichText.text("a")
                                              .hover(RichText.text("")))
                            .getHoverEvent()).isNull();
    }

    @Test
    void every_click_action_is_converted() {
        assertThat(click(ClickAction.runCommand("/a"))).isEqualTo(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                                                 "/a"));
        assertThat(click(ClickAction.suggestCommand("/b"))).isEqualTo(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                                                     "/b"));
        assertThat(click(ClickAction.copyToClipboard("c"))).isEqualTo(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                                                                                     "c"));
        assertThat(click(ClickAction.openUrl("https://example.com"))).isEqualTo(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                                                               "https://example.com"));
    }

    @Test
    void translatable_arguments_and_children_are_converted() {
        BaseComponent component = RichTexts.toBungee(RichText.translatable("commands.give.success",
                                                                           RichText.text("x")
                                                                                   .bold())
                                                             .append(RichText.text("!")
                                                                             .italic()));

        assertThat(component).isInstanceOf(TranslatableComponent.class);
        TranslatableComponent translatable = (TranslatableComponent) component;
        assertThat(translatable.getTranslate()).isEqualTo("commands.give.success");
        assertThat(translatable.getWith()).hasSize(1);
        assertThat(translatable.getWith()
                               .get(0)
                               .toPlainText()).isEqualTo("x");
        assertThat(translatable.getWith()
                               .get(0)
                               .isBoldRaw()).isTrue();
        assertThat(component.getExtra()).hasSize(1);
        assertThat(component.getExtra()
                            .get(0)).isInstanceOf(TextComponent.class);
        assertThat(component.getExtra()
                            .get(0)
                            .isItalicRaw()).isTrue();
    }

    private static ClickEvent click(ClickAction action) {
        return RichTexts.toBungee(RichText.text("a")
                                          .click(action))
                        .getClickEvent();
    }
}
