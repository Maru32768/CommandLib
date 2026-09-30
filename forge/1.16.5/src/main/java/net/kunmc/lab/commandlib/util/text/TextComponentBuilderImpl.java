package net.kunmc.lab.commandlib.util.text;

import net.minecraft.util.text.*;

public class TextComponentBuilderImpl extends TextComponentBuilder<ITextComponent, TextComponent, TextComponentBuilderImpl> {
    public TextComponentBuilderImpl(String text) {
        super(new StringTextComponent(text));
    }

    @Override
    public TextComponentBuilderImpl color(int rgb) {
        component.setStyle(component.getStyle()
                                    .setColor(Color.fromInt(rgb)));
        return this;
    }

    @Override
    public TextComponentBuilderImpl italic() {
        component.mergeStyle(TextFormatting.ITALIC);
        return this;
    }

    @Override
    public TextComponentBuilderImpl append(ITextComponent component) {
        this.component.appendSibling(component);
        return this;
    }
}
