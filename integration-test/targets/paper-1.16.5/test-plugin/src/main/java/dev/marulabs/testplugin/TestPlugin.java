package dev.marulabs.testplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class TestPlugin extends JavaPlugin {
    private TestMain main;

    @Override
    public void onEnable() {
        main = new TestMain(this);
        main.register();
    }

    @Override
    public void onDisable() {
        if (main != null) {
            main.unregisterOnDisable();
        }
    }

    public static void print(Object obj) {
        if (Objects.equals(System.getProperty("plugin.env"), "DEV")) {
            System.out.printf("[%s] %s%n", TestPlugin.class.getSimpleName(), obj);
        }
    }

    public static void broadcast(Object obj) {
        if (Objects.equals(System.getProperty("plugin.env"), "DEV")) {
            Bukkit.broadcastMessage(String.format("[%s] %s", TestPlugin.class.getSimpleName(), obj));
        }
    }
}
