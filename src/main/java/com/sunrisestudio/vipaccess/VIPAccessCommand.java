package com.sunrisestudio.vipaccess;

import com.sunrisestudio.vipaccess.gui.ConfigMenu;
import com.sunrisestudio.vipaccess.managers.TempAccessManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class VIPAccessCommand implements CommandExecutor, TabCompleter {
    private final VIPAccess plugin;
    private final ConfigMenu configMenu;
    private final TempAccessManager tempAccessManager;

    public VIPAccessCommand(VIPAccess plugin, ConfigMenu configMenu, TempAccessManager tempAccessManager) {
        this.plugin = plugin;
        this.configMenu = configMenu;
        this.tempAccessManager = tempAccessManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vipaccess.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    configMenu.open(player);
                } else {
                    plugin.getLanguageManager().sendMessage(sender, "command-only-players");
                }
                break;
            case "reload":
                plugin.reloadPlugin();
                String lang = plugin.getConfig().getString("language", "en");
                plugin.getLanguageManager().sendMessage(sender, "reload-success", Placeholder.unparsed("lang", lang));
                break;
            case "grant":
                handleGrant(sender, args);
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, "command-help-grant");
            return;
        }

        String targetName = args[1];
        String timeString = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        long duration = parseTime(timeString);

        if (duration <= 0) {
            plugin.getLanguageManager().sendMessage(sender, "invalid-time-format");
            return;
        }

        tempAccessManager.grantAccess(target.getUniqueId(), duration);
        plugin.getLanguageManager().sendMessage(sender, "grant-success",
                Placeholder.unparsed("player", targetName),
                Placeholder.unparsed("time", timeString));
    }

    private long parseTime(String time) {
        try {
            long multiplier = 1000;
            if (time.endsWith("s")) {
                time = time.substring(0, time.length() - 1);
            } else if (time.endsWith("m")) {
                multiplier *= 60;
                time = time.substring(0, time.length() - 1);
            } else if (time.endsWith("h")) {
                multiplier *= 3600;
                time = time.substring(0, time.length() - 1);
            } else if (time.endsWith("d")) {
                multiplier *= 86400;
                time = time.substring(0, time.length() - 1);
            }
            return Long.parseLong(time) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void sendHelp(CommandSender sender) {
        plugin.getLanguageManager().sendMessage(sender, "command-help-header");
        plugin.getLanguageManager().sendMessage(sender, "command-help-gui");
        plugin.getLanguageManager().sendMessage(sender, "command-help-reload");
        plugin.getLanguageManager().sendMessage(sender, "command-help-grant");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vipaccess.admin")) return List.of();
        if (args.length == 1) {
            return filter(List.of("gui", "reload", "grant"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("grant")) {
            return null;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grant")) {
            return filter(List.of("1h", "30m", "1d"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String arg) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }
}