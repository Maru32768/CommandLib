package net.kunmc.lab.integration.core;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class JUnitXmlReport {
    public static String build(List<TestResult> results) {
        long failedCount = results.stream()
                                  .filter(x -> x.status() == TestStatus.FAILED)
                                  .count();
        String testCases = results.stream()
                                  .sorted(Comparator.comparing(TestResult::key))
                                  .map(JUnitXmlReport::buildTestCaseXml)
                                  .collect(Collectors.joining());
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<testsuite name=\"CommandLib Test Plugin\" tests=\"" + results.size() + "\" failures=\"" + failedCount + "\" errors=\"0\" skipped=\"0\">\n" + testCases + "</testsuite>\n";
    }

    private static String buildTestCaseXml(TestResult result) {
        String className = testClassName(result.key());
        String testName = testMethodName(result.key());
        if (result.status() == TestStatus.SUCCEEDED) {
            return "  <testcase classname=\"" + xmlEscape(className) + "\" name=\"" + xmlEscape(testName) + "\"/>\n";
        }
        return "  <testcase classname=\"" + xmlEscape(className) + "\" name=\"" + xmlEscape(testName) + "\">\n" + "    <failure message=\"" + xmlEscape(
                firstLine(result.message())) + "\">" + xmlEscape(result.message()) + "</failure>\n" + "  </testcase>\n";
    }

    private static String testClassName(String key) {
        int separator = key.lastIndexOf('.');
        return separator == -1 ? "CommandLibTest" : key.substring(0, separator);
    }

    private static String testMethodName(String key) {
        int separator = key.lastIndexOf('.');
        return separator == -1 ? key : key.substring(separator + 1);
    }

    private static String firstLine(String value) {
        int newLine = value.indexOf('\n');
        return newLine == -1 ? value : value.substring(0, newLine);
    }

    private static String xmlEscape(String value) {
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    private JUnitXmlReport() {
    }
}
