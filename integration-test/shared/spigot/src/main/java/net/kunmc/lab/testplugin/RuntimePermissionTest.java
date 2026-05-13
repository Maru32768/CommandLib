package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.commandlib.DefaultPermission;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RuntimePermissionTest extends TestBase {
    private final Plugin plugin;

    public RuntimePermissionTest(Command command, Plugin plugin) {
        super(command);
        this.plugin = plugin;
    }

    @Override
    public List<String> build() {
        return runtimePermissionDefaultCleanup();
    }

    public List<String> runtimePermissionDefaultCleanup() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                CommandLib allRegistration = null;
                CommandLib noneRegistration = null;
                String commandName = "runtimepermissioncleanup";
                String node = plugin.getName()
                                    .toLowerCase() + ".command." + commandName;
                try {
                    allRegistration = CommandLib.register(plugin, new Command(commandName) {{
                        permission(DefaultPermission.ALL);
                    }});
                    assertPermission(node, PermissionDefault.TRUE);
                    assertDefaultPermissionSetContains(node, false);
                    assertDefaultPermissionSetContains(node, true);

                    allRegistration.unregister();
                    allRegistration = null;
                    assertPermissionRemoved(node);

                    noneRegistration = CommandLib.register(plugin, new Command(commandName) {{
                        permission(DefaultPermission.NONE);
                    }});
                    assertPermission(node, PermissionDefault.FALSE);
                    assertDefaultPermissionSetDoesNotContain(node, false);
                    assertDefaultPermissionSetDoesNotContain(node, true);

                    noneRegistration.unregister();
                    noneRegistration = null;
                    assertPermissionRemoved(node);

                    putResult(new TestResult(key, TestStatus.SUCCEEDED, "Runtime permission cleanup succeeded."));
                } catch (Throwable e) {
                    putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
                } finally {
                    unregisterQuietly(allRegistration);
                    unregisterQuietly(noneRegistration);
                }
            });
        }});

        return List.of(buildCommand(command, name));
    }

    private void assertPermission(String node, PermissionDefault expected) {
        Permission permission = Bukkit.getPluginManager()
                                      .getPermission(node);
        if (permission == null) {
            throw new AssertionError("Permission " + node + " was not registered.");
        }
        if (permission.getDefault() != expected) {
            throw new AssertionError("Expected " + node + " default to be " + expected + " but was " + permission.getDefault());
        }
    }

    private void assertPermissionRemoved(String node) {
        Permission permission = Bukkit.getPluginManager()
                                      .getPermission(node);
        if (permission != null) {
            throw new AssertionError("Permission " + node + " was not removed.");
        }
        assertDefaultPermissionSetDoesNotContain(node, false);
        assertDefaultPermissionSetDoesNotContain(node, true);
    }

    private void assertDefaultPermissionSetContains(String node, boolean op) {
        Set<String> nodes = defaultPermissionNodes(op);
        if (!nodes.contains(node)) {
            throw new AssertionError("Default permission set op=" + op + " did not contain " + node + ": " + nodes);
        }
    }

    private void assertDefaultPermissionSetDoesNotContain(String node, boolean op) {
        Set<String> nodes = defaultPermissionNodes(op);
        if (nodes.contains(node)) {
            throw new AssertionError("Default permission set op=" + op + " still contained " + node + ": " + nodes);
        }
    }

    private Set<String> defaultPermissionNodes(boolean op) {
        return Bukkit.getPluginManager()
                     .getDefaultPermissions(op)
                     .stream()
                     .map(Permission::getName)
                     .collect(Collectors.toSet());
    }

    private void unregisterQuietly(CommandLib registration) {
        if (registration == null) {
            return;
        }
        try {
            registration.unregister();
        } catch (Throwable ignored) {
        }
    }
}
