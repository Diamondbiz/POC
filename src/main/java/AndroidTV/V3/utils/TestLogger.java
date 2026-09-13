package AndroidTV.V3.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestLogger {
    private static PrintWriter writer;
    private static String logFileName;

    public static void init(String testName) {
        logFileName = "test-logs/" + testName + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log";
        try {
            writer = new PrintWriter(new FileWriter(logFileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        System.out.println(message);
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    public static void logStep(String stepNumber, String description) {
        log("📍 Step " + stepNumber + ": " + description);
    }

    public static void logSuccess(String message) {
        log("✅ " + message);
    }

    public static void logError(String message) {
        log("❌ " + message);
    }

    public static void logWarning(String message) {
        log("⚠️ " + message);
    }

    public static void close() {
        if (writer != null) {
            writer.close();
        }
    }
}