package com.sunrisestudio.vipaccess.managers;

import com.sunrisestudio.vipaccess.VIPAccess;
import com.sunrisestudio.vipaccess.api.TempAccessGrantEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TempAccessManager {
    private final VIPAccess plugin;
    private final Map<UUID, Long> tempAccessMap = new HashMap<>();
    private final File dataFile;

    public TempAccessManager(VIPAccess plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "temp_access.yml");
        loadData();
    }

    public void grantAccess(UUID uuid, long durationMillis) {
        long expiry = System.currentTimeMillis() + durationMillis;
        tempAccessMap.put(uuid, expiry);

        TempAccessGrantEvent event = new TempAccessGrantEvent(uuid, expiry);
        Bukkit.getPluginManager().callEvent(event);

        saveData();
    }

    public void revokeAccess(UUID uuid) {
        tempAccessMap.remove(uuid);
        saveData();
    }

    public boolean hasAccess(UUID uuid) {
        if (!tempAccessMap.containsKey(uuid)) return false;
        long expiry = tempAccessMap.get(uuid);
        if (System.currentTimeMillis() > expiry) {
            tempAccessMap.remove(uuid);
            return false;
        }
        return true;
    }

    public Map<UUID, Long> getAllGrants() {
        return new HashMap<>(tempAccessMap);
    }

    public void loadData() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        tempAccessMap.clear();
        if (config.contains("data")) {
            for (String key : config.getConfigurationSection("data").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long expiry = config.getLong("data." + key);
                    if (System.currentTimeMillis() < expiry) {
                        tempAccessMap.put(uuid, expiry);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : tempAccessMap.entrySet()) {
            if (System.currentTimeMillis() < entry.getValue()) {
                config.set("data." + entry.getKey().toString(), entry.getValue());
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save temp access data!");
        }
    }
}