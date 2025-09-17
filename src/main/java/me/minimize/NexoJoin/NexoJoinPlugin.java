package me.minimize.NexoJoin;

import me.minimize.NexoJoin.command.JoinMessageCommand;
import me.minimize.NexoJoin.listener.JoinQuitListener;
import me.minimize.NexoJoin.menu.MenuListener;
import me.minimize.NexoJoin.menu.MenuService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoJoinPlugin extends JavaPlugin {
    private MessageManager messageManager;
    private PlayerDataManager playerDataManager;
    private MenuService menuService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageManager = new MessageManager(this);
        messageManager.load();

        playerDataManager = new PlayerDataManager(this, messageManager);
        menuService = new MenuService(this, messageManager, playerDataManager);

        PluginCommand command = getCommand("joinmessages");
        if (command != null) {
            JoinMessageCommand joinMessageCommand = new JoinMessageCommand(this, menuService, messageManager);
            command.setExecutor(joinMessageCommand);
            command.setTabCompleter(joinMessageCommand);
        } else {
            getLogger().warning("joinmessages command is not defined in plugin.yml");
        }

        Bukkit.getPluginManager().registerEvents(new MenuListener(menuService, messageManager, playerDataManager), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(messageManager, playerDataManager), this);
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.save();
        }
    }
}
