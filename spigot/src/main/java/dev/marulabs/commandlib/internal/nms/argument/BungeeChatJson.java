package dev.marulabs.commandlib.internal.nms.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * Prepares the JSON of a vanilla component for Spigot 1.20.3's Bungee chat library (1.20-R0.1), which reads the
 * entity of a {@code show_entity} hover event only as a string UUID, while Minecraft 1.20.3 writes it as four ints. A
 * message argument with a selector, such as {@code @p}, has that hover event.
 */
@ApiStatus.Internal
public final class BungeeChatJson {
    private BungeeChatJson() {
    }

    // The Gson the spigot module compiles against has no JsonParser.parseString.
    @SuppressWarnings("deprecation")
    public static String withStringEntityIds(String json) {
        JsonElement root = new JsonParser().parse(json);
        rewrite(root);
        return root.toString();
    }

    private static void rewrite(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray()
                   .forEach(BungeeChatJson::rewrite);
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement action = object.get("action");
        JsonElement contents = object.get("contents");
        if (action != null && action.isJsonPrimitive() && "show_entity".equals(action.getAsString()) && contents != null
                && contents.isJsonObject()) {
            JsonObject entity = contents.getAsJsonObject();
            JsonElement id = entity.get("id");
            if (id != null && id.isJsonArray() && id.getAsJsonArray()
                                                    .size() == 4) {
                entity.addProperty("id", uuid(id.getAsJsonArray()).toString());
            }
        }
        object.entrySet()
              .forEach(x -> rewrite(x.getValue()));
    }

    private static UUID uuid(JsonArray ints) {
        long most = ((long) ints.get(0)
                                .getAsInt() << 32) | (ints.get(1)
                                                          .getAsInt() & 0xFFFFFFFFL);
        long least = ((long) ints.get(2)
                                 .getAsInt() << 32) | (ints.get(3)
                                                           .getAsInt() & 0xFFFFFFFFL);
        return new UUID(most, least);
    }
}
