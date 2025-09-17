package me.minimize.NexoJoin.command;

import me.minimize.NexoJoin.MessageManager;
import me.minimize.NexoJoin.NexoJoinPlugin;
import me.minimize.NexoJoin.menu.MenuService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class JoinMessageCommand implements CommandExecutor, TabCompleter {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final NexoJoinPlugin plugin;
    private final MenuService menuService;
    private final MessageManager messageManager;

    public JoinMessageCommand(NexoJoinPlugin plugin, MenuService menuService, MessageManager messageManager) {
        this.plugin = plugin;
        this.menuService = menuService;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nexojoin.reload")) {
                sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cYou do not have permission to do that."));
                return true;
            }
            plugin.reloadConfig();
            messageManager.load();
            sender.sendMessage(LEGACY_SERIALIZER.deserialize("&aNexoJoin configuration reloaded."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cOnly players can use this command."));
            return true;
        }

        if (!player.hasPermission("nexojoin.command")) {
            sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cYou do not have permission to use this command."));
            return true;
        }

        menuService.openRootMenu(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("nexojoin.reload")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
