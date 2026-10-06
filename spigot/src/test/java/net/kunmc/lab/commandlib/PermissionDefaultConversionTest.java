package net.kunmc.lab.commandlib;

import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionDefaultConversionTest {
    @ParameterizedTest
    @CsvSource({"TRUE, ALL", "FALSE, NONE", "OP, OP"})
    void bukkit_permission_default_is_converted(PermissionDefault bukkit, DefaultPermission expected) {
        Command command = new Command("spawn");

        command.permission(bukkit);

        assertThat(command.permissionConfig("plugin")
                          .defaultPermission()).isEqualTo(expected);
    }

    @Test
    void node_and_description_overloads_keep_all_values() {
        Command command = new Command("spawn");

        command.permission("custom.spawn", PermissionDefault.TRUE, "Teleport to spawn");

        PermissionConfig config = command.permissionConfig("plugin");
        assertThat(config.node()).isEqualTo("custom.spawn");
        assertThat(config.defaultPermission()).isEqualTo(DefaultPermission.ALL);
        assertThat(config.description()).isEqualTo("Teleport to spawn");
    }

    @Test
    void not_op_has_no_platform_independent_equivalent() {
        Command command = new Command("spawn");

        assertThatThrownBy(() -> command.permission(PermissionDefault.NOT_OP)).isInstanceOf(IllegalArgumentException.class)
                                                                              .hasMessageContaining("NOT_OP");
    }
}
