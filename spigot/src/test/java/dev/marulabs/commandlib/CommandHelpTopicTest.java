package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicFactory;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandHelpTopicTest {
    private static BukkitCommand wrapper(String name) {
        return new BukkitCommand(name, "A Mojang provided command.", "/" + name, Collections.emptyList()) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return true;
            }
        };
    }

    private static LiteralCommandNode<Object> node(String name) {
        return LiteralArgumentBuilder.literal(name)
                                     .executes(ctx -> 1)
                                     .then(LiteralArgumentBuilder.literal("sub"))
                                     .then(RequiredArgumentBuilder.argument("value", StringArgumentType.word()))
                                     .build();
    }

    @Test
    void describesTheWrapperWithTheCommandPermissionDescriptionUsageAndAliases() {
        Command command = new Command("foo");
        command.description("Does foo things");
        command.addAliases("f");
        BukkitCommand wrapper = wrapper("foo");

        CommandLib.describe(wrapper, command, node("foo"), "plugin.command", "Plugin");

        assertThat(wrapper.getPermission()).isEqualTo("plugin.command.foo");
        assertThat(wrapper.getDescription()).isEqualTo("Does foo things");
        assertThat(wrapper.getUsage()).isEqualTo("\n- /<command>\n- /<command> sub\n- /<command> <value>");
        assertThat(wrapper.getAliases()).containsExactly("f");
    }

    @Test
    void listsTheOtherLabelsAsAliasesOfAnAlias() {
        Command command = new Command("foo");
        command.addAliases("f", "fo");
        BukkitCommand wrapper = wrapper("f");

        CommandLib.describe(wrapper, command, node("f"), "plugin.command", "Plugin");

        assertThat(wrapper.getPermission()).isEqualTo("plugin.command.foo");
        assertThat(wrapper.getAliases()).containsExactly("foo", "fo");
    }

    @Test
    void namesThePluginWhenTheDescriptionIsMissingOrDependsOnTheContext() {
        Command missing = new Command("foo");
        Command dynamic = new Command("bar");
        dynamic.description(ctx -> "for " + ctx.getSender()
                                               .getName());
        BukkitCommand missingWrapper = wrapper("foo");
        BukkitCommand dynamicWrapper = wrapper("bar");

        CommandLib.describe(missingWrapper, missing, node("foo"), "plugin.command", "Plugin");
        CommandLib.describe(dynamicWrapper, dynamic, node("bar"), "plugin.command", "Plugin");

        assertThat(missingWrapper.getDescription()).isEqualTo("A command by the Plugin plugin.");
        assertThat(dynamicWrapper.getDescription()).isEqualTo("A command by the Plugin plugin.");
    }

    @Test
    void serverHelpTextReplacesTheDescription() {
        Command command = new Command("foo");
        command.description("Does foo things");
        command.serverHelp("Short help");
        BukkitCommand wrapper = wrapper("foo");

        CommandLib.describe(wrapper, command, node("foo"), "plugin.command", "Plugin");

        assertThat(wrapper.getDescription()).isEqualTo("Short help");
    }

    @Test
    void helpTopicShowsTheServerHelpPageWhenSet() {
        Command generated = new Command("foo");
        generated.serverHelp("Short help");
        Command custom = new Command("bar");
        custom.serverHelp("Short help", "The whole page");
        BukkitCommand generatedWrapper = wrapper("foo");
        BukkitCommand customWrapper = wrapper("bar");
        CommandLib.describe(generatedWrapper, generated, node("foo"), "plugin.command", "Plugin");
        CommandLib.describe(customWrapper, custom, node("bar"), "plugin.command", "Plugin");

        HelpTopic generatedTopic = CommandLib.helpTopic(generatedWrapper, generated);
        HelpTopic customTopic = CommandLib.helpTopic(customWrapper, custom);

        assertThat(generatedTopic.getShortText()).isEqualTo("Short help");
        assertThat(generatedTopic.getFullText(mock(CommandSender.class))).contains("Short help", "/foo sub");
        assertThat(customTopic.getShortText()).isEqualTo("Short help");
        assertThat(customTopic.getFullText(mock(CommandSender.class))).isEqualTo("The whole page");
    }

    @Test
    void helpTopicIsVisibleOnlyWithTheCommandPermission() {
        Command command = new Command("foo");
        BukkitCommand wrapper = wrapper("foo");
        CommandLib.describe(wrapper, command, node("foo"), "plugin.command", "Plugin");
        wrapper.register(mock(CommandMap.class));
        HelpTopic topic = new GenericCommandHelpTopic(wrapper);
        CommandSender allowed = mock(CommandSender.class);
        when(allowed.hasPermission("plugin.command.foo")).thenReturn(true);
        CommandSender denied = mock(CommandSender.class);

        assertThat(topic.getName()).isEqualTo("/foo");
        assertThat(topic.getShortText()).isEqualTo("A command by the Plugin plugin.");
        assertThat(topic.canSee(allowed)).isTrue();
        assertThat(topic.canSee(denied)).isFalse();
    }

    @Test
    void putHelpTopicReplacesATopicOfTheSameName() {
        HelpMap helpMap = new TestHelpMap();
        HelpTopic old = topic("/foo");
        helpMap.addTopic(old);
        HelpTopic replacement = topic("/foo");

        CommandLib.putHelpTopic(helpMap, replacement);

        assertThat(helpMap.getHelpTopic("/foo")).isSameAs(replacement);
        assertThat(helpMap.getHelpTopics()).containsExactly(replacement);
    }

    @Test
    void removeHelpTopicKeepsATopicThatReplacedIt() {
        HelpMap helpMap = new TestHelpMap();
        HelpTopic own = topic("/foo");
        HelpTopic other = topic("/foo");
        helpMap.addTopic(other);

        CommandLib.removeHelpTopic(helpMap, own);

        assertThat(helpMap.getHelpTopic("/foo")).isSameAs(other);

        CommandLib.removeHelpTopic(helpMap, other);

        assertThat(helpMap.getHelpTopic("/foo")).isNull();
    }

    private static HelpTopic topic(String name) {
        BukkitCommand command = wrapper(name.substring(1));
        return new GenericCommandHelpTopic(command);
    }

    /**
     * Keeps topics like CraftBukkit's SimpleHelpMap: addTopic keeps an existing topic, and getHelpTopics() is a live
     * view.
     */
    private static final class TestHelpMap implements HelpMap {
        private final Map<String, HelpTopic> topics = new TreeMap<>();

        @Override
        public HelpTopic getHelpTopic(String topicName) {
            return topics.get(topicName);
        }

        @Override
        public Collection<HelpTopic> getHelpTopics() {
            return topics.values();
        }

        @Override
        public void addTopic(HelpTopic topic) {
            topics.putIfAbsent(topic.getName(), topic);
        }

        @Override
        public void clear() {
            topics.clear();
        }

        @Override
        public void registerHelpTopicFactory(Class<?> commandClass, HelpTopicFactory<?> factory) {
        }

        @Override
        public List<String> getIgnoredPlugins() {
            return Collections.emptyList();
        }
    }
}
