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
    void refuses_two_players_of_one_name() {
        assertThat(failures(TwoDefaultPlayers.class)).singleElement(InstanceOfAssertFactories.THROWABLE)
                                                     .isInstanceOf(ParameterResolutionException.class)
                                                     .hasMessageContaining("another injected player is named Player");
    }

    @Test
    void refuses_two_players_whose_names_differ_only_in_case() {
        assertThat(failures(TwoPlayersOfOneNameIgnoringCase.class)).singleElement(InstanceOfAssertFactories.THROWABLE)
                                                                   .isInstanceOf(ParameterResolutionException.class)
                                                                   .hasMessageContaining("another injected player is named Alex");
    }

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

    private static CommandTester tester() {
        return new CommandTester(new Command("hub"), "test.command");
    }

    @Tag("fixture")
    static class TwoDefaultPlayers {
        @RegisterExtension
        final CommandLibExtension commandLib = new CommandLibExtension(CommandLibExtensionRefusalTest::tester);

        @Test
        void test(FakeSender first, FakeSender second) {
        }
    }

    @Tag("fixture")
    static class TwoPlayersOfOneNameIgnoringCase {
        @RegisterExtension
        final CommandLibExtension commandLib = new CommandLibExtension(CommandLibExtensionRefusalTest::tester);

        @Test
        void test(@Sender("alex") FakeSender first, @Sender("Alex") FakeSender second) {
        }
    }

    @Tag("fixture")
    static class BeforeAllTester {
        @RegisterExtension
        static final CommandLibExtension commandLib = new CommandLibExtension(CommandLibExtensionRefusalTest::tester);

        @BeforeAll
        static void setUp(CommandTester tester) {
        }

        @Test
        void test() {
        }
    }
}
