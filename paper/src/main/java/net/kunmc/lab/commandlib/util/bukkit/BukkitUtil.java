package net.kunmc.lab.commandlib.util.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BukkitUtil {
    private static final Pattern PATTERN = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?");

    public static String getMinecraftVersion() {
        Matcher matcher = PATTERN.matcher(Bukkit.getBukkitVersion());
        matcher.find();
        return matcher.group();
    }

    @Nullable
    public static OfflinePlayer getOfflinePlayerIfEverPlayed(String name) {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                     .filter(x -> {
                         if (x.getName() == null) {
                             return false;
                         }
                         // Minecraft player names are case-insensitive.
                         return x.getName()
                                 .equalsIgnoreCase(name);
                     })
                     .findFirst()
                     .orElse(null);
    }

    private BukkitUtil() {
    }
}
