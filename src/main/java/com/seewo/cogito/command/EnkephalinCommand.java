package com.seewo.cogito.command;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /enkephalin —— 查看持有量，以及管理员用的发放 / 扣除 / 配置查看 / 重载。 */
public final class EnkephalinCommand implements TabExecutor {

    private static final String PERMISSION_ADMIN = "cogito.admin";
    private static final List<String> SUB_COMMANDS = List.of("give", "take", "info", "reload");

    private final CogitoPlugin plugin;

    public EnkephalinCommand(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("count")) {
            return showCount(sender, label);
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> {
                return change(sender, label, args, true);
            }
            case "take" -> {
                return change(sender, label, args, false);
            }
            case "info" -> {
                return showInfo(sender);
            }
            case "reload" -> {
                return reload(sender);
            }
            default -> {
                sendUsage(sender, label);
                return true;
            }
        }
    }

    private boolean showCount(CommandSender sender, String label) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "<yellow>控制台看不到背包，用法：<white>/" + label + " give <玩家> <数量>");
            return true;
        }
        int amount = plugin.enkephalinItem().count(player);
        Messages.send(player, "你当前持有 <white>" + amount + "</white> 个脑啡肽");
        return true;
    }

    private boolean change(CommandSender sender, String label, String[] args, boolean giving) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 3) {
            Messages.send(sender, "<red>用法：<white>/" + label + " " + args[0].toLowerCase(Locale.ROOT)
                    + " <玩家> <数量>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.send(sender, "<red>找不到在线玩家 <white>" + args[1]);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException error) {
            Messages.send(sender, "<red>数量必须是整数");
            return true;
        }
        if (amount <= 0) {
            Messages.send(sender, "<red>数量必须大于 0");
            return true;
        }

        if (giving) {
            plugin.enkephalinItem().give(target, amount);
            Messages.send(sender, "<green>已给 <white>" + target.getName() + "</white> " + amount + " 个脑啡肽");
            Messages.send(target, "<green>你收到了 <white>" + amount + "</white> 个脑啡肽");
        } else {
            int removed = plugin.enkephalinItem().remove(target, amount);
            if (removed <= 0) {
                Messages.send(sender, "<red>" + target.getName() + " 身上没有脑啡肽");
                return true;
            }
            Messages.send(sender, "<green>已从 <white>" + target.getName() + "</white> 扣除 " + removed + " 个脑啡肽");
            Messages.send(target, "<red>你被扣除了 " + removed + " 个脑啡肽");
        }
        return true;
    }

    private boolean showInfo(CommandSender sender) {
        Messages.raw(sender, "<gray>—— 脑啡肽物品配置 ——");
        Messages.raw(sender, "<gray>材质：<white>" + plugin.enkephalinItem().material()
                + " <gray>| 标签：<white>" + plugin.enkephalinItem().tagKey()
                + " <gray>| 附魔：<white>" + plugin.enkephalinItem().enchantment());
        Messages.raw(sender, "<gray>CustomModelData：<white>" + plugin.enkephalinItem().customModelData());
        Messages.raw(sender, "<gray>兑换价：<white>"
                + plugin.getConfig().getDouble("economy.price-per-enkephalin", 100D)
                + " <gray>/ 个，单次上限 <white>" + plugin.getConfig().getInt("economy.max-per-exchange", 64)
                + " <gray>| 经济：<white>"
                + (plugin.vault().isReady() ? plugin.vault().economy().getName() : "未连接"));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        plugin.reloadPluginConfig();
        Messages.send(sender, "<green>配置已重载");
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        Messages.raw(sender, "<gray>—— 脑啡肽 ——");
        Messages.raw(sender, "<yellow>/" + label + " <gray>- 查看自己持有的数量");
        Messages.raw(sender, "<yellow>/exchange <数量> <gray>- 用钱兑换脑啡肽");
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.raw(sender, "<yellow>/" + label + " give <玩家> <数量> <gray>- 发放");
            Messages.raw(sender, "<yellow>/" + label + " take <玩家> <数量> <gray>- 扣除");
            Messages.raw(sender, "<yellow>/" + label + " info <gray>- 查看物品配置");
            Messages.raw(sender, "<yellow>/" + label + " reload <gray>- 重载 config.yml");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(player.getName());
                }
            }
            return result;
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            return List.of("1", "8", "16", "64");
        }
        return List.of();
    }
}
