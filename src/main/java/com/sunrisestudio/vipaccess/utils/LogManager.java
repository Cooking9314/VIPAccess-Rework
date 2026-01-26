package com.sunrisestudio.vipaccess.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LogManager {
    private final JavaPlugin plugin;
    private final File logFile;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "logs.txt");
    }

    public void logAttempt(String playerName, String uuid, String ip) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }

                String logEntry = String.format("[%s] Player: %s | UUID: %s | IP: %s%n",
                        LocalDateTime.now().format(DATE_FORMAT),
                        playerName,
                        uuid,
                        ip
                );

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                    writer.write(logEntry);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save log entry", e);
            }
        });
    }
}