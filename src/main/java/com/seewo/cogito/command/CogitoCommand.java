// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.command;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /cogito gui（/cog、/box）—— 打开「脑啡肽箱子」这个箱子样式的菜单。 */
public final class CogitoCommand implements TabExecutor {

    private static final String PERMISSION_ADMIN = "cogito.admin";
    private static final List<String> SUB_COMMANDS = List.of("gui", "help", "reload");

    private final CogitoPlugin plugin;

    public CogitoCommand(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return openMenu(sender);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui", "box", "menu" -> {
                return openMenu(sender);
            }
            case "help" -> {
                sendHelp(sender, label);
                return true;
            }
            case "reload" -> {
                return reload(sender);
            }
            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "<red>箱子界面只能在游戏里打开");
            return true;
        }
        plugin.openBox(player);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        Messages.raw(sender, "<gray>—— Cogito ——");
        Messages.raw(sender, "<yellow>/" + label + " gui <gray>- 打开脑啡肽箱子");
        Messages.raw(sender, "<yellow>/enkephalin <gray>- 查看自己持有的数量");
        Messages.raw(sender, "<yellow>/exchange <数量> <gray>- 用钱兑换脑啡肽");
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.raw(sender, "<yellow>/" + label + " reload <gray>- 重载 config.yml");
            Messages.raw(sender, "<yellow>/enkephalin give|take <玩家> <数量> <gray>- 发放 / 扣除");
        }
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String sub : SUB_COMMANDS) {
            if (sub.equals("reload") && !sender.hasPermission(PERMISSION_ADMIN)) {
                continue;
            }
            if (sub.startsWith(prefix)) {
                result.add(sub);
            }
        }
        return result;
    }
}
