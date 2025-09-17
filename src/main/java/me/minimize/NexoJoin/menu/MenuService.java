package me.minimize.NexoJoin.menu;

import me.minimize.NexoJoin.MessageManager;
import me.minimize.NexoJoin.MessageOption;
import me.minimize.NexoJoin.MessageType;
import me.minimize.NexoJoin.NexoJoinPlugin;
import me.minimize.NexoJoin.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

public class MenuService {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final NexoJoinPlugin plugin;
    private final MessageManager messageManager;
    private final PlayerDataManager playerDataManager;
    private final NamespacedKey optionKey;
    private final NamespacedKey actionKey;
    private final Logger logger;

    public MenuService(NexoJoinPlugin plugin, MessageManager messageManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.playerDataManager = playerDataManager;
        this.optionKey = new NamespacedKey(plugin, "message-option");
        this.actionKey = new NamespacedKey(plugin, "menu-action");
        this.logger = plugin.getLogger();
    }

    public NamespacedKey getOptionKey() {
        return optionKey;
    }

    public NamespacedKey getActionKey() {
        return actionKey;
    }

    public void openRootMenu(Player player) {
        ConfigurationSection rootSection = plugin.getConfig().getConfigurationSection("menu.root");
        int size = resolveInventorySize(rootSection, 27);
        String title = getString(rootSection, "title", "Messages");
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuView.ROOT, null), size, LEGACY_SERIALIZER.deserialize(title));

        ConfigurationSection itemsSection = rootSection == null ? null : rootSection.getConfigurationSection("items");

        ItemStack joinItem = createConfiguredMenuItem(
                itemsSection == null ? null : itemsSection.getConfigurationSection("join"),
                "menu.root.items.join",
                Material.LIME_DYE,
                getString(rootSection, "join-title", "Join Messages"),
                List.of("&7Select your join message."));
        ItemStack leaveItem = createConfiguredMenuItem(
                itemsSection == null ? null : itemsSection.getConfigurationSection("leave"),
                "menu.root.items.leave",
                Material.RED_DYE,
                getString(rootSection, "leave-title", "Leave Messages"),
                List.of("&7Select your leave message."));

        applyAction(joinItem, "open:JOIN");
        applyAction(leaveItem, "open:LEAVE");

        Set<Integer> usedSlots = new HashSet<>();

        int joinSlot = resolveItemSlot(size, itemsSection, "join", 11, usedSlots);
        if (joinSlot >= 0) {
            usedSlots.add(joinSlot);
            inventory.setItem(joinSlot, joinItem);
        }

        int leaveSlot = resolveItemSlot(size, itemsSection, "leave", 15, usedSlots);
        if (leaveSlot >= 0) {
            usedSlots.add(leaveSlot);
            inventory.setItem(leaveSlot, leaveItem);
        }

        player.openInventory(inventory);
    }

    public void openSelectionMenu(Player player, MessageType type) {
        ConfigurationSection selectionSection = plugin.getConfig().getConfigurationSection("menu.selection");
        int size = resolveInventorySize(selectionSection, 54);
        String titleKey = type == MessageType.JOIN ? "join.title" : "leave.title";
        String fallbackTitle = type == MessageType.JOIN ? "Join Messages" : "Leave Messages";
        String title = getString(selectionSection, titleKey, fallbackTitle);
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuView.SELECTION, type), size, LEGACY_SERIALIZER.deserialize(title));

        int backSlot = resolveSlot(selectionSection, size, "back-item.slot", size - 1);
        int emptySlot = resolveSlot(selectionSection, size, "empty.slot", 22);

        List<MessageOption> options = messageManager.getAvailable(player, type);
        String selectedId = playerDataManager.getSelectedMessageId(player.getUniqueId(), type);
        String currentSelectedId = selectedId;
        boolean hasSelection = currentSelectedId != null && options.stream().anyMatch(option -> option.getId().equalsIgnoreCase(currentSelectedId));
        if (!hasSelection) {
            if (!options.isEmpty()) {
                MessageOption first = options.get(0);
                selectedId = first.getId();
                playerDataManager.setSelection(player.getUniqueId(), type, selectedId);
            } else if (selectedId != null) {
                playerDataManager.setSelection(player.getUniqueId(), type, null);
                selectedId = null;
            }
        }

        List<Integer> configuredSlots = getConfiguredSlots(selectionSection);
        Set<Integer> reservedSlots = new HashSet<>();
        if (backSlot >= 0) {
            reservedSlots.add(backSlot);
        }
        if (emptySlot >= 0) {
            reservedSlots.add(emptySlot);
        }

        Set<Integer> usedSlots = new HashSet<>();
        int optionIndex = 0;
        for (MessageOption option : options) {
            ItemStack item = new ItemStack(option.getMaterial());
            ItemMeta meta = item.getItemMeta();
            boolean selected = option.getId().equalsIgnoreCase(selectedId);
            meta.displayName(option.asDisplayComponent());
            meta.lore(option.buildLore(selected));
            if (selected) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            }
            meta.getPersistentDataContainer().set(optionKey, PersistentDataType.STRING, option.getId());
            item.setItemMeta(meta);

            int slot = resolveOptionSlot(optionIndex++, configuredSlots, size, usedSlots, reservedSlots);
            if (slot >= 0) {
                usedSlots.add(slot);
                inventory.setItem(slot, item);
            }
        }

        if (options.isEmpty() && emptySlot >= 0) {
            ItemStack placeholder = createEmptyPlaceholder(selectionSection);
            inventory.setItem(emptySlot, placeholder);
        }

        ItemStack backItem = createBackItem(selectionSection);
        applyAction(backItem, "back");
        if (backSlot >= 0) {
            inventory.setItem(backSlot, backItem);
        }

        player.openInventory(inventory);
    }

    public void refreshSelectionMenu(Player player, MessageType type) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        if (holder.getView() != MenuView.SELECTION || holder.getMessageType() != type) {
            return;
        }
        openSelectionMenu(player, type);
    }

    private ItemStack createConfiguredMenuItem(ConfigurationSection itemSection, String path, Material defaultMaterial, String defaultName, List<String> defaultLore) {
        Material material = defaultMaterial;
        String name = defaultName;
        List<String> lore = defaultLore;

        if (itemSection != null) {
            String materialName = itemSection.getString("material");
            if (materialName != null && !materialName.isEmpty()) {
                Material configured = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                if (configured != null) {
                    material = configured;
                } else {
                    logger.warning("Invalid material '" + materialName + "' configured at " + path + ".material. Using " + defaultMaterial + ".");
                }
            }
            name = itemSection.getString("name", name);
            List<String> configuredLore = itemSection.getStringList("lore");
            if (!configuredLore.isEmpty()) {
                lore = configuredLore;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(deserializeNonItalic(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(this::deserializeNonItalic).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem(ConfigurationSection selectionSection) {
        ConfigurationSection backSection = selectionSection == null ? null : selectionSection.getConfigurationSection("back-item");
        Material material = Material.ARROW;
        String name = "&cBack";
        List<String> lore = new ArrayList<>();
        if (backSection != null) {
            String materialName = backSection.getString("material");
            if (materialName != null && !materialName.isEmpty()) {
                Material configured = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                if (configured != null) {
                    material = configured;
                } else {
                    logger.warning("Invalid material '" + materialName + "' configured at menu.selection.back-item.material. Using ARROW.");
                }
            }
            name = backSection.getString("name", name);
            List<String> configuredLore = backSection.getStringList("lore");
            if (!configuredLore.isEmpty()) {
                lore = configuredLore;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(deserializeNonItalic(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(this::deserializeNonItalic).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyPlaceholder(ConfigurationSection selectionSection) {
        ConfigurationSection emptySection = selectionSection == null ? null : selectionSection.getConfigurationSection("empty");
        Material material = Material.BARRIER;
        String name = "&cNo messages available";
        List<String> lore = List.of("&7You do not have access to any messages.");
        if (emptySection != null) {
            String materialName = emptySection.getString("material");
            if (materialName != null && !materialName.isEmpty()) {
                Material configured = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                if (configured != null) {
                    material = configured;
                } else {
                    logger.warning("Invalid material '" + materialName + "' configured at menu.selection.empty.material. Using BARRIER.");
                }
            }
            name = emptySection.getString("name", name);
            List<String> configuredLore = emptySection.getStringList("lore");
            if (!configuredLore.isEmpty()) {
                lore = configuredLore;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(deserializeNonItalic(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(this::deserializeNonItalic).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private void applyAction(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
    }

    private Component deserializeNonItalic(String input) {
        return LEGACY_SERIALIZER.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    private String getString(ConfigurationSection section, String path, String def) {
        if (section == null) {
            return def;
        }
        return section.getString(path, def);
    }

    private int resolveItemSlot(int inventorySize, ConfigurationSection itemsSection, String key, int fallback, Set<Integer> usedSlots) {
        if (itemsSection == null) {
            return fallbackRootSlot(inventorySize, fallback, usedSlots);
        }
        ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
        if (itemSection == null) {
            return fallbackRootSlot(inventorySize, fallback, usedSlots);
        }
        boolean explicit = itemSection.isSet("slot");
        int configured = itemSection.getInt("slot", fallback);
        int slot = clampSlot(inventorySize, configured);
        if (slot == -1 && explicit) {
            logger.warning("Configured slot 'menu.root.items." + key + ".slot' is outside the menu size of " + inventorySize + ". Using fallback.");
        }
        if (slot == -1) {
            slot = clampSlot(inventorySize, fallback);
        }
        if (slot == -1) {
            slot = findNextAvailableSlot(inventorySize, usedSlots);
        }
        if (slot >= 0 && usedSlots.contains(slot)) {
            logger.warning("Configured slot 'menu.root.items." + key + ".slot' is already occupied. Searching for another slot.");
            slot = findNextAvailableSlot(inventorySize, usedSlots);
        }
        return slot;
    }

    private int fallbackRootSlot(int inventorySize, int fallback, Set<Integer> usedSlots) {
        int slot = clampSlot(inventorySize, fallback);
        if (slot == -1) {
            slot = findNextAvailableSlot(inventorySize, usedSlots);
        }
        if (slot >= 0 && usedSlots.contains(slot)) {
            slot = findNextAvailableSlot(inventorySize, usedSlots);
        }
        return slot;
    }

    private int resolveSlot(ConfigurationSection section, int inventorySize, String path, int fallback) {
        int configured = fallback;
        boolean explicit = section != null && section.isSet(path);
        if (section != null && section.isInt(path)) {
            configured = section.getInt(path, fallback);
        }
        int slot = clampSlot(inventorySize, configured);
        if (slot == -1 && explicit) {
            logger.warning("Configured slot 'menu.selection." + path + "' is outside the menu size of " + inventorySize + ". Using fallback.");
        }
        if (slot == -1) {
            if (inventorySize > 0) {
                int sanitized = Math.min(Math.max(fallback, 0), inventorySize - 1);
                slot = clampSlot(inventorySize, sanitized);
            }
        }
        return slot;
    }

    private int clampSlot(int inventorySize, int slot) {
        if (inventorySize <= 0) {
            return -1;
        }
        if (slot < 0 || slot >= inventorySize) {
            return -1;
        }
        return slot;
    }

    private int resolveInventorySize(ConfigurationSection section, int fallback) {
        int size = fallback;
        boolean explicit = section != null && section.isSet("size");
        if (section != null && section.isInt("size")) {
            size = section.getInt("size", fallback);
        }
        int original = size;
        if (size < 9) {
            size = 9;
        }
        if (size > 54) {
            size = 54;
        }
        if (size % 9 != 0) {
            size = ((size / 9) + 1) * 9;
            if (size > 54) {
                size = 54;
            }
        }
        if (explicit && size != original) {
            logger.warning("Adjusted inventory size from " + original + " to " + size + " to satisfy inventory constraints.");
        }
        return size;
    }

    private List<Integer> getConfiguredSlots(ConfigurationSection selectionSection) {
        if (selectionSection == null) {
            return List.of();
        }
        List<Integer> slots = selectionSection.getIntegerList("option-slots");
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        List<Integer> normalized = new ArrayList<>();
        for (Integer slot : slots) {
            if (slot != null) {
                normalized.add(slot);
            }
        }
        return normalized;
    }

    private int resolveOptionSlot(int index, List<Integer> configuredSlots, int size, Set<Integer> usedSlots, Set<Integer> reservedSlots) {
        if (index < configuredSlots.size()) {
            int configured = configuredSlots.get(index);
            if (configured >= 0 && configured < size && !usedSlots.contains(configured) && !reservedSlots.contains(configured)) {
                return configured;
            }
            logger.warning("Configured slot 'menu.selection.option-slots[" + index + "]' is not available in the current menu.");
        }
        for (int i = 0; i < size; i++) {
            if (!usedSlots.contains(i) && !reservedSlots.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    private int findNextAvailableSlot(int size, Set<Integer> usedSlots) {
        for (int i = 0; i < size; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        return -1;
    }
}
