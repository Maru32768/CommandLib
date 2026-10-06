package net.kunmc.lab.commandlib;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FakeSenderTest {
    @Test
    void player_factories_grant_all_permissions() {
        UUID uuid = UUID.randomUUID();

        assertThat(FakeSender.player("Alex")
                             .hasPermission("any.node")).isTrue();
        assertThat(FakeSender.player("Alex", uuid)
                             .hasPermission("any.node")).isTrue();
        assertThat(FakeSender.player("Alex", uuid)
                             .getUniqueId()).contains(uuid);
    }

    @Test
    void console_is_operator_with_all_permissions() {
        FakeSender console = FakeSender.console();

        assertThat(console.hasPermission("any.node")).isTrue();
        assertThat(console.isOperator()).isTrue();
        assertThat(console.isConsole()).isTrue();
        assertThat(console.isPlayer()).isFalse();
        assertThat(console.getUniqueId()).isEmpty();
    }

    @Test
    void unknown_sender_has_no_permissions() {
        FakeSender unknown = FakeSender.unknown();

        assertThat(unknown.hasPermission("any.node")).isFalse();
        assertThat(unknown.getType()).isEqualTo(CommandActorType.UNKNOWN);
        assertThat(unknown.isConsole()).isFalse();
        assertThat(unknown.isPlayer()).isFalse();
    }

    @Test
    void permissions_restricts_sender_to_listed_nodes() {
        FakeSender sender = FakeSender.player("Alex")
                                      .permissions("a.b", "c.d");

        assertThat(sender.hasPermission("a.b")).isTrue();
        assertThat(sender.hasPermission("c.d")).isTrue();
        assertThat(sender.hasPermission("e.f")).isFalse();
    }

    @Test
    void permissions_replaces_previous_permission_set() {
        FakeSender sender = FakeSender.player("Alex")
                                      .permissions("a.b")
                                      .permissions("c.d");

        assertThat(sender.hasPermission("a.b")).isFalse();
        assertThat(sender.hasPermission("c.d")).isTrue();
    }

    @Test
    void denied_permissions_accumulate_and_override_everything() {
        FakeSender sender = FakeSender.console()
                                      .denyPermissions("a.b")
                                      .denyPermissions("c.d");

        assertThat(sender.hasPermission("a.b")).isFalse();
        assertThat(sender.hasPermission("c.d")).isFalse();
        assertThat(sender.hasPermission("e.f")).isTrue();
    }

    @Test
    void operator_grants_unlisted_permissions_but_not_denied_ones() {
        FakeSender sender = FakeSender.unknown()
                                      .op(true)
                                      .denyPermissions("a.b");

        assertThat(sender.isOperator()).isTrue();
        assertThat(sender.hasPermission("c.d")).isTrue();
        assertThat(sender.hasPermission("a.b")).isFalse();
    }

    @Test
    void copies_do_not_share_state_with_original() {
        FakeSender original = FakeSender.player("Alex");
        FakeSender restricted = original.permissions("a.b");

        assertThat(original.hasPermission("e.f")).isTrue();
        assertThat(restricted.hasPermission("e.f")).isFalse();
    }

    @Test
    void sent_message_texts_strip_legacy_and_hex_colors() {
        FakeSender sender = FakeSender.console();

        sender.sendMessage("§aGreen §lbold§r");
        sender.sendMessage("§x§f§f§0§0§0§0Hex");

        assertThat(sender.getSentMessageTexts()).containsExactly("Green bold", "Hex");
        assertThat(sender.getSentMessageLegacyTexts()).containsExactly("§aGreen §lbold§r",
                                                                       "§x§f§f§0§0§0§0Hex");
    }

    @Test
    void unwrap_returns_sender_only_for_compatible_type() {
        FakeSender sender = FakeSender.console();

        assertThat(sender.unwrap(FakeSender.class)).contains(sender);
        assertThat(sender.unwrap(CommandActor.class)).contains(sender);
        assertThat(sender.unwrap(String.class)).isEmpty();
    }
}
