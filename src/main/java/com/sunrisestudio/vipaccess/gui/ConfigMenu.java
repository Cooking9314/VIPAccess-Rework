package com.sunrisestudio.vipaccess.gui;

import com.sunrisestudio.vipaccess.VIPAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigMenu implements InventoryHolder, Listener {
    private final VIPAccess plugin;
    private Inventory inventory;

    public ConfigMenu(VIPAccess plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Component titleComponent = plugin.getLanguageManager().getMessage("gui-title");

        String titleString = LegacyComponentSerializer.legacySection().serialize(titleComponent);

        this.inventory = Bukkit.createInventory(this, 9, titleString);
        refreshItems();
        player.openInventory(inventory);
    }

    private void refreshItems() {
        boolean isEnabled = plugin.getConfig().getBoolean("enabled");

        ItemStack statusItem = new ItemStack(isEnabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta statusMeta = statusItem.getItemMeta();

        Component statusText = isEnabled
                ? plugin.getLanguageManager().getMessage("gui-status-on")
                : plugin.getLanguageManager().getMessage("gui-status-off");

        statusMeta.setDisplayName(serialize(statusText.decoration(TextDecoration.ITALIC, false)));

        Component loreComp = plugin.getLanguageManager().getMessage("gui-toggle-lore").decoration(TextDecoration.ITALIC, false);
        statusMeta.setLore(List.of(serialize(loreComp)));

        statusItem.setItemMeta(statusMeta);

        ItemStack permItem = new ItemStack(Material.PAPER);
        ItemMeta permMeta = permItem.getItemMeta();

        Component permLabel = plugin.getLanguageManager().getMessage("gui-perm-label").decoration(TextDecoration.ITALIC, false);
        permMeta.setDisplayName(serialize(permLabel));

        String currentPerm = plugin.getConfig().getString("required-permission", "unset");
        Component permLore = plugin.getLanguageManager().getMessage("gui-perm-current",
                Placeholder.unparsed("perm", currentPerm));

        permMeta.setLore(List.of(serialize(permLore.decoration(TextDecoration.ITALIC, false))));
        permItem.setItemMeta(permMeta);

        ItemStack reloadItem = new ItemStack(Material.COMPASS);
        ItemMeta reloadMeta = reloadItem.getItemMeta();

        Component reloadLabel = plugin.getLanguageManager().getMessage("gui-reload-label").decoration(TextDecoration.ITALIC, false);
        reloadMeta.setDisplayName(serialize(reloadLabel));

        reloadItem.setItemMeta(reloadMeta);

        inventory.setItem(2, permItem);
        inventory.setItem(4, statusItem);
        inventory.setItem(6, reloadItem);
    }

    private String serialize(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ConfigMenu) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();

            switch (event.getSlot()) {
                case 4:
                    boolean current = plugin.getConfig().getBoolean("enabled");
                    plugin.getConfig().set("enabled", !current);
                    plugin.saveConfig();
                    refreshItems();
                    player.updateInventory();
                    break;
                case 6:
                    plugin.reloadPlugin();
                    player.closeInventory();
                    String lang = plugin.getConfig().getString("language", "en");
                    plugin.getLanguageManager().sendMessage(player, "reload-success", Placeholder.unparsed("lang", lang));
                    break;
            }
        }
    }
}