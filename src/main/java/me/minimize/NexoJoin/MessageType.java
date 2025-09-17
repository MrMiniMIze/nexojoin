package me.minimize.NexoJoin;

public enum MessageType {
    JOIN("join-messages", "join"),
    LEAVE("leave-messages", "leave");

    private final String configKey;
    private final String storageKey;

    MessageType(String configKey, String storageKey) {
        this.configKey = configKey;
        this.storageKey = storageKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getStorageKey() {
        return storageKey;
    }
}
