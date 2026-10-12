package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks the server's /help for the main command, the text set with serverHelp and the plugin's index instead of the
 * "Minecraft" one, and that the command runs under its namespaced label. Paper rebuilds the help map on
 * /minecraft:reload, so the help check runs again after it.
 */
public final class HelpTopicTest extends TestBase {
    public static final String SHORT_TEXT = "CommandLib integration test command";
    public static final String FULL_TEXT = "The CommandLib integration test command runs the test cases.";
    public static final String AFTER_RELOAD = "helpTopicAfterReload";

    private final Plugin plugin;

    public HelpTopicTest(Command command, Plugin plugin) {
        super(command);
        this.plugin = plugin;
        command.serverHelp(SHORT_TEXT, FULL_TEXT);
    }

    @Override
    public List<String> build() {
        String key = getKey();
        String afterReloadKey = getClass().getSimpleName() + "." + AFTER_RELOAD;
        String namespacedKey = getClass().getSimpleName() + ".namespacedLabel";

        putCommandNotExecutedResult(key);
        putCommandNotExecutedResult(namespacedKey);
        putResult(new TestResult(afterReloadKey,
                                 TestStatus.FAILED,
                                 "Help topic was not checked after /minecraft:reload."));
        command.addChildren(new Command("helpTopic") {{
            execute(ctx -> putResult(check(key)));
        }}, new Command(AFTER_RELOAD) {{
            execute(ctx -> putResult(check(afterReloadKey)));
        }}, new Command("namespacedLabel") {{
            execute(ctx -> putResult(new TestResult(namespacedKey,
                                                    TestStatus.SUCCEEDED,
                                                    "Command ran under " + ctx.getHandle()
                                                                              .getInput())));
        }});

        return List.of(buildCommand(command, "helpTopic"),
                       plugin.getName()
                             .toLowerCase(Locale.ROOT) + ":" + buildCommand(command, "namespacedLabel"));
    }

    private TestResult check(String key) {
        HelpMap helpMap = Bukkit.getHelpMap();
        CommandSender console = Bukkit.getConsoleSender();
        String topicName = "/" + command.name();
        List<String> problems = new ArrayList<>();

        HelpTopic topic = helpMap.getHelpTopic(topicName);
        if (topic == null) {
            problems.add("no topic " + topicName);
        } else {
            if (!SHORT_TEXT.equals(topic.getShortText())) {
                problems.add("short text was " + topic.getShortText());
            }
            if (!FULL_TEXT.equals(topic.getFullText(console))) {
                problems.add("full text was " + topic.getFullText(console));
            }
        }
        HelpTopic pluginIndex = helpMap.getHelpTopic(plugin.getName());
        if (!(pluginIndex instanceof IndexHelpTopic) || !pluginIndex.getFullText(console)
                                                                    .contains(topicName + ":")) {
            problems.add("plugin index does not list " + topicName + ": "
                    + (pluginIndex == null ? null : pluginIndex.getFullText(console)));
        }
        HelpTopic minecraftIndex = helpMap.getHelpTopic("Minecraft");
        if (minecraftIndex != null && minecraftIndex.getFullText(console)
                                                    .contains(topicName + ":")) {
            problems.add("Minecraft index lists " + topicName);
        }

        return problems.isEmpty() ? new TestResult(key, TestStatus.SUCCEEDED, "Help topic is " + SHORT_TEXT)
                : new TestResult(key, TestStatus.FAILED, String.join("\n", problems));
    }
}
