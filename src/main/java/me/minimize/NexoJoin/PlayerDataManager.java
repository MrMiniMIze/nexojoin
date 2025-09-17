package me.minimize.NexoJoin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {
    private final NexoJoinPlugin plugin;
    private final MessageManager messageManager;
    private final File dataFile;
    private YamlConfiguration dataConfiguration;

    public PlayerDataManager(NexoJoinPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.dataFile = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players.yml", e);
            }
        }
        dataConfiguration = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            dataConfiguration.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players.yml", e);
        }
    }

    public String getSelectedMessageId(UUID uuid, MessageType type) {
        return dataConfiguration.getString(uuid.toString() + "." + type.getStorageKey());
    }

    public MessageOption getSelectedOption(UUID uuid, MessageType type) {
        String id = getSelectedMessageId(uuid, type);
        if (id == null) {
            return null;
        }
        return messageManager.getOption(type, id);
    }

    public void setSelection(UUID uuid, MessageType type, String id) {
        String path = uuid.toString() + "." + type.getStorageKey();
        if (id == null || id.isEmpty()) {
            dataConfiguration.set(path, null);
        } else {
            dataConfiguration.set(path, id);
        }
        save();
    }
}
