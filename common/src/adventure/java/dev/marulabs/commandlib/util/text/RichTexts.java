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
// Shared by paper and velocity, which add this directory to their sources.
public final class RichTexts {
    private RichTexts() {
    }

    /**
     * Returns the Adventure component for {@code text}. The 16 legacy colors become {@link NamedTextColor}s.
     */
    @NotNull
    public static Component toAdventure(@NotNull RichText text) {
        Component component;
        if (text instanceof TextRichText literal) {
            component = Component.text(literal.content());
        } else {
            TranslatableRichText translatable = (TranslatableRichText) text;
            component = Component.translatable(translatable.key(),
                                               translatable.arguments()
                                                           .stream()
                                                           .map(RichTexts::toAdventure)
                                                           .collect(Collectors.toList()));
        }
        Integer color = text.color();
        if (color != null) {
            NamedTextColor named = NamedTextColor.namedColor(color);
            component = component.color(named != null ? named : TextColor.color(color));
        }
        component = component.decoration(TextDecoration.BOLD, TextDecoration.State.byBoolean(text.isBold()))
                             .decoration(TextDecoration.ITALIC, TextDecoration.State.byBoolean(text.isItalic()))
                             .decoration(TextDecoration.UNDERLINED, TextDecoration.State.byBoolean(text.isUnderlined()))
                             .decoration(TextDecoration.STRIKETHROUGH,
                                         TextDecoration.State.byBoolean(text.isStrikethrough()))
                             .decoration(TextDecoration.OBFUSCATED,
                                         TextDecoration.State.byBoolean(text.isObfuscated()));
        RichText hover = text.hover();
        if (hover != null) {
            component = component.hoverEvent(HoverEvent.showText(toAdventure(hover)));
        }
        ClickAction click = text.click();
        if (click != null) {
            component = component.clickEvent(clickEvent(click));
        }
        return component.children(text.children()
                                      .stream()
                                      .map(RichTexts::toAdventure)
                                      .collect(Collectors.toList()));
    }

    // Adventure 5 (Paper 26.x, Velocity 4.x) has no ClickEvent.clickEvent(Action, String) and no enum Action, while the factory
    // methods link against both Adventure 4 and 5.
    private static ClickEvent clickEvent(ClickAction click) {
        switch (click.type()) {
            case RUN_COMMAND:
                return ClickEvent.runCommand(click.value());
            case SUGGEST_COMMAND:
                return ClickEvent.suggestCommand(click.value());
            case COPY_TO_CLIPBOARD:
                return ClickEvent.copyToClipboard(click.value());
            case OPEN_URL:
                return ClickEvent.openUrl(click.value());
            default:
                throw new IllegalArgumentException("Unknown click action: " + click.type());
        }
    }
}
