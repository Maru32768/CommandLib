package dev.marulabs.commandlib.util.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}
import org.jetbrains.annotations.NotNull;

/**
 * Converts {@link RichText} to vanilla chat components.
 */
public final class RichTexts {
    private RichTexts() {
    }

    /**
     * Returns a new component for {@code text}. The 16 legacy colors become named colors ({@link ChatFormatting}).
     */
    @NotNull
    public static MutableComponent toVanilla(@NotNull RichText text) {
        MutableComponent component;
        if (text instanceof TextRichText) {
            String content = ((TextRichText) text).content();
            //? if >=1.19 {
            component = Component.literal(content);
            //?} else
            /*component = new TextComponent(content);*/
        } else {
            TranslatableRichText translatable = (TranslatableRichText) text;
            Object[] args = translatable.arguments()
                                        .stream()
                                        .map(RichTexts::toVanilla)
                                        .toArray();
            //? if >=1.19 {
            component = Component.translatable(translatable.key(), args);
            //?} else
            /*component = new TranslatableComponent(translatable.key(), args);*/
        }

        Style style = Style.EMPTY;
        if (text.color() != null) {
            style = style.withColor(color(text.color()));
        }
        style = style.withBold(text.isBold())
                     .withItalic(text.isItalic());
        //? if >=1.17 {
        style = style.withUnderlined(text.isUnderlined())
                     .withStrikethrough(text.isStrikethrough())
                     .withObfuscated(text.isObfuscated());
        //?} else {
        /*// 1.16.5 has no Boolean setters for these, so only true is applied and an explicit false is dropped.
        if (Boolean.TRUE.equals(text.isUnderlined())) {
            style = style.applyFormat(ChatFormatting.UNDERLINE);
        }
        if (Boolean.TRUE.equals(text.isStrikethrough())) {
            style = style.applyFormat(ChatFormatting.STRIKETHROUGH);
        }
        if (Boolean.TRUE.equals(text.isObfuscated())) {
            style = style.applyFormat(ChatFormatting.OBFUSCATED);
        }
        *///?}
        if (text.hover() != null) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, toVanilla(text.hover())));
        }
        if (text.click() != null) {
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.valueOf(text.click()
                                                                                      .type()
                                                                                      .name()),
                                                        text.click()
                                                            .value()));
        }
        component.setStyle(style);

        for (RichText child : text.children()) {
            component.append(toVanilla(child));
        }
        return component;
    }

    private static TextColor color(int rgb) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            Integer color = formatting.getColor();
            if (color != null && color == rgb) {
                return TextColor.fromLegacyFormat(formatting);
            }
        }
        return TextColor.fromRgb(rgb);
    }
}
