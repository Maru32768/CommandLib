package dev.marulabs.commandlib.util.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Converts {@link RichText} to Bungee chat components.
 */
public final class RichTexts {
    private static final ChatColor[] NAMED_COLORS = {ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE};

    private RichTexts() {
    }

    /**
     * Returns a new component for {@code text}. The 16 legacy colors become named {@link ChatColor}s.
     */
    @NotNull
    public static BaseComponent toBungee(@NotNull RichText text) {
        BaseComponent component;
        if (text.text() != null) {
            component = new TextComponent(text.text());
        } else {
            component = new TranslatableComponent(text.translationKey(),
                                                  text.arguments()
                                                      .stream()
                                                      .map(RichTexts::toBungee)
                                                      .toArray());
        }
        if (text.color() != null) {
            component.setColor(color(text.color()));
        }
        component.setBold(text.isBold());
        component.setItalic(text.isItalic());
        component.setUnderlined(text.isUnderlined());
        component.setStrikethrough(text.isStrikethrough());
        component.setObfuscated(text.isObfuscated());
        if (text.hover() != null) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                   new Text(new BaseComponent[]{toBungee(text.hover())})));
        }
        if (text.click() != null) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(text.click()
                                                                                 .type()
                                                                                 .name()),
                                                   text.click()
                                                       .value()));
        }
        text.children()
            .forEach(child -> component.addExtra(toBungee(child)));
        return component;
    }

    private static ChatColor color(int rgb) {
        for (ChatColor named : NAMED_COLORS) {
            if ((named.getColor()
                      .getRGB() & 0xFFFFFF) == rgb) {
                return named;
            }
        }
        return ChatColor.of(new Color(rgb));
    }
}
