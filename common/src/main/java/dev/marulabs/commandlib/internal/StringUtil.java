package dev.marulabs.commandlib.internal;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class StringUtil {
    private StringUtil() {
    }


    public static boolean containsIgnoreCase(String s, String search) {
        if (s == null || search == null) {
            return false;
        }

        int searchLength = search.length();
        int max = s.length() - searchLength;
        for (int i = 0; i <= max; i++) {
            if (s.regionMatches(true, i, search, 0, searchLength)) {
                return true;
            }
        }
        return false;
    }
}
