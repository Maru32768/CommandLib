package net.kunmc.lab.testplugin;

import net.kunmc.lab.commandlib.Command;
import org.bukkit.permissions.PermissionDefault;

/**
 * Registers a command with only a child (no executor) so that the bot can send it directly
 * and receive the help message output as a system chat packet.
 * Verification is done by IntegrationTest rather than server-side, because capturing
 * messages sent to the command sender reliably requires receiving them on the client side.
 */
public final class HelpMessageTest {
    private final Command command;

    public HelpMessageTest(Command command) {
        this.command = command;
        registerCommands();
    }

    private void registerCommands() {
        command.addChildren(new Command("helpMessageRoot") {{
            addChildren(new Command("helpMessageChild") {{
                permission(PermissionDefault.TRUE);
                description("A child command for help message prefix testing.");
                execute(ctx -> {
                });
            }});
        }});
    }
}
