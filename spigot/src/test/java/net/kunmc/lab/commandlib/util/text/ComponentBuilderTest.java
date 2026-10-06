package net.kunmc.lab.commandlib.util.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentBuilderTest {
    @Test
    void text_builder_appends_component_as_extra() {
        BaseComponent appended = new TextComponent("b");

        BaseComponent component = new TextComponentBuilderImpl("a").append(appended)
                                                                   .build();

        assertThat(component.toPlainText()).isEqualTo("ab");
        assertThat(component.getExtra()).containsExactly(appended);
        assertThat(appended.getExtra()).isNull();
    }

    @Test
    void text_builder_applies_color_and_italic() {
        BaseComponent component = new TextComponentBuilderImpl("a").color(0xFF5555)
                                                                   .italic()
                                                                   .build();

        assertThat(component.getColor()).isEqualTo(ChatColor.of(new Color(0xFF5555)));
        assertThat(component.isItalic()).isTrue();
    }

    @Test
    void translatable_builder_appends_component_and_applies_style() {
        BaseComponent appended = new TextComponent("!");

        BaseComponent component = new TranslatableComponentBuilderImpl("command.unknown.argument").color(0xFF5555)
                                                                                                 .italic()
                                                                                                 .append(appended)
                                                                                                 .build();

        assertThat(component).isInstanceOf(TranslatableComponent.class);
        assertThat(((TranslatableComponent) component).getTranslate()).isEqualTo("command.unknown.argument");
        assertThat(component.getColor()).isEqualTo(ChatColor.of(new Color(0xFF5555)));
        assertThat(component.isItalic()).isTrue();
        assertThat(component.getExtra()).containsExactly(appended);
    }
}
