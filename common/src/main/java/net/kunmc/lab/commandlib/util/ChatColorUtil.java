package net.kunmc.lab.commandlib.util;

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

public class ChatColorUtil {
    public static final ChatColor BLACK = new ChatColor('0', new Color(0x000000));
    public static final ChatColor DARK_BLUE = new ChatColor('1', new Color(0x0000AA));
    public static final ChatColor DARK_GREEN = new ChatColor('2', new Color(0x00AA00));
    public static final ChatColor DARK_AQUA = new ChatColor('3', new Color(0x00AAAA));
    public static final ChatColor DARK_RED = new ChatColor('4', new Color(0xAA0000));
    public static final ChatColor DARK_PURPLE = new ChatColor('5', new Color(0xAA00AA));
    public static final ChatColor GOLD = new ChatColor('6', new Color(0xFFAA00));
    public static final ChatColor GRAY = new ChatColor('7', new Color(0xAAAAAA));
    public static final ChatColor DARK_GRAY = new ChatColor('8', new Color(0x555555));
    public static final ChatColor BLUE = new ChatColor('9', new Color(0x5555FF));
    public static final ChatColor GREEN = new ChatColor('a', new Color(0x55FF55));
    public static final ChatColor AQUA = new ChatColor('b', new Color(0x55FFFF));
    public static final ChatColor RED = new ChatColor('c', new Color(0xFF5555));
    public static final ChatColor LIGHT_PURPLE = new ChatColor('d', new Color(0xFF55FF));
    public static final ChatColor YELLOW = new ChatColor('e', new Color(0xFFFF55));
    public static final ChatColor WHITE = new ChatColor('f', new Color(0xFFFFFF));
    public static final ChatColor MAGIC = new ChatColor('k');
    public static final ChatColor BOLD = new ChatColor('l');
    public static final ChatColor STRIKETHROUGH = new ChatColor('m');
    public static final ChatColor UNDERLINE = new ChatColor('n');
    public static final ChatColor ITALIC = new ChatColor('o');
    public static final ChatColor RESET = new ChatColor('r');

    public static class ChatColor {
        public static final char COLOR_CHAR = '\u00A7';
        private final String str;
        private final Color color;

        private ChatColor(char code) {
            this(code, null);
        }

        private ChatColor(char code, Color color) {
            this.str = new String(new char[]{COLOR_CHAR, code});
            this.color = color;
        }

        @Nullable
        public Integer getRGB() {
            if (color == null) {
                return null;
            }
            // Color#getRGB includes the alpha channel; callers expect a plain 0xRRGGBB value.
            return color.getRGB() & 0xFFFFFF;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.str);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ChatColor other = (ChatColor) obj;

            return Objects.equals(this.str, other.str);
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
