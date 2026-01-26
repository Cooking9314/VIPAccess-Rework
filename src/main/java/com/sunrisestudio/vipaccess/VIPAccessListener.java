package com.sunrisestudio.vipaccess;

import com.sunrisestudio.vipaccess.api.VIPLoginEvent;
import com.sunrisestudio.vipaccess.managers.BotProtectionManager;
import com.sunrisestudio.vipaccess.utils.LogManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VIPAccessListener implements Listener {

    private final VIPAccess plugin;
    private final LogManager logManager;
    private final BotProtectionManager botManager;
    private final Cache<UUID, Boolean> spamCache;

    public VIPAccessListener(VIPAccess plugin, LogManager logManager, BotProtectionManager botManager) {
        this.plugin = plugin;
        this.logManager = logManager;
        this.botManager = botManager;
        this.spamCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", true)) return;

        Player player = event.getPlayer();
        String requiredPerm = plugin.getConfig().getString("required-permission", "server.vip");
        String bypassPerm = "vipaccess.bypass";

        boolean hasAccess = player.hasPermission(requiredPerm) || player.hasPermission(bypassPerm);

        if (!hasAccess) {
            if (botManager.isUnderAttack()) {
                botManager.punish(event.getAddress(), event);
                return;
            }
            botManager.registerFailedAttempt();
        }

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;

        if (hasAccess) {
            return;
        }

        String rawKickMsg = plugin.getLanguageManager().getRawString("kick-message");
        VIPLoginEvent apiEvent = new VIPLoginEvent(player, rawKickMsg);
        Bukkit.getPluginManager().callEvent(apiEvent);

        if (apiEvent.isCancelled()) {
            return;
        }

        String finalMsgString = apiEvent.getKickMessage();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            finalMsgString = PlaceholderAPI.setPlaceholders(player, finalMsgString);
        }

        Component kickComponent = plugin.getMiniMessage().deserialize(finalMsgString);

        String legacyKickMessage = LegacyComponentSerializer.legacySection().serialize(kickComponent);

        event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);

        event.setKickMessage(legacyKickMessage);

        if (spamCache.getIfPresent(player.getUniqueId()) != null) {
            return;
        }
        spamCache.put(player.getUniqueId(), true);

        String ip = event.getAddress().getHostAddress();
        logManager.logAttempt(player.getName(), player.getUniqueId().toString(), ip);
        notifyAdmins(player);
    }

    private void notifyAdmins(Player player) {
        Component msg = plugin.getLanguageManager().getMessage("admin-notify",
                Placeholder.unparsed("name", player.getName()));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("vipaccess.admin"))
                .forEach(p -> plugin.getLanguageManager().sendMessage(p, msg));
    }
}