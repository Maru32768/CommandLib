package dev.marulabs.commandlib.util.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Converts {@link RichText} to Adventure components.
 */
public final class RichTexts {
    private RichTexts() {
    }

    /**
     * Returns the Adventure component for {@code text}. The 16 legacy colors become {@link NamedTextColor}s.
     */
    @NotNull
    public static Component toAdventure(@NotNull RichText text) {
        Component component;
        if (text.text() != null) {
            component = Component.text(text.text());
        } else {
            component = Component.translatable(text.translationKey(),
                                               text.arguments()
                                                   .stream()
                                                   .map(RichTexts::toAdventure)
                                                   .collect(Collectors.toList()));
        }
        if (text.color() != null) {
            NamedTextColor named = NamedTextColor.namedColor(text.color());
            component = component.color(named != null ? named : TextColor.color(text.color()));
        }
        component = component.decoration(TextDecoration.BOLD, TextDecoration.State.byBoolean(text.isBold()))
                             .decoration(TextDecoration.ITALIC, TextDecoration.State.byBoolean(text.isItalic()))
                             .decoration(TextDecoration.UNDERLINED, TextDecoration.State.byBoolean(text.isUnderlined()))
                             .decoration(TextDecoration.STRIKETHROUGH,
                                         TextDecoration.State.byBoolean(text.isStrikethrough()))
                             .decoration(TextDecoration.OBFUSCATED,
                                         TextDecoration.State.byBoolean(text.isObfuscated()));
        if (text.hover() != null) {
            component = component.hoverEvent(HoverEvent.showText(toAdventure(text.hover())));
        }
        if (text.click() != null) {
            component = component.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.valueOf(text.click()
                                                                                                 .type()
                                                                                                 .name()),
                                                                   text.click()
                                                                       .value()));
        }
        return component.children(text.children()
                                      .stream()
                                      .map(RichTexts::toAdventure)
                                      .collect(Collectors.toList()));
    }
}
