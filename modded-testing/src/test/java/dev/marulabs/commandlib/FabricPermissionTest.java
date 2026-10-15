package dev.marulabs.commandlib;

//? if fabric {
/*import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

// CommandTester answers permissions from the FakeSender, so check CommandLib's own Fabric lookup here. No
// fabric-permissions-api is loaded in tests, so the node defaults decide.
class FabricPermissionTest {
    @Test
    void defaults_decide_without_fabric_permissions_api() {
        CommandLib.register("fabrictest", new Command("open") {{
            permission(DefaultPermission.ALL);
        }}, new Command("closed") {{
            permission(DefaultPermission.NONE);
        }}, new Command("ops"));
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        Mockito.when(player.hasPermissions(4))
               .thenReturn(true);
        CommandSourceStack source = Mockito.mock(CommandSourceStack.class);

        assertThat(CommandLib.hasPermission(source, player, "fabrictest.open")).isTrue();
        assertThat(CommandLib.hasPermission(source, player, "fabrictest.closed")).isFalse();
        assertThat(CommandLib.hasPermission(source, player, "fabrictest.ops")).isTrue();
        assertThat(CommandLib.hasPermission(source, Mockito.mock(ServerPlayer.class), "fabrictest.ops")).isFalse();
    }
}
*///?}
