package com.sunrisestudio.vipaccess.managers;

import com.sunrisestudio.vipaccess.VIPAccess;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class BotProtectionManager {
    private final VIPAccess plugin;
    private final AtomicInteger connectionCounter = new AtomicInteger(0);
    private boolean protectionMode = false;
    private long protectionEndTime = 0;

    public BotProtectionManager(VIPAccess plugin) {
        this.plugin = plugin;
        startResetTask();
    }

    private void startResetTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int count = connectionCounter.getAndSet(0);

            if (protectionMode && System.currentTimeMillis() > protectionEndTime) {
                protectionMode = false;
                plugin.getLogger().info("Anti-Bot protection disabled.");
            }

            if (count > 5 && !protectionMode) {
            }
        }, 20L, 20L);
    }

    public void registerFailedAttempt() {
        if (!plugin.getConfig().getBoolean("anti-bot.enabled", false)) return;

        int currentCount = connectionCounter.incrementAndGet();
        int threshold = plugin.getConfig().getInt("anti-bot.threshold", 50);

        if (!protectionMode && currentCount >= threshold) {
            enableProtection();
        }
    }

    private void enableProtection() {
        protectionMode = true;
        int durationSeconds = plugin.getConfig().getInt("anti-bot.protection-duration", 60);
        protectionEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        plugin.getLogger().warning("!!! ANTI-BOT PROTECTION ENABLED !!! Threshold reached.");
    }

    public boolean isUnderAttack() {
        return protectionMode;
    }

    public void punish(InetAddress address, PlayerLoginEvent event) {
        String punishmentType = plugin.getConfig().getString("anti-bot.punishment", "KICK").toUpperCase();

        if ("BAN_IP".equals(punishmentType)) {
            String ip = address.getHostAddress();
            Bukkit.getBanList(BanList.Type.IP).addBan(ip, "Anti-Bot Protection",
                    Date.from(Instant.now().plusSeconds(600)), "VIPAccess"); // Бан на 10 минут
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    plugin.getMiniMessage().deserialize("<red>IP Banned due to bot attack."));
        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getMiniMessage().deserialize("<red>Anti-Bot Protection Active. Try again later."));
        }
    }
}