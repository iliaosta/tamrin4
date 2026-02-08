package rps.server.core.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String LOG_FILE = "game_logs.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public synchronized void log(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;

        // console
        System.out.println(line);

        // file
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(line);
        } catch (IOException e) {
            System.out.println("⚠️ Failed to write log file: " + e.getMessage());
        }
    }
}
