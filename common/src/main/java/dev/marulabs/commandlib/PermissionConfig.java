package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

final class PermissionConfig {
    private final String node;
    private final DefaultPermission defaultPermission;
    private final String description;

    PermissionConfig(@NotNull String node, @NotNull DefaultPermission defaultPermission, @NotNull String description) {
        this.node = node;
        this.defaultPermission = defaultPermission;
        this.description = description;
    }

    String node() {
        return node;
    }

    DefaultPermission defaultPermission() {
        return defaultPermission;
    }

    String description() {
        return description;
    }
}
