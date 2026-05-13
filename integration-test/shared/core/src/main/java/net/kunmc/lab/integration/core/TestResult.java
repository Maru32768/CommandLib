package net.kunmc.lab.integration.core;

import java.util.Objects;

public class TestResult {
    private final String key;
    private final TestStatus status;
    private final String message;

    public TestResult(String key, TestStatus status, String message) {
        this.key = key;
        this.status = status;
        this.message = message;
    }

    public String key() {
        return key;
    }

    public TestStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestResult that = (TestResult) o;

        if (!Objects.equals(key, that.key)) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestResult{" + "key='" + key + '\'' + ", status=" + status + ", message='" + message + '\'' + '}';
    }
}
