package me.minimize.NexoJoin.menu;

import me.minimize.NexoJoin.MessageType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MenuHolder implements InventoryHolder {
    private final MenuView view;
    private final MessageType messageType;

    public MenuHolder(MenuView view, @Nullable MessageType messageType) {
        this.view = view;
        this.messageType = messageType;
    }

    public MenuView getView() {
        return view;
    }

    @Nullable
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("Menu inventories are managed directly");
    }
}
