package me.minimize.NexoJoin;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class MessageManager {
    private final NexoJoinPlugin plugin;
    private final Map<MessageType, Map<String, MessageOption>> options = new EnumMap<>(MessageType.class);
    private final Map<MessageType, String> defaultOptions = new EnumMap<>(MessageType.class);

    public MessageManager(NexoJoinPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        options.clear();
        defaultOptions.clear();

        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        for (MessageType type : MessageType.values()) {
            ConfigurationSection section = config.getConfigurationSection(type.getConfigKey());
            Map<String, MessageOption> typeOptions = new LinkedHashMap<>();

            if (section == null) {
                logger.warning("No configuration section found for " + type.getConfigKey());
            } else {
                for (String id : section.getKeys(false)) {
                    ConfigurationSection optionSection = section.getConfigurationSection(id);
                    if (optionSection == null) {
                        continue;
                    }

                    String displayName = optionSection.getString("display-name", id);
                    String message = optionSection.getString("message", "");
                    String permission = optionSection.getString("permission", "");
                    String materialName = optionSection.getString("material", "PAPER");
                    List<String> lore = optionSection.getStringList("lore");

                    Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                    if (material == null) {
                        logger.warning("Invalid material '" + materialName + "' for option '" + id + "' in " + type.getConfigKey() + ". Using PAPER.");
                        material = Material.PAPER;
                    }

                    MessageOption option = new MessageOption(id, displayName, message, permission, material, lore);
                    typeOptions.put(id, option);
                }
            }

            if (typeOptions.isEmpty()) {
                logger.warning("No message options defined for " + type.name().toLowerCase(Locale.ROOT) + ".");
            }

            options.put(type, typeOptions);

            String defaultId = config.getString("defaults." + type.getStorageKey());
            if (defaultId == null || !typeOptions.containsKey(defaultId)) {
                if (defaultId != null) {
                    logger.warning("Default option '" + defaultId + "' for " + type.name().toLowerCase(Locale.ROOT) + " not found. Using the first available option.");
                }
                defaultId = typeOptions.keySet().stream().findFirst().orElse(null);
            }
            defaultOptions.put(type, defaultId);
        }
    }

    public Map<String, MessageOption> getOptions(MessageType type) {
        Map<String, MessageOption> typeOptions = options.get(type);
        return typeOptions == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeOptions);
    }

    public MessageOption getOption(MessageType type, String id) {
        Map<String, MessageOption> typeOptions = options.get(type);
        if (typeOptions == null) {
            return null;
        }
        return typeOptions.get(id);
    }

    public String getDefaultId(MessageType type) {
        return defaultOptions.get(type);
    }

    public MessageOption getDefault(MessageType type) {
        String id = getDefaultId(type);
        if (id == null) {
            return null;
        }
        return getOption(type, id);
    }

    public MessageOption getFirstAvailable(Player player, MessageType type) {
        Map<String, MessageOption> typeOptions = options.get(type);
        if (typeOptions == null || typeOptions.isEmpty()) {
            return null;
        }
        for (MessageOption option : typeOptions.values()) {
            if (option.canUse(player)) {
                return option;
            }
        }
        return null;
    }

    public List<MessageOption> getAvailable(Player player, MessageType type) {
        Map<String, MessageOption> typeOptions = options.get(type);
        if (typeOptions == null || typeOptions.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageOption> result = new ArrayList<>();
        for (MessageOption option : typeOptions.values()) {
            if (option.canUse(player)) {
                result.add(option);
            }
        }
        return result;
    }
}
