package net.kunmc.lab.commandlib.util.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RichTextsTest {
    @Test
    void legacy_colors_become_named_colors_and_others_stay_hex() {
        assertThat(RichTexts.toAdventure(RichText.text("a")
                                                 .color(0xFF5555))
                            .color()).isSameAs(NamedTextColor.RED);
        assertThat(RichTexts.toAdventure(RichText.text("a")
                                                 .color(0x123456))
                            .color()).isEqualTo(TextColor.color(0x123456));
        assertThat(RichTexts.toAdventure(RichText.text("a"))
                            .color()).isNull();
    }

    @Test
    void decorations_keep_tri_state() {
        Component component = RichTexts.toAdventure(RichText.text("a")
                                                            .bold()
                                                            .italic(false)
                                                            .underlined()
                                                            .strikethrough(false)
                                                            .obfuscated());

        assertThat(component.decoration(TextDecoration.BOLD)).isEqualTo(TextDecoration.State.TRUE);
        assertThat(component.decoration(TextDecoration.ITALIC)).isEqualTo(TextDecoration.State.FALSE);
        assertThat(component.decoration(TextDecoration.UNDERLINED)).isEqualTo(TextDecoration.State.TRUE);
        assertThat(component.decoration(TextDecoration.STRIKETHROUGH)).isEqualTo(TextDecoration.State.FALSE);
        assertThat(component.decoration(TextDecoration.OBFUSCATED)).isEqualTo(TextDecoration.State.TRUE);
        assertThat(RichTexts.toAdventure(RichText.text("a"))
                            .decoration(TextDecoration.BOLD)).isEqualTo(TextDecoration.State.NOT_SET);
    }

    @Test
    void hover_text_is_converted() {
        Component component = RichTexts.toAdventure(RichText.text("a")
                                                            .hover(RichText.text("details")
                                                                           .color(0x55FF55)));

        assertThat(component.hoverEvent()).isEqualTo(HoverEvent.showText(Component.text("details",
                                                                                        NamedTextColor.GREEN)));
        assertThat(RichTexts.toAdventure(RichText.text("a")
                                                 .hover(RichText.text("")))
                            .hoverEvent()).isNull();
    }

    @Test
    void every_click_action_is_converted() {
        assertThat(click(ClickAction.runCommand("/a"))).isEqualTo(ClickEvent.runCommand("/a"));
        assertThat(click(ClickAction.suggestCommand("/b"))).isEqualTo(ClickEvent.suggestCommand("/b"));
        assertThat(click(ClickAction.copyToClipboard("c"))).isEqualTo(ClickEvent.copyToClipboard("c"));
        assertThat(click(ClickAction.openUrl("https://example.com"))).isEqualTo(ClickEvent.openUrl(
                "https://example.com"));
    }

    @Test
    void translatable_arguments_and_children_are_converted() {
        Component component = RichTexts.toAdventure(RichText.translatable("commands.give.success",
                                                                          RichText.text("x")
                                                                                  .bold())
                                                            .append(RichText.text("!")
                                                                            .italic()));

        assertThat(component).isEqualTo(Component.translatable("commands.give.success",
                                                               Component.text("x")
                                                                        .decorate(TextDecoration.BOLD))
                                                  .append(Component.text("!")
                                                                   .decorate(TextDecoration.ITALIC)));
    }

    private static ClickEvent click(ClickAction action) {
        return RichTexts.toAdventure(RichText.text("a")
                                             .click(action))
                        .clickEvent();
    }
}
