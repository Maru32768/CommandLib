package dev.marulabs.commandlib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionLoadErrorFilterTest {
    private final AtomicBoolean recompiling = new AtomicBoolean(true);
    private final List<String> logged = new ArrayList<>();
    private final Logger logger = LogManager.getLogger("net.minecraft.server.ServerFunctionLibrary");
    private FunctionLoadErrorFilter filter;
    private AbstractAppender appender;

    @BeforeEach
    void setUp() {
        filter = Objects.requireNonNull(FunctionLoadErrorFilter.install(getClass().getClassLoader(), recompiling::get));
        appender = new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                logged.add(event.getMessage()
                                .getFormattedMessage());
            }
        };
        appender.start();
        LoggerContext context = (LoggerContext) LogManager.getContext(getClass().getClassLoader(), false);
        context.getConfiguration()
               .getRootLogger()
               .addAppender(appender, null, null);
        context.updateLoggers();
    }

    @AfterEach
    void tearDown() {
        filter.uninstall();
        LoggerContext context = (LoggerContext) LogManager.getContext(getClass().getClassLoader(), false);
        context.getConfiguration()
               .getRootLogger()
               .removeAppender("capture");
        context.updateLoggers();
    }

    @Test
    void drops_the_servers_error_while_commandlib_compiles_again() {
        logger.error(FunctionLoadErrorFilter.MESSAGE, "test:probe", new IllegalArgumentException("Unknown command"));

        assertThat(logged).isEmpty();
    }

    @Test
    void comes_before_a_filter_that_accepts_errors() {
        filter.uninstall();
        Configuration configuration = ((LoggerContext) LogManager.getContext(getClass().getClassLoader(),
                                                                             false)).getConfiguration();
        // Like Forge's configuration, which Mohist uses.
        Filter accepting = ThresholdFilter.createFilter(org.apache.logging.log4j.Level.ERROR,
                                                        Filter.Result.ACCEPT,
                                                        Filter.Result.NEUTRAL);
        configuration.addFilter(accepting);
        try {
            filter = Objects.requireNonNull(FunctionLoadErrorFilter.install(getClass().getClassLoader(),
                                                                            recompiling::get));

            logger.error(FunctionLoadErrorFilter.MESSAGE, "test:probe", new IllegalArgumentException("Unknown"));

            assertThat(logged).isEmpty();
        } finally {
            configuration.removeFilter(accepting);
        }
    }

    @Test
    void keeps_the_error_of_commandlibs_own_compilation() {
        recompileFunctions();

        assertThat(logged).containsExactly("Failed to load function test:probe");
    }

    @Test
    void keeps_the_error_when_commandlib_does_not_compile_again() {
        recompiling.set(false);

        logger.error(FunctionLoadErrorFilter.MESSAGE, "test:probe", new IllegalArgumentException("Unknown command"));

        assertThat(logged).containsExactly("Failed to load function test:probe");
    }

    @Test
    void keeps_other_messages() {
        logger.error("Failed to load something else {}", "x");

        assertThat(logged).containsExactly("Failed to load something else x");
        assertThat(filter.decide("Failed to load something else {}")).isEqualTo(Filter.Result.NEUTRAL);
    }

    @Test
    void drops_the_error_of_a_function_tag_listing_a_function_the_server_dropped() {
        ServerFunctionLibrary.buildTags(logger, FunctionLoadErrorFilter.TAG_MESSAGES.get(1));
        ServerFunctionLibrary.buildTags(logger, FunctionLoadErrorFilter.TAG_MESSAGES.get(0));

        assertThat(logged).isEmpty();
    }

    @Test
    void keeps_the_error_of_a_tag_of_another_registry() {
        logger.error(FunctionLoadErrorFilter.TAG_MESSAGES.get(1), "minecraft:logs", "test:missing");

        assertThat(logged).containsExactly("Couldn't load tag minecraft:logs as it is missing following references: test:missing");
    }

    // Stands for the server's function library, which builds the function tags.
    private static final class ServerFunctionLibrary {
        static void buildTags(Logger logger, String format) {
            if (format.startsWith("Couldn't load {}")) {
                logger.error(format, "function", "minecraft:load", "test:probe");
            } else {
                logger.error(format, "minecraft:load", "test:probe");
            }
        }
    }

    // Named as FunctionLoadErrorFilter.RECOMPILE_METHOD.
    private void recompileFunctions() {
        logger.error(FunctionLoadErrorFilter.MESSAGE, "test:probe", new IllegalArgumentException("Unknown command"));
    }
}
