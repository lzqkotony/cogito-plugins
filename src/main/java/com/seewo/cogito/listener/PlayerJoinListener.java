package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** 事件监听示例（进服提示），config.yml 里 join-message 留空即关闭。 */
public final class PlayerJoinListener implements Listener {

    private final CogitoPlugin plugin;

    public PlayerJoinListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String template = plugin.getConfig().getString("join-message", "");
        if (template == null || template.isBlank()) {
            return;
        }
        event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(template,
                Placeholder.unparsed("player", event.getPlayer().getName()),
                Placeholder.unparsed("online", String.valueOf(plugin.getServer().getOnlinePlayers().size()))));

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(event.getPlayer().getName() + " 加入了服务器");
        }
        Messages.raw(event.getPlayer(), "<gray>提示：<yellow>/enkephalin <gray>查看脑啡肽，<yellow>/exchange 10 <gray>用钱兑换");
    }
}
