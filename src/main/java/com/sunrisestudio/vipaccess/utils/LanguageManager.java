package com.sunrisestudio.vipaccess.utils;

import com.sunrisestudio.vipaccess.VIPAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class LanguageManager {
    private final VIPAccess plugin;
    private YamlConfiguration langConfig;

    public LanguageManager(VIPAccess plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    public void reload() {
        loadLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        createLangFileIfNotExists(langFolder, "en.yml");
        createLangFileIfNotExists(langFolder, "ru.yml");

        String selectedLang = plugin.getConfig().getString("language", "en");
        File langFile = new File(langFolder, selectedLang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + selectedLang + ".yml not found! Falling back to en.yml");
            langFile = new File(langFolder, "en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void createLangFileIfNotExists(File folder, String fileName) {
        File file = new File(folder, fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("lang/" + fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    fillDefaults(config, fileName);
                    config.save(file);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create language file: " + fileName);
            }
        }
    }

    private void fillDefaults(YamlConfiguration c, String fileName) {
        c.set("prefix", "<gradient:gold:yellow>[VIPAccess]</gradient> ");
        c.set("kick-message", "<red>Access Denied!");
        c.set("command-help-grant", "<gray>/vipaccess grant <player> <time> <dark_gray>- <white>Give temp access");
        c.set("grant-success", "<green>Granted access to <gold><player></gold> for <aqua><time></aqua>");
        c.set("invalid-time-format", "<red>Invalid time format! Use 1h, 30m, 1d.");
        c.set("console-enabled", "<gradient:green:aqua>VIPAccess Rework enabled successfully!</gradient>");
        c.set("command-only-players", "<red>Only for players!");
        c.set("antibot-disabled", "<green>Anti-Bot protection disabled.");
        c.set("antibot-enabled", "<red>!!! ANTI-BOT PROTECTION ENABLED !!! Threshold reached.");
        c.set("antibot-ban-reason", "Anti-Bot Protection");
        c.set("antibot-kick-ban", "<red>IP Banned due to bot attack.");
        c.set("antibot-kick-temp", "<red>Anti-Bot Protection Active. Try again later.");
    }

    public Component getMessage(String key) {
        return getMessage(key, new TagResolver[0]);
    }

    public Component getMessage(String key, TagResolver... placeholders) {
        String raw = langConfig.getString(key, "<red>Missing key: " + key);
        String prefix = langConfig.getString("prefix", "");

        if (key.startsWith("kick-message") || key.startsWith("gui") || key.startsWith("console") || key.startsWith("antibot")) {
            return MiniMessage.miniMessage().deserialize(raw, placeholders);
        }

        return MiniMessage.miniMessage().deserialize(prefix + raw, placeholders);
    }

    public String getRawString(String key) {
        return langConfig.getString(key, "Missing key: " + key);
    }

    public void sendMessage(CommandSender sender, String key, TagResolver... placeholders) {
        Component message = getMessage(key, placeholders);
        plugin.adventure().sender(sender).sendMessage(message);
    }

    public void sendMessage(CommandSender sender, Component message) {
        plugin.adventure().sender(sender).sendMessage(message);
    }
}