package dev.marulabs.commandlib.integration;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JUnitReportAssertionsTest {
    private static final Path REPORT = Path.of("fixture-report.xml");
    private static final List<String> EXPECTED = List.of("ArgumentTest.booleanArgument", "OptionTest.requiredOption");

    @Test
    void complete_report_succeeds_regardless_of_order() throws Exception {
        JUnitReportAssertions.assertSucceeded(REPORT, report(List.of(EXPECTED.get(1), EXPECTED.get(0))), EXPECTED);
    }

    @Test
    void missing_case_fails_even_with_positive_count_and_zero_failures() throws Exception {
        assertRejected(report(List.of(EXPECTED.get(0))), EXPECTED.get(1));
    }

    @Test
    void missing_suite_is_reported_by_its_case_ids() throws Exception {
        List<String> expected = JUnitReportAssertions.expectedCases(integrationTestDir(), "paper-26.3");
        List<String> actual = expected.stream()
                                      .filter(id -> !id.startsWith("ScenarioTest."))
                                      .toList();
        assertThatThrownBy(() -> JUnitReportAssertions.assertSucceeded(REPORT,
                                                                       report(actual),
                                                                       expected)).isInstanceOf(AssertionError.class)
                                                                                 .hasMessageContaining("ScenarioTest.lateRegisteredCommandSurvivesReload");
    }

    @Test
    void replacement_and_duplicate_cases_fail_even_when_count_is_unchanged() throws Exception {
        assertRejected(report(List.of(EXPECTED.get(0), "UnexpectedTest.case")), "UnexpectedTest.case");
        assertRejected(report(List.of(EXPECTED.get(0), EXPECTED.get(0))), "Found duplicate");
    }

    @Test
    void incorrect_summary_count_fails() throws Exception {
        Document report = report(EXPECTED);
        report.getDocumentElement()
              .setAttribute("tests", "100");
        assertRejected(report, "test count");
    }

    @Test
    void failed_or_skipped_cases_cannot_hide_behind_zero_summary_counters() throws Exception {
        for (String status : List.of("failure", "error", "skipped")) {
            Document report = report(EXPECTED);
            report.getElementsByTagName("testcase")
                  .item(0)
                  .appendChild(report.createElement(status));
            assertRejected(report, status + " elements");
        }
        for (String status : List.of("failures", "errors", "skipped")) {
            Document report = report(EXPECTED);
            report.getDocumentElement()
                  .setAttribute(status, "1");
            assertRejected(report, status + " count");
        }
    }

    @Test
    void every_positive_shared_runner_target_has_an_inventory() throws Exception {
        try (Stream<Path> targets = Files.list(integrationTestDir().resolve("targets"))) {
            for (Path target : targets.filter(path -> Files.isRegularFile(path.resolve("build.gradle.kts")))
                                      .toList()) {
                String configuration = Files.readString(target.resolve("build.gradle.kts"));
                if (configuration.contains("commandlib.integration.expectedEnableFailure")
                        || configuration.contains("commandlib.integration.testClass")) {
                    continue;
                }
                List<String> expected = JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                                            target.getFileName()
                                                                                  .toString());
                JUnitReportAssertions.assertSucceeded(REPORT, report(expected), expected);
            }
        }
    }

    @Test
    void version_exclusions_and_unknown_targets_are_explicit() throws Exception {
        assertThat(JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                       "spigot-1.16.5")).doesNotContain("ArgumentTest.longArgument");
        assertThat(JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                       "spigot-1.17.1")).contains("ArgumentTest.longArgument");
        assertThat(JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                       "paper-1.21.7")).doesNotContain("ArgumentTest.blockPredicateArgument");
        assertThat(JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                       "paper-1.21.8")).contains("ArgumentTest.blockPredicateArgument");
        assertThat(JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                       "paper-1.21.1")).contains("TypedNmsTest.everyTypedNmsClassLinks",
                                                                                 "ArgumentTest.blockPredicateArgument");
        assertThatThrownBy(() -> JUnitReportAssertions.expectedCases(integrationTestDir(),
                                                                     "unknown-target")).isInstanceOf(AssertionError.class)
                                                                                       .hasMessageContaining("unknown-target");
    }

    private static Path integrationTestDir() {
        return Path.of(System.getProperty("commandlib.integrationTestDir", "integration-test"));
    }

    private static void assertRejected(Document report, String message) {
        assertThatThrownBy(() -> JUnitReportAssertions.assertSucceeded(REPORT,
                                                                       report,
                                                                       EXPECTED)).isInstanceOf(AssertionError.class)
                                                                                 .hasMessageContaining(message);
    }

    private static Document report(List<String> ids) throws Exception {
        Document report = DocumentBuilderFactory.newInstance()
                                                .newDocumentBuilder()
                                                .newDocument();
        Element suite = report.createElement("testsuite");
        report.appendChild(suite);
        suite.setAttribute("tests", Integer.toString(ids.size()));
        for (String status : List.of("failures", "errors", "skipped")) {
            suite.setAttribute(status, "0");
        }
        for (String id : ids) {
            int separator = id.lastIndexOf('.');
            Element testCase = report.createElement("testcase");
            testCase.setAttribute("classname", id.substring(0, separator));
            testCase.setAttribute("name", id.substring(separator + 1));
            suite.appendChild(testCase);
        }
        return report;
    }
}
