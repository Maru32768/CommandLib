package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicComparator;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.TreeSet;

/**
 * The /help topics of the labels a registration added to the live dispatcher.
 */
final class HelpTopics {
    private HelpTopics() {
    }

    /**
     * Paper builds the help map from the command map only when the server loads and on a data reload, so the labels of
     * a direct add get their topics here.
     */
    static void show(Plugin plugin, CommandSet commands, Collection<String> labels) {
        HelpMap helpMap = Bukkit.getHelpMap();
        for (String label : labels) {
            if (helpMap.getHelpTopic("/" + label) != null) {
                continue;
            }
            org.bukkit.command.Command wrapper = Bukkit.getCommandMap()
                                                       .getCommand(label);
            if (wrapper != null) {
                helpMap.addTopic(new GenericCommandHelpTopic(wrapper));
            }
        }
        amend(plugin, commands, labels);
    }

    /**
     * Paper's help topic for a registered command has no permission, so /help would list it for everyone, and its page
     * is generated. The help map is rebuilt after the COMMANDS event, so the topics are amended once it is ready.
     */
    static void amend(Plugin plugin, CommandSet commands, Collection<String> labels) {
        HelpMap helpMap = Bukkit.getHelpMap();
        for (String label : labels) {
            HelpTopic topic = helpMap.getHelpTopic("/" + label);
            Command command = commands.command(label);
            if (topic == null || command == null) {
                continue;
            }
            topic.amendCanSee(command.permissionName(commands.permissionPrefix()));
            String fullText = command.serverHelpFullText();
            if (fullText != null) {
                topic.amendTopic(null, fullText);
            }
            // At startup and on Bukkit /reload, Paper builds the plugin indexes before the COMMANDS event and keeps
            // them when it builds the help map again after it, so a plugin index made for the plugin's plugin.yml
            // commands lacks these. A direct add after the help map was built has no plugin index if the plugin had no
            // commands then.
            Collection<HelpTopic> pluginTopics = pluginIndexTopics(helpMap, plugin.getName());
            if (pluginTopics != null) {
                pluginTopics.remove(topic);
                pluginTopics.add(topic);
            }
        }
    }

    static void remove(Plugin plugin, Collection<String> labels) {
        HelpMap helpMap = Bukkit.getHelpMap();
        HelpTopic index = helpMap.getHelpTopic(plugin.getName());
        Collection<HelpTopic> pluginTopics = indexTopics(index);
        for (String label : labels) {
            HelpTopic topic = helpMap.getHelpTopic("/" + label);
            if (topic == null) {
                continue;
            }
            // CraftBukkit's getHelpTopics() is a live view of its topics.
            helpMap.getHelpTopics()
                   .remove(topic);
            if (pluginTopics != null) {
                pluginTopics.remove(topic);
            }
        }
        if (pluginTopics != null && pluginTopics.isEmpty()) {
            helpMap.getHelpTopics()
                   .remove(index);
        }
    }

    @Nullable
    private static Collection<HelpTopic> pluginIndexTopics(HelpMap helpMap, String name) {
        HelpTopic index = helpMap.getHelpTopic(name);
        if (index != null) {
            return indexTopics(index);
        }
        Collection<HelpTopic> topics = new TreeSet<>(HelpTopicComparator.helpTopicComparatorInstance());
        // The same texts as CraftBukkit's plugin indexes.
        helpMap.addTopic(new IndexHelpTopic(name,
                                            "All commands for " + name,
                                            null,
                                            topics,
                                            "Below is a list of all " + name + " commands:"));
        return topics;
    }

    /**
     * The topics an index lists, which IndexHelpTopic keeps in a protected field without an accessor. The plugin
     * indexes of the help map hold a mutable set there.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Collection<HelpTopic> indexTopics(@Nullable HelpTopic index) {
        if (!(index instanceof IndexHelpTopic)) {
            return null;
        }
        try {
            Field field = IndexHelpTopic.class.getDeclaredField("allTopics");
            field.setAccessible(true);
            return (Collection<HelpTopic>) field.get(index);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
