package com.sunrisestudio.vipaccess;

import com.sunrisestudio.vipaccess.gui.ConfigMenu;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

    public VIPAccessCommand(VIPAccess plugin, ConfigMenu configMenu) {
        this.plugin = plugin;
        this.configMenu = configMenu;
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
                    sender.sendMessage("Only for players!");
                }
                break;
            case "reload":
                plugin.reloadPlugin();
                String lang = plugin.getConfig().getString("language", "en");
                plugin.getLanguageManager().sendMessage(sender, "reload-success", Placeholder.unparsed("lang", lang));
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getLanguageManager().sendMessage(sender, "command-help-header");
        plugin.getLanguageManager().sendMessage(sender, "command-help-gui");
        plugin.getLanguageManager().sendMessage(sender, "command-help-reload");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vipaccess.admin")) return List.of();
        if (args.length == 1) {
            return filter(List.of("gui", "reload"), args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String arg) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }
}