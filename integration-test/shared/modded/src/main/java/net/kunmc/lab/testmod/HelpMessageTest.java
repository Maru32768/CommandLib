package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.DefaultPermission;

// Registers a command with only a child (no executor) so that the bot can send it directly and receive the
// help message output. Verification is done by IntegrationTest on the client side.
public final class HelpMessageTest {
    public HelpMessageTest(Command command) {
        command.addChildren(new Command("helpMessageRoot") {{
            permission(DefaultPermission.ALL);
            addChildren(new Command("helpMessageChild") {{
                permission(DefaultPermission.ALL);
                description("A child command for help message prefix testing.");
                execute(ctx -> {
                });
            }});
        }});
    }
}
