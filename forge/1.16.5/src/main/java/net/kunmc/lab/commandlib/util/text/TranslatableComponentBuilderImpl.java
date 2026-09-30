package net.kunmc.lab.commandlib.util.text;

import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;

public class TranslatableComponentBuilderImpl extends TranslatableComponentBuilder<ITextComponent, TranslationTextComponent, TranslatableComponentBuilderImpl> {
    public TranslatableComponentBuilderImpl(@NotNull String key) {
        super(new TranslationTextComponent(key));
    }

    @Override
    public TranslatableComponentBuilderImpl color(int rgb) {
        component.setStyle(component.getStyle()
                                    .setColor(Color.fromInt(rgb)));
        return this;
    }

    @Override
    public TranslatableComponentBuilderImpl italic() {
        component.mergeStyle(TextFormatting.ITALIC);
        return this;
    }

    @Override
    public TranslatableComponentBuilderImpl append(ITextComponent component) {
        this.component.appendSibling(component);
        return this;
    }
}
