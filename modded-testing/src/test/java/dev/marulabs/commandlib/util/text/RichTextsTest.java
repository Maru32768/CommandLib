package dev.marulabs.commandlib.util.text;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
//? if >=1.19 {
import net.minecraft.network.chat.contents.TranslatableContents;
//?} else
/*import net.minecraft.network.chat.TranslatableComponent;*/
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RichTextsTest {
    @Test
    void legacy_colors_become_named_colors_and_others_stay_hex() {
        assertThat(RichTexts.toVanilla(RichText.text("a")
                                               .color(0xFF5555))
                            .getStyle()
                            .getColor()
                            .serialize()).isEqualTo("red");
        assertThat(RichTexts.toVanilla(RichText.text("a")
                                               .color(0x123456))
                            .getStyle()
                            .getColor()
                            .serialize()).isEqualToIgnoringCase("#123456");
        assertThat(RichTexts.toVanilla(RichText.text("a"))
                            .getStyle()
                            .getColor()).isNull();
    }

    @Test
    void decorations_keep_tri_state() {
        Style style = RichTexts.toVanilla(RichText.text("a")
                                                  .bold()
                                                  .italic(false)
                                                  .underlined()
                                                  .strikethrough()
                                                  .obfuscated())
                               .getStyle();

        assertThat(style.isBold()).isTrue();
        assertThat(style.isItalic()).isFalse();
        assertThat(style.isUnderlined()).isTrue();
        assertThat(style.isStrikethrough()).isTrue();
        assertThat(style.isObfuscated()).isTrue();
        // An explicit false is kept apart from "not set", so it cancels a decoration inherited from the parent.
        Style italicParent = Style.EMPTY.withItalic(true);
        assertThat(RichTexts.toVanilla(RichText.text("a")
                                               .italic(false))
                            .getStyle()
                            .applyTo(italicParent)
                            .isItalic()).isFalse();
        assertThat(RichTexts.toVanilla(RichText.text("a"))
                            .getStyle()
                            .applyTo(italicParent)
                            .isItalic()).isTrue();
    }

    @Test
    void hover_text_is_converted() {
        MutableComponent component = RichTexts.toVanilla(RichText.text("a")
                                                                 .hover(RichText.text("details")));

        Component hover = component.getStyle()
                                   .getHoverEvent()
                                   .getValue(HoverEvent.Action.SHOW_TEXT);
        assertThat(hover.getString()).isEqualTo("details");
        assertThat(RichTexts.toVanilla(RichText.text("a")
                                               .hover(RichText.text("")))
                            .getStyle()
                            .getHoverEvent()).isNull();
    }

    @Test
    void every_click_action_is_converted() {
        assertThat(click(ClickAction.runCommand("/a"))).isEqualTo(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/a"));
        assertThat(click(ClickAction.suggestCommand("/b"))).isEqualTo(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                                                     "/b"));
        assertThat(click(ClickAction.copyToClipboard("c"))).isEqualTo(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                                                                                     "c"));
        assertThat(click(ClickAction.openUrl("https://example.com"))).isEqualTo(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                                                               "https://example.com"));
    }

    @Test
    void translatable_arguments_and_children_are_converted() {
        MutableComponent component = RichTexts.toVanilla(RichText.translatable("commands.give.success",
                                                                               RichText.text("x"))
                                                                 .append(RichText.text("!")
                                                                                 .italic()));

        //? if >=1.19 {
        TranslatableContents translatable = (TranslatableContents) component.getContents();
        //?} else
        /*TranslatableComponent translatable = (TranslatableComponent) component;*/
        assertThat(translatable.getKey()).isEqualTo("commands.give.success");
        assertThat(translatable.getArgs()).hasSize(1);
        assertThat(((Component) translatable.getArgs()[0]).getString()).isEqualTo("x");
        assertThat(component.getSiblings()).hasSize(1);
        assertThat(component.getSiblings()
                            .get(0)
                            .getStyle()
                            .isItalic()).isTrue();
    }

    private static ClickEvent click(ClickAction action) {
        return RichTexts.toVanilla(RichText.text("a")
                                           .click(action))
                        .getStyle()
                        .getClickEvent();
    }
}
