package net.kunmc.lab.commandlib.util.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
//? if >=1.19 {
import net.minecraft.network.chat.MutableComponent;
//?} else {
/*import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
*///?}
import net.minecraft.network.chat.TextColor;

//? if >=1.19 {
public class TextComponentBuilderImpl extends TextComponentBuilder<Component, MutableComponent, TextComponentBuilderImpl> {
    public TextComponentBuilderImpl(String text) {
        super(Component.literal(text));
    }
//?} else {
/*public class TextComponentBuilderImpl extends TextComponentBuilder<Component, BaseComponent, TextComponentBuilderImpl> {
    public TextComponentBuilderImpl(String text) {
        super(new TextComponent(text));
    }
*///?}

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
