package me.minimize.NexoJoin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MessageOption {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final Component EMPTY_LINE = Component.empty().decoration(TextDecoration.ITALIC, false);

    private final String id;
    private final String displayName;
    private final String message;
    private final String permission;
    private final Material material;
    private final List<String> lore;

    public MessageOption(String id, String displayName, String message, String permission, Material material, List<String> lore) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? id : displayName;
        this.message = message == null ? "" : message;
        this.permission = permission == null ? "" : permission;
        this.material = material == null ? Material.PAPER : material;
        this.lore = lore == null ? Collections.emptyList() : new ArrayList<>(lore);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessage() {
        return message;
    }

    public String getPermission() {
        return permission;
    }

    public Material getMaterial() {
        return material;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public boolean canUse(Player player) {
        return permission.isEmpty() || player.hasPermission(permission);
    }

    public Component asDisplayComponent() {
        return deserializeNonItalic(displayName);
    }

    public List<Component> buildLore(boolean selected) {
        List<Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(deserializeNonItalic(line));
        }
        if (!message.isEmpty()) {
            if (!components.isEmpty()) {
                components.add(EMPTY_LINE);
            }
            components.add(deserializeNonItalic("&7Preview:"));
            components.add(deserializeNonItalic("&f" + message.replace("%player%", "Player").replace("%displayname%", "Player")));
        }
        if (selected) {
            if (!components.isEmpty()) {
                components.add(EMPTY_LINE);
            }
            components.add(deserializeNonItalic("&aCurrently selected"));
        }
        return components;
    }

    public Component formatMessage(Player player) {
        if (message.isEmpty()) {
            return Component.empty();
        }
        String formatted = message
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName());
        return deserializeNonItalic(formatted);
    }

    private Component deserializeNonItalic(String input) {
        return LEGACY_SERIALIZER.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }
}
