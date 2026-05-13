package net.kunmc.lab.integration.core;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private ExceptionUtil() {
    }
}
