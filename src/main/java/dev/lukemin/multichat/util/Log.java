package dev.lukemin.multichat.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Log {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Log() {
    }

    public static void info(String event, String message) {
        String thread = Thread.currentThread().getName();
        System.out.printf("%s [%s] %-12s %s%n", LocalDateTime.now().format(FORMATTER), thread, event, message);
    }

    public static void error(String event, String message, Exception exception) {
        info(event, message + " (" + exception.getClass().getSimpleName() + ": " + exception.getMessage() + ")");
    }
}

