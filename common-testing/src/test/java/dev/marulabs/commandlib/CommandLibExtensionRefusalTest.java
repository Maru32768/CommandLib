package dev.marulabs.commandlib;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class CommandLibExtensionRefusalTest {
    @Test
    void refuses_a_before_all_method() {
        assertThat(failures(BeforeAllTester.class)).singleElement(InstanceOfAssertFactories.THROWABLE)
                                                   .isInstanceOf(ParameterResolutionException.class)
                                                   .hasMessageContaining("injects only into test methods");
    }

    private static List<Throwable> failures(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter")
                            .selectors(selectClass(fixture))
                            .execute()
                            .allEvents()
                            .failed()
                            .stream()
                            .map(x -> x.getRequiredPayload(TestExecutionResult.class)
                                       .getThrowable()
                                       .orElseThrow())
                            .collect(Collectors.toList());
    }

    @Tag("fixture")
    static class BeforeAllTester {
        @RegisterExtension
        static final CommandLibExtension commandLib = new CommandLibExtension(() -> new CommandTester(new TestCommand("hub"),
                                                                                                      "test.command"));

        @BeforeAll
        static void setUp(CommandTester tester) {
        }

        @Test
        void test() {
        }
    }
}
