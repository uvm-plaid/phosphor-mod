package edu.columbia.cs.psl.phosphor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Logger {

    private static String logPath;

    public static String getLogPath() {
        return logPath;
    }

    public static void setLogPath(String logPath) {
        Logger.logPath = logPath;
        Logger.info("Logging to: " + logPath);
    }

    public static void log(String label, String message) {
        log(label, message, true);
    }

    public static void log(String label, String message, boolean logToConsole) {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
        String out = "[Phosphor] [" + label + "] [" + dateFormat.format(new Date()) + "] " + message;

        if (logToConsole) {
            System.out.println(out);
        }

        if (logPath == null) {
            return;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logPath, true));
            writer.write(out + "\n");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warning(String message) {
        log("WARNING", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

}
