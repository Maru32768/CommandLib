package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

public final class PermissionConfig {
    private final String node;
    private final DefaultPermission defaultPermission;
    private final String description;

    public PermissionConfig(@NotNull String node,
                            @NotNull DefaultPermission defaultPermission,
                            @NotNull String description) {
        this.node = node;
        this.defaultPermission = defaultPermission;
        this.description = description;
    }

    public String node() {
        return node;
    }

    public DefaultPermission defaultPermission() {
        return defaultPermission;
    }

    public String description() {
        return description;
    }
}
