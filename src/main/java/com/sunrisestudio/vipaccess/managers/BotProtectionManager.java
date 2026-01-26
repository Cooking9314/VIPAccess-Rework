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
    private boolean lockdownMode = false;
    private long protectionEndTime = 0;

    public BotProtectionManager(VIPAccess plugin) {
        this.plugin = plugin;
        startResetTask();
    }

    private void startResetTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int count = connectionCounter.getAndSet(0);

            if (protectionMode && System.currentTimeMillis() > protectionEndTime && !lockdownMode) {
                protectionMode = false;
                plugin.getLanguageManager().sendMessage(Bukkit.getConsoleSender(), "antibot-disabled");
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
        plugin.getLanguageManager().sendMessage(Bukkit.getConsoleSender(), "antibot-enabled");
    }

    public boolean isUnderAttack() {
        return protectionMode || lockdownMode;
    }

    public boolean isLockdown() {
        return lockdownMode;
    }

    public void setLockdown(boolean lockdown) {
        this.lockdownMode = lockdown;
    }

    public void punish(InetAddress address, PlayerLoginEvent event) {
        if (lockdownMode) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getLanguageManager().getMessage("panic-kick-message"));
            return;
        }

        String punishmentType = plugin.getConfig().getString("anti-bot.punishment", "KICK").toUpperCase();

        if ("BAN_IP".equals(punishmentType)) {
            String ip = address.getHostAddress();
            String banReason = plugin.getLanguageManager().getRawString("antibot-ban-reason");

            Bukkit.getBanList(BanList.Type.IP).addBan(ip, banReason,
                    Date.from(Instant.now().plusSeconds(600)), "VIPAccess");

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    plugin.getLanguageManager().getMessage("antibot-kick-ban"));
        } else if ("WHITELIST".equals(punishmentType)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST,
                    plugin.getLanguageManager().getMessage("antibot-kick-whitelist"));
        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getLanguageManager().getMessage("antibot-kick-temp"));
        }
    }
}