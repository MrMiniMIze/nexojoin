package me.minimize.NexoJoin.listener;

import me.minimize.NexoJoin.MessageManager;
import me.minimize.NexoJoin.MessageOption;
import me.minimize.NexoJoin.MessageType;
import me.minimize.NexoJoin.PlayerDataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {
    private final MessageManager messageManager;
    private final PlayerDataManager playerDataManager;

    public JoinQuitListener(MessageManager messageManager, PlayerDataManager playerDataManager) {
        this.messageManager = messageManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MessageOption option = resolveOption(player, MessageType.JOIN);
        if (option == null) {
            return;
        }
        if (option.getMessage().isEmpty()) {
            event.joinMessage((Component) null);
            return;
        }
        event.joinMessage(option.formatMessage(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MessageOption option = resolveOption(player, MessageType.LEAVE);
        if (option == null) {
            return;
        }
        if (option.getMessage().isEmpty()) {
            event.quitMessage((Component) null);
            return;
        }
        event.quitMessage(option.formatMessage(player));
    }

    private MessageOption resolveOption(Player player, MessageType type) {
        MessageOption option = playerDataManager.getSelectedOption(player.getUniqueId(), type);
        if (option != null && option.canUse(player)) {
            return option;
        }
        if (option != null) {
            playerDataManager.setSelection(player.getUniqueId(), type, null);
        }

        MessageOption defaultOption = messageManager.getDefault(type);
        if (defaultOption != null && defaultOption.canUse(player)) {
            playerDataManager.setSelection(player.getUniqueId(), type, defaultOption.getId());
            return defaultOption;
        }

        MessageOption fallback = messageManager.getFirstAvailable(player, type);
        if (fallback != null) {
            playerDataManager.setSelection(player.getUniqueId(), type, fallback.getId());
        }
        return fallback;
    }
}
