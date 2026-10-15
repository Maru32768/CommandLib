package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Writes maps, lists, strings, numbers, booleans and null as indented JSON, for the exports of {@link CommandDocs}.
 */
final class Json {
    private Json() {
    }

    static String write(@Nullable Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.append('\n')
                  .toString();
    }

    private static void write(StringBuilder out, @Nullable Object value, int depth) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Map) {
            writeObject(out, (Map<?, ?>) value, depth);
        } else if (value instanceof List) {
            writeArray(out, (List<?>) value, depth);
        } else if (value instanceof Boolean || value instanceof Number) {
            out.append(value);
        } else {
            writeString(out, value.toString());
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map, int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet()
                                                         .iterator();
        while (entries.hasNext()) {
            Map.Entry<?, ?> entry = entries.next();
            indent(out, depth + 1);
            writeString(out, String.valueOf(entry.getKey()));
            out.append(": ");
            write(out, entry.getValue(), depth + 1);
            out.append(entries.hasNext() ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<?> list, int depth) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(out, depth + 1);
            write(out, list.get(i), depth + 1);
            out.append(i < list.size() - 1 ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append(']');
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }
}
