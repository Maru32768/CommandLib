package net.kunmc.lab.commandlib.util.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TextComponentBuilderImpl extends TextComponentBuilder<BaseComponent, TextComponent, TextComponentBuilderImpl> {
    public TextComponentBuilderImpl(@NotNull String text) {
        super(new TextComponent(text));
    }

    @Override
    public TextComponentBuilderImpl color(int rgb) {
        component.setColor(ChatColor.of(new Color(rgb)));
        return this;
    }

    @Override
    public TextComponentBuilderImpl italic() {
        component.setItalic(true);
        return this;
    }

    @Override
    public TextComponentBuilderImpl append(BaseComponent component) {
        this.component.addExtra(component);
        return this;
    }
}
