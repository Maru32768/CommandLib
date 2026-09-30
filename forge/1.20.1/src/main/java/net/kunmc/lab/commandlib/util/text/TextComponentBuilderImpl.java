package net.kunmc.lab.commandlib.util.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public class TextComponentBuilderImpl extends TextComponentBuilder<Component, MutableComponent, TextComponentBuilderImpl> {
    public TextComponentBuilderImpl(String text) {
        super(Component.literal(text));
    }

    @Override
    public TextComponentBuilderImpl color(int rgb) {
        component.setStyle(component.getStyle()
                                    .withColor(TextColor.fromRgb(rgb)));
        return this;
    }

    @Override
    public TextComponentBuilderImpl italic() {
        component.withStyle(ChatFormatting.ITALIC);
        return this;
    }

    @Override
    public TextComponentBuilderImpl append(Component component) {
        this.component.append(component);
        return this;
    }
}
