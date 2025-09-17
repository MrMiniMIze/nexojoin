package me.minimize.NexoJoin.menu;

import me.minimize.NexoJoin.MessageManager;
import me.minimize.NexoJoin.MessageOption;
import me.minimize.NexoJoin.MessageType;
import me.minimize.NexoJoin.PlayerDataManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final MenuService menuService;
    private final MessageManager messageManager;
    private final PlayerDataManager playerDataManager;

    public MenuListener(MenuService menuService, MessageManager messageManager, PlayerDataManager playerDataManager) {
        this.menuService = menuService;
        this.messageManager = messageManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        String action = container.get(menuService.getActionKey(), PersistentDataType.STRING);
        if (action != null) {
            handleAction(player, holder, action);
            return;
        }

        if (holder.getView() != MenuView.SELECTION || holder.getMessageType() == null) {
            return;
        }

        String optionId = container.get(menuService.getOptionKey(), PersistentDataType.STRING);
        if (optionId == null) {
            return;
        }

        MessageType type = holder.getMessageType();
        MessageOption option = messageManager.getOption(type, optionId);
        if (option == null) {
            player.sendMessage(LEGACY_SERIALIZER.deserialize("&cThat message option is no longer available."));
            menuService.refreshSelectionMenu(player, type);
            return;
        }

        if (!option.canUse(player)) {
            player.sendMessage(LEGACY_SERIALIZER.deserialize("&cYou do not have permission to use this message."));
            return;
        }

        playerDataManager.setSelection(player.getUniqueId(), type, option.getId());
        player.sendMessage(LEGACY_SERIALIZER.deserialize((type == MessageType.JOIN ? "&aSelected join message: " : "&aSelected leave message: ") + option.getDisplayName()));
        menuService.refreshSelectionMenu(player, type);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (!(inventory.getHolder() instanceof MenuHolder)) {
            return;
        }

        event.setCancelled(true);
    }

    private void handleAction(Player player, MenuHolder holder, String action) {
        if (action.equalsIgnoreCase("back")) {
            menuService.openRootMenu(player);
            return;
        }

        if (!action.startsWith("open:")) {
            return;
        }

        String typeName = action.substring("open:".length());
        try {
            MessageType type = MessageType.valueOf(typeName);
            menuService.openSelectionMenu(player, type);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
