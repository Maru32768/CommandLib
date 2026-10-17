package dev.marulabs.commandlib.util.nms.argument;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BungeeChatJsonTest {
    @Test
    void writes_the_int_array_uuid_of_a_show_entity_hover_event_as_a_string() {
        UUID uuid = UUID.fromString("1b13440a-c070-33ad-9860-42cb2c56963f");
        String ints = (int) (uuid.getMostSignificantBits() >> 32) + "," + (int) uuid.getMostSignificantBits() + ","
                + (int) (uuid.getLeastSignificantBits() >> 32) + "," + (int) uuid.getLeastSignificantBits();
        // The message "hello, @p!" as Minecraft 1.20.3 writes it.
        String json = "{\"text\":\"\",\"extra\":[\"hello, \",{\"text\":\"Maru32768\",\"hoverEvent\":{\"action\":"
                + "\"show_entity\",\"contents\":{\"type\":\"minecraft:player\",\"id\":[" + ints + "],\"name\":"
                + "\"Maru32768\"}},\"insertion\":\"Maru32768\"},\"!\"]}";

        String rewritten = BungeeChatJson.withStringEntityIds(json);

        assertThat(rewritten).contains("\"id\":\"" + uuid + "\"");
        BaseComponent component = ComponentSerializer.parse(rewritten)[0];
        assertThat(component.toPlainText()).isEqualTo("hello, Maru32768!");
    }

    @Test
    void leaves_other_json_alone() {
        assertThat(BungeeChatJson.withStringEntityIds("\"plain\"")).isEqualTo("\"plain\"");
        assertThat(BungeeChatJson.withStringEntityIds("{\"text\":\"a\",\"hoverEvent\":{\"action\":\"show_text\","
                + "\"contents\":{\"id\":[1,2,3,4]}}}")).contains("\"id\":[1,2,3,4]");
    }
}
