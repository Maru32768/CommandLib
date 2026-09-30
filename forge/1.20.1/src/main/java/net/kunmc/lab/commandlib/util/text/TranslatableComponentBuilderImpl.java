package net.kunmc.lab.commandlib.util.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

public class TranslatableComponentBuilderImpl extends TranslatableComponentBuilder<Component, MutableComponent, TranslatableComponentBuilderImpl> {
    public TranslatableComponentBuilderImpl(@NotNull String key) {
        super(Component.translatable(key));
    }

    @Override
    public TranslatableComponentBuilderImpl color(int rgb) {
        component.setStyle(component.getStyle()
                                    .withColor(TextColor.fromRgb(rgb)));
        return this;
    }

    @Override
    public TranslatableComponentBuilderImpl italic() {
        component.withStyle(ChatFormatting.ITALIC);
        return this;
    }

    @Override
    public TranslatableComponentBuilderImpl append(Component component) {
        this.component.append(component);
        return this;
    }
}
