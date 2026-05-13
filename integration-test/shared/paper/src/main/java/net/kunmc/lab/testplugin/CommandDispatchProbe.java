package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class CommandDispatchProbe {
    private final Plugin plugin;

    public CommandDispatchProbe(Plugin plugin) {
        this.plugin = plugin;
    }

    public CommandDispatchResult dispatch(CommandSender sender, String commandLine) {
        CapturingLogHandler handler = new CapturingLogHandler();
        Logger pluginLogger = plugin.getLogger();
        Logger bukkitLogger = Bukkit.getLogger();
        Logger rootLogger = Logger.getLogger("");
        pluginLogger.addHandler(handler);
        bukkitLogger.addHandler(handler);
        rootLogger.addHandler(handler);

        boolean dispatched = false;
        Throwable throwable = null;
        try {
            dispatched = Bukkit.getServer()
                               .dispatchCommand(sender, commandLine);
        } catch (Throwable e) {
            throwable = e;
        } finally {
            pluginLogger.removeHandler(handler);
            bukkitLogger.removeHandler(handler);
            rootLogger.removeHandler(handler);
        }

        return new CommandDispatchResult(dispatched, throwable, handler.messages());
    }

    private static final class CapturingLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        List<String> messages() {
            return new ArrayList<>(messages);
        }

        @Override
        public void publish(LogRecord record) {
            if (record == null || record.getLevel()
                                        .intValue() < Level.INFO.intValue()) {
                return;
            }

            String loggerName = record.getLoggerName() == null ? "unknown" : record.getLoggerName();
            messages.add("[" + record.getLevel() + "] [" + loggerName + "] " + record.getMessage());
            if (record.getThrown() != null) {
                messages.add(ExceptionUtil.stackTraceToString(record.getThrown()));
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
