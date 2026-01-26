package com.sunrisestudio.vipaccess.gui;

import com.sunrisestudio.vipaccess.VIPAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConfigMenu implements Listener {
    private final VIPAccess plugin;

    public ConfigMenu(VIPAccess plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        openDashboard(player);
    }

    private void openDashboard(Player player) {
        Inventory inv = createInventory(new MenuHolder(MenuType.DASHBOARD, 0), 27, "gui-title-dashboard");
        fillBackground(inv);

        inv.setItem(10, createItem(Material.COMPARATOR, "gui-item-settings", "gui-lore-settings"));
        inv.setItem(12, createItem(Material.PLAYER_HEAD, "gui-item-players", "gui-lore-players"));
        inv.setItem(14, createItem(Material.SHIELD, "gui-item-antibot", "gui-lore-antibot"));
        inv.setItem(16, createItem(Material.BOOK, "gui-item-stats", "gui-lore-stats"));

        player.openInventory(inv);
        playSound(player, Sound.BLOCK_CHEST_OPEN);
    }

    private void openSettings(Player player) {
        Inventory inv = createInventory(new MenuHolder(MenuType.SETTINGS, 0), 27, "gui-title-settings");
        fillBackground(inv);

        boolean enabled = plugin.getConfig().getBoolean("enabled");
        inv.setItem(11, createItem(
                enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                enabled ? "gui-status-on" : "gui-status-off",
                "gui-toggle-lore"
        ));

        String currentPerm = plugin.getConfig().getString("required-permission", "unset");
        inv.setItem(13, createItem(Material.PAPER, "gui-perm-label", "gui-perm-lore", Placeholder.unparsed("perm", currentPerm)));

        inv.setItem(15, createItem(Material.COMPASS, "gui-reload-label", "gui-reload-lore"));

        addBackButton(inv);
        player.openInventory(inv);
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    private void openAntiBot(Player player) {
        Inventory inv = createInventory(new MenuHolder(MenuType.ANTIBOT, 0), 27, "gui-title-antibot");
        fillBackground(inv);

        boolean panic = plugin.getBotManager().isLockdown();
        inv.setItem(13, createItem(
                panic ? Material.REDSTONE_BLOCK : Material.TNT,
                panic ? "gui-panic-on" : "gui-panic-off",
                "gui-panic-lore"
        ));

        String punishment = plugin.getConfig().getString("anti-bot.punishment", "KICK");
        inv.setItem(11, createItem(Material.IRON_AXE, "gui-punish-label", "gui-punish-lore", Placeholder.unparsed("mode", punishment)));

        int threshold = plugin.getConfig().getInt("anti-bot.threshold");
        inv.setItem(15, createItem(Material.REPEATER, "gui-threshold-label", "gui-threshold-lore", Placeholder.unparsed("amount", String.valueOf(threshold))));

        addBackButton(inv);
        player.openInventory(inv);
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    private void openTempAccess(Player player, int page) {
        Inventory inv = createInventory(new MenuHolder(MenuType.TEMP_ACCESS, page), 54, "gui-title-tempaccess");

        Map<UUID, Long> grants = plugin.getTempAccessManager().getAllGrants();
        List<UUID> players = new ArrayList<>(grants.keySet());

        int slotsPerPage = 45;
        int totalPages = (int) Math.ceil((double) players.size() / slotsPerPage);
        if (totalPages == 0) totalPages = 1;

        int start = page * slotsPerPage;
        int end = Math.min(start + slotsPerPage, players.size());

        for (int i = start; i < end; i++) {
            UUID uuid = players.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            long expiry = grants.get(uuid);
            long timeLeft = (expiry - System.currentTimeMillis()) / 1000;

            String timeStr = formatTime(timeLeft);

            ItemStack head = createHead(op, "gui-head-name", "gui-head-lore",
                    Placeholder.unparsed("player", op.getName() != null ? op.getName() : "Unknown"),
                    Placeholder.unparsed("time", timeStr));

            inv.setItem(i - start, head);
        }

        fillRow(inv, 5, Material.GRAY_STAINED_GLASS_PANE);
        addBackButton(inv, 49);

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "gui-prev-page", "gui-page-lore"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, "gui-next-page", "gui-page-lore"));
        }

        player.openInventory(inv);
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    private void addBackButton(Inventory inv) {
        addBackButton(inv, inv.getSize() - 9);
    }

    private void addBackButton(Inventory inv, int slot) {
        inv.setItem(slot, createItem(Material.BARRIER, "gui-back", "gui-back-lore"));
    }

    private void fillBackground(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, "gui-empty", null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }

    private void fillRow(Inventory inv, int row, Material mat) {
        ItemStack item = createItem(mat, "gui-empty", null);
        for (int i = row * 9; i < (row + 1) * 9; i++) {
            inv.setItem(i, item);
        }
    }

    private Inventory createInventory(MenuHolder holder, int size, String titleKey) {
        Component title = plugin.getLanguageManager().getMessage(titleKey);
        return Bukkit.createInventory(holder, size, LegacyComponentSerializer.legacySection().serialize(title));
    }

    private ItemStack createItem(Material mat, String nameKey, String loreKey, TagResolver... placeholders) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (!nameKey.equals("gui-empty")) {
            Component name = plugin.getLanguageManager().getMessage(nameKey, placeholders).decoration(TextDecoration.ITALIC, false);
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
        } else {
            meta.setDisplayName(" ");
        }

        if (loreKey != null) {
            Component loreComp = plugin.getLanguageManager().getMessage(loreKey, placeholders).decoration(TextDecoration.ITALIC, false);
            String loreRaw = LegacyComponentSerializer.legacySection().serialize(loreComp);
            meta.setLore(Arrays.asList(loreRaw.split("\\n")));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHead(OfflinePlayer player, String nameKey, String loreKey, TagResolver... placeholders) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);

        Component name = plugin.getLanguageManager().getMessage(nameKey, placeholders).decoration(TextDecoration.ITALIC, false);
        meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));

        Component loreComp = plugin.getLanguageManager().getMessage(loreKey, placeholders).decoration(TextDecoration.ITALIC, false);
        String loreRaw = LegacyComponentSerializer.legacySection().serialize(loreComp);
        meta.setLore(Arrays.asList(loreRaw.split("\\n")));

        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return String.format("%dh %dm", h, m);
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (event.getCurrentItem().getType() == Material.BARRIER) {
            if (holder.type == MenuType.DASHBOARD) {
                player.closeInventory();
            } else {
                openDashboard(player);
            }
            return;
        }

        switch (holder.type) {
            case DASHBOARD:
                handleDashboard(player, slot);
                break;
            case SETTINGS:
                handleSettings(player, slot);
                break;
            case ANTIBOT:
                handleAntiBot(player, slot);
                break;
            case TEMP_ACCESS:
                handleTempAccess(player, slot, holder.page, event.getCurrentItem());
                break;
        }
    }

    private void handleDashboard(Player player, int slot) {
        switch (slot) {
            case 10: openSettings(player); break;
            case 12: openTempAccess(player, 0); break;
            case 14: openAntiBot(player); break;
            case 16:
                plugin.getLanguageManager().sendMessage(player, "stats-message");
                break;
        }
    }

    private void handleSettings(Player player, int slot) {
        switch (slot) {
            case 11:
                boolean current = plugin.getConfig().getBoolean("enabled");
                plugin.getConfig().set("enabled", !current);
                plugin.saveConfig();
                openSettings(player);
                break;
            case 15:
                plugin.reloadPlugin();
                player.closeInventory();
                plugin.getLanguageManager().sendMessage(player, "reload-success", Placeholder.unparsed("lang", plugin.getConfig().getString("language")));
                break;
        }
    }

    private void handleAntiBot(Player player, int slot) {
        switch (slot) {
            case 11:
                List<String> modes = List.of("KICK", "BAN_IP", "WHITELIST");
                String current = plugin.getConfig().getString("anti-bot.punishment", "KICK");
                int index = modes.indexOf(current);
                String next = modes.get((index + 1) % modes.size());
                plugin.getConfig().set("anti-bot.punishment", next);
                plugin.saveConfig();
                openAntiBot(player);
                break;
            case 13:
                boolean panic = !plugin.getBotManager().isLockdown();
                plugin.getBotManager().setLockdown(panic);
                openAntiBot(player);
                if (panic) {
                    Bukkit.broadcast(plugin.getLanguageManager().getMessage("panic-enabled"));
                }
                break;
        }
    }

    private void handleTempAccess(Player player, int slot, int page, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            if (slot == 45) openTempAccess(player, page - 1);
            else if (slot == 53) openTempAccess(player, page + 1);
        } else if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            OfflinePlayer target = meta.getOwningPlayer();
            if (target != null) {
                plugin.getTempAccessManager().revokeAccess(target.getUniqueId());
                openTempAccess(player, page);
                plugin.getLanguageManager().sendMessage(player, "access-revoked", Placeholder.unparsed("player", target.getName()));
            }
        }
    }

    private static class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private final int page;

        public MenuHolder(MenuType type, int page) {
            this.type = type;
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }

    private enum MenuType {
        DASHBOARD, SETTINGS, ANTIBOT, TEMP_ACCESS
    }
}