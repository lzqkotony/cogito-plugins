// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.command;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** /cogito gui（/cog、/box）—— 打开「脑啡肽箱子」这个箱子样式的菜单。 */
public final class CogitoCommand implements TabExecutor {

    private static final String PERMISSION_ADMIN = "cogito.admin";
    private static final List<String> SUB_COMMANDS =
            List.of("gui", "give", "items", "debug", "help", "reload");

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
            case "give" -> {
                return giveItem(sender, label, args);
            }
            case "items" -> {
                return listItems(sender);
            }
            case "debug" -> {
                return debugItem(sender, args);
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
            Messages.raw(sender, "<yellow>/" + label + " items <gray>- 列出所有已注册物品");
            Messages.raw(sender, "<yellow>/" + label + " give <物品id> <玩家> <数量> <gray>- 发放物品");
            Messages.raw(sender, "<yellow>/" + label + " debug <物品id> <gray>- 造一个物品并打印它的数据");
            Messages.raw(sender, "<yellow>/" + label + " reload <gray>- 重载 config.yml");
            Messages.raw(sender, "<yellow>/enkephalin give|take <玩家> <数量> <gray>- 发放 / 扣除");
        }
    }

    /**
     * /cogito debug <物品id> —— 造一个物品，把它实际生成出来的数据打到聊天栏。
     *
     * <p>用途：服务器上不装客户端也能确认材质、堆叠上限、NBT 标签、识别是否正常。
     */
    private boolean debugItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 2) {
            Messages.send(sender, "<red>用法：<white>/cogito debug <物品id>");
            return true;
        }
        CustomItem item = plugin.items().find(args[1]);
        if (item == null) {
            Messages.send(sender, "<red>没有这个物品：<white>" + args[1] + "</white>，可用：" + itemIds());
            return true;
        }

        ItemStack stack = item.create(1);
        ItemMeta meta = stack.getItemMeta();
        String tag = "无";
        if (meta != null) {
            String value = meta.getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "item"), PersistentDataType.STRING);
            if (value != null) {
                tag = plugin.getName().toLowerCase(Locale.ROOT) + ":item=" + value;
            }
        }
        Messages.raw(sender, "<gray>—— 物品自检：" + item.displayName() + " <gray>——");
        Messages.raw(sender, "<gray>材质：<white>" + stack.getType() + "<gray>，堆叠上限：<white>" + item.maxStackSize());
        Messages.raw(sender, "<gray>NBT 标签：<white>" + tag);
        Messages.raw(sender, "<gray>识别自检：<white>"
                + (plugin.items().identify(stack) != null ? "通过（能被认出来）" : "失败（认不出来）"));
        Messages.raw(sender, "<gray>不可放置：<white>" + !item.placeable()
                + "<gray>，不可合成：<white>" + !item.craftable());
        return true;
    }

    /** /cogito give <物品id> <玩家> <数量> —— 给玩家注册物品（仅 OP）。 */
    private boolean giveItem(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 4) {
            Messages.send(sender, "<red>用法：<white>/" + label + " give <物品id> <玩家> <数量>");
            Messages.send(sender, "<gray>可用物品：<white>" + itemIds());
            return true;
        }

        String rawId = args[1].toLowerCase(Locale.ROOT);
        // 设计稿里的 give ego / aberrations / tool 属于后续版本
        if (rawId.equals("ego") || rawId.equals("aberrations") || rawId.equals("tool")) {
            Messages.send(sender, "<yellow>" + rawId + " 的发放还没实现（E.G.O 与异想体在后续版本）");
            return true;
        }

        CustomItem item = plugin.items().find(rawId);
        if (item == null) {
            Messages.send(sender, "<red>没有这个物品：<white>" + args[1] + "</white>，可用：" + itemIds());
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(sender, "<red>找不到在线玩家 <white>" + args[2]);
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException error) {
            Messages.send(sender, "<red>数量必须是整数");
            return true;
        }
        if (amount <= 0) {
            Messages.send(sender, "<red>数量必须大于 0");
            return true;
        }

        give(target, item, amount);
        Messages.send(sender, "<green>已给 <white>" + target.getName() + "</white> "
                + amount + " 个 " + item.displayName());
        Messages.send(target, "<green>你收到了 " + amount + " 个 " + item.displayName());
        return true;
    }

    /** 按物品自己的堆叠上限分批发放，装不下的掉在脚下。 */
    private void give(Player target, CustomItem item, int amount) {
        int perStack = item.maxStackSize();
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            int size = Math.min(perStack, remaining);
            stacks.add(item.create(size));
            remaining -= size;
        }
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) {
            Location location = target.getLocation();
            for (ItemStack stack : leftover.values()) {
                target.getWorld().dropItemNaturally(location, stack);
            }
        }
    }

    /** /cogito items —— 列出所有已注册物品。 */
    private boolean listItems(CommandSender sender) {
        Messages.raw(sender, "<gray>—— Cogito 物品（共 " + plugin.items().all().size() + " 个）——");
        for (CustomItem item : plugin.items().all()) {
            Messages.raw(sender, "<white>" + item.id() + " <dark_gray>| <gray>" + item.material()
                    + " <dark_gray>| " + item.displayName());
        }
        return true;
    }

    private String itemIds() {
        List<String> ids = new ArrayList<>();
        for (CustomItem item : plugin.items().all()) {
            ids.add(item.id());
        }
        return String.join(", ", ids);
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
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (!sender.hasPermission(PERMISSION_ADMIN)
                        && (sub.equals("reload") || sub.equals("give") || sub.equals("items"))) {
                    continue;
                }
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return List.of();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        boolean giveLike = sub.equals("give") || sub.equals("debug");
        if (!giveLike) {
            return result;
        }
        if (args.length == 2) {
            for (CustomItem item : plugin.items().all()) {
                if (item.id().startsWith(prefix)) {
                    result.add(item.id());
                }
            }
        } else if (args.length == 3 && sub.equals("give")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(online.getName());
                }
            }
        } else if (args.length == 4 && sub.equals("give")) {
            result.addAll(List.of("1", "8", "64"));
        }
        return result;
    }
}
