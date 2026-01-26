package com.sunrisestudio.vipaccess;

import com.sunrisestudio.vipaccess.gui.ConfigMenu;
import com.sunrisestudio.vipaccess.managers.BotProtectionManager;
import com.sunrisestudio.vipaccess.managers.ProxyManager;
import com.sunrisestudio.vipaccess.managers.TempAccessManager;
import com.sunrisestudio.vipaccess.utils.LanguageManager;
import com.sunrisestudio.vipaccess.utils.LogManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VIPAccess extends JavaPlugin {

    private LogManager logManager;
    private LanguageManager languageManager;
    private BotProtectionManager botManager;
    private TempAccessManager tempAccessManager;
    private ProxyManager proxyManager;
    private ConfigMenu configMenu;
    private BukkitAudiences adventure;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.adventure = BukkitAudiences.create(this);

        this.logManager = new LogManager(this);
        this.languageManager = new LanguageManager(this);
        this.botManager = new BotProtectionManager(this);
        this.tempAccessManager = new TempAccessManager(this);
        this.proxyManager = new ProxyManager(this);
        this.configMenu = new ConfigMenu(this);

        VIPAccessListener mainListener = new VIPAccessListener(this, logManager, botManager, tempAccessManager);
        getServer().getPluginManager().registerEvents(mainListener, this);
        getServer().getPluginManager().registerEvents(configMenu, this);

        PluginCommand command = getCommand("vipaccess");
        if (command != null) {
            command.setExecutor(new VIPAccessCommand(this, configMenu, tempAccessManager));
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", proxyManager);

        languageManager.sendMessage(Bukkit.getConsoleSender(), "console-enabled");
    }

    @Override
    public void onDisable() {
        if (this.tempAccessManager != null) {
            this.tempAccessManager.saveData();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public void reloadPlugin() {
        reloadConfig();
        languageManager.reload();
        tempAccessManager.loadData();
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public TempAccessManager getTempAccessManager() {
        return tempAccessManager;
    }

    public BotProtectionManager getBotManager() {
        return botManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public @NonNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }
}