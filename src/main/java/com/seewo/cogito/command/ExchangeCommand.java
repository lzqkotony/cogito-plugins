package com.seewo.cogito.command;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /exchange [数量] —— 用 Vault 的钱兑换脑啡肽（单向，不可逆）。 */
public final class ExchangeCommand implements TabExecutor {

    private final CogitoPlugin plugin;

    public ExchangeCommand(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "<red>这个命令只能在游戏里执行");
            return true;
        }
        if (!plugin.vault().isReady()) {
            Messages.send(player, "<red>服务器没有可用的经济系统：需要安装 Vault + 一个经济插件");
            return true;
        }

        int amount = 1;
        if (args.length >= 1) {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException error) {
                Messages.send(player, "<red>数量必须是整数，例如 <white>/" + label + " 10");
                return true;
            }
        }
        if (amount <= 0) {
            Messages.send(player, "<red>数量必须大于 0");
            return true;
        }

        int max = Math.max(1, plugin.getConfig().getInt("economy.max-per-exchange", 64));
        if (amount > max) {
            Messages.send(player, "<red>单次最多兑换 <white>" + max + "</white> 个脑啡肽");
            return true;
        }

        double price = plugin.getConfig().getDouble("economy.price-per-enkephalin", 100D);
        double cost = price * amount;
        if (!plugin.vault().has(player, cost)) {
            Messages.send(player, "<red>余额不足：需要 <white>" + plugin.vault().format(cost)
                    + "</white>，你当前有 <white>" + plugin.vault().format(plugin.vault().balance(player)));
            return true;
        }
        if (!plugin.vault().withdraw(player, cost)) {
            Messages.send(player, "<red>扣款失败，请稍后再试");
            return true;
        }

        plugin.enkephalinItem().give(player, amount);
        Messages.send(player, "<green>兑换成功：<white>" + plugin.vault().format(cost)
                + "</white> → <white>" + amount + "</white> 个脑啡肽 <gray>（单向兑换，不可逆）");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        int max = Math.max(1, plugin.getConfig().getInt("economy.max-per-exchange", 64));
        List<String> suggestions = new ArrayList<>();
        for (int value : new int[]{1, 8, 16, 32, 64}) {
            if (value <= max && String.valueOf(value).startsWith(args[0])) {
                suggestions.add(String.valueOf(value));
            }
        }
        return suggestions;
    }
}
