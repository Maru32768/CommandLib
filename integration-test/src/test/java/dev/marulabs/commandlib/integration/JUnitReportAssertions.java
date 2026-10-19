package dev.marulabs.commandlib.integration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

final class JUnitReportAssertions {
    private JUnitReportAssertions() {
    }

    static List<String> expectedCases(Path integrationTestDir, String target) throws IOException {
        Path directory = integrationTestDir.resolve("expected-cases");
        Properties targets = properties(directory.resolve("targets.properties"));
        String profile = targets.getProperty(target);
        assertThat(profile).as("Expected case profile for target %s", target)
                           .isNotNull();
        List<String> expected = new ArrayList<>(Files.readAllLines(directory.resolve(profile + ".txt")));
        assertThat(expected).as("Expected case inventory for %s", target)
                            .isNotEmpty()
                            .doesNotHaveDuplicates();
        String excluded = properties(directory.resolve("exclusions.properties")).getProperty(target);
        if (excluded != null) {
            assertThat(expected).as("Documented exclusion for %s", target)
                                .contains(excluded);
            expected.remove(excluded);
        }
        return expected;
    }

    private static Properties properties(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }

    static void assertSucceeded(Path reportFile, Document report, List<String> expected) {
        Element suite = report.getDocumentElement();
        assertThat(suite.getTagName()).as("JUnit XML root in %s", reportFile)
                                      .isEqualTo("testsuite");
        assertThat(expected).as("Expected case inventory")
                            .isNotEmpty()
                            .doesNotHaveDuplicates();
        NodeList cases = suite.getElementsByTagName("testcase");
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < cases.getLength(); i++) {
            Element testCase = (Element) cases.item(i);
            actual.add(testCase.getAttribute("classname") + "." + testCase.getAttribute("name"));
        }
        assertThat(actual).as("JUnit XML case IDs in %s", reportFile)
                          .doesNotHaveDuplicates()
                          .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(Integer.parseInt(suite.getAttribute("tests"))).as("JUnit XML test count in %s", reportFile)
                                                                 .isEqualTo(expected.size());
        for (String status : List.of("failures", "errors", "skipped")) {
            assertThat(Integer.parseInt(suite.getAttribute(status))).as("JUnit XML %s count in %s", status, reportFile)
                                                                    .isZero();
        }
        // Inspect the cases too: incorrect summary counters must not hide a failed or skipped case.
        for (String status : List.of("failure", "error", "skipped")) {
            assertThat(suite.getElementsByTagName(status)
                            .getLength()).as("JUnit XML %s elements in %s", status, reportFile)
                                         .isZero();
        }
    }
}
