package net.kunmc.lab.testplugin;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;

/**
 * Detects that a data pack reload has been applied. /minecraft:reload loads the resources asynchronously and then
 * swaps the command dispatcher, advancements and other reloadable resources together on the main thread, so a new
 * advancement handle means the dispatcher has been replaced as well.
 */
final class ReloadWatcher {
    private static final NamespacedKey ADVANCEMENT = NamespacedKey.minecraft("story/root");
    private final Object handleBeforeReload = advancementHandle();

    boolean reloaded() {
        return advancementHandle() != handleBeforeReload;
    }

    private static Object advancementHandle() {
        Advancement advancement = Bukkit.getAdvancement(ADVANCEMENT);
        if (advancement == null) {
            throw new IllegalStateException("Advancement " + ADVANCEMENT + " is not loaded.");
        }
        try {
            return advancement.getClass()
                              .getMethod("getHandle")
                              .invoke(advancement);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read the handle of " + advancement.getClass(), e);
        }
    }
}
