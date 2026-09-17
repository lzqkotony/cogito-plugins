// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.command;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerDataStore;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.DamageChannel;
import com.seewo.cogito.ego.EgoEquipped;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
            List.of("gui", "give", "items", "set", "reset", "data", "debug", "help", "reload");

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
            case "set" -> {
                return setValue(sender, label, args);
            }
            case "reset" -> {
                return resetData(sender, label, args);
            }
            case "data" -> {
                return showData(sender, label, args);
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
            Messages.raw(sender, "<yellow>/" + label + " give ego <玩家> <套装id> <数量> <gray>- 发放整套 E.G.O.");
            Messages.raw(sender, "<yellow>/" + label + " give blueprint <玩家> <套装id> [数量] <gray>- 发放专属图纸");
            Messages.raw(sender, "<yellow>/" + label + " set Lv <玩家> <数字> <gray>- 设置玩家等级");
            Messages.raw(sender, "<yellow>/" + label + " reset player_information <玩家> <gray>- 重置玩家数据");
            Messages.raw(sender, "<yellow>/" + label + " data <玩家> <gray>- 查看玩家数据");
            Messages.raw(sender, "<yellow>/" + label + " debug <物品id> <gray>- 造一个物品并打印它的数据");
            Messages.raw(sender, "<yellow>/" + label + " debug ego <玩家> <gray>- 查看 E.G.O. 抗性结算");
            Messages.raw(sender, "<yellow>/" + label + " debug attack <red|blue> <攻击者> <目标> <伤害> <gray>- 伤害通道自检");
            Messages.raw(sender, "<yellow>/" + label + " debug db <gray>- 数据库读写自检");
            Messages.raw(sender, "<yellow>/" + label + " reload <gray>- 重载 config.yml");
            Messages.raw(sender, "<yellow>/enkephalin give|take <玩家> <数量> <gray>- 发放 / 扣除");
        }
    }

    // ------------------------------------------------------------ 玩家数据

    /** /cogito set Lv <玩家> <数字> —— 目前只支持等级这一项。 */
    private boolean setValue(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 4) {
            Messages.send(sender, "<red>用法：<white>/" + label + " set Lv <玩家> <数字>");
            return true;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        if (!key.equals("lv") && !key.equals("level")) {
            Messages.send(sender, "<yellow>目前只支持 <white>Lv</white>（等级），其它键等对应功能做出来再加");
            return true;
        }
        Optional<PlayerProfile> found = plugin.data().lookup(args[2]);
        if (found.isEmpty()) {
            Messages.send(sender, "<red>没有 <white>" + args[2] + "</white> 的数据（该玩家至少进服过一次才能改）");
            return true;
        }
        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException error) {
            Messages.send(sender, "<red>等级必须是整数");
            return true;
        }
        if (value < 1) {
            Messages.send(sender, "<red>等级不能小于 1");
            return true;
        }

        PlayerProfile profile = found.get();
        int before = profile.level();
        profile.level(value);
        plugin.data().saveAsync(profile);
        Messages.send(sender, "<green>已把 <white>" + profile.name() + "</white> 的等级从 "
                + before + " 设为 <white>" + profile.level());

        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null && online != sender) {
            Messages.send(online, "<gray>你的等级被设置为 <white>" + profile.level());
        }
        return true;
    }

    /** /cogito reset player_information <玩家> —— 清空该玩家数据（等级、镇压次数、E.G.O 解锁）。 */
    private boolean resetData(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 3) {
            Messages.send(sender, "<red>用法：<white>/" + label + " reset player_information <玩家>");
            return true;
        }
        String what = args[1].toLowerCase(Locale.ROOT);
        if (what.equals("setting")) {
            Messages.send(sender, "<yellow>reset setting 还没实现（想恢复默认配置请手动删掉 plugins/Cogito 下的 config.yml）");
            return true;
        }
        if (!what.equals("player_information") && !what.equals("player")) {
            Messages.send(sender, "<red>只支持 <white>reset player_information <玩家>");
            return true;
        }

        Optional<PlayerProfile> found = plugin.data().lookup(args[2]);
        if (found.isEmpty()) {
            Messages.send(sender, "<red>没有 <white>" + args[2] + "</white> 的数据");
            return true;
        }
        PlayerProfile profile = found.get();
        plugin.data().reset(profile);
        Messages.send(sender, "<green>已重置 <white>" + profile.name() + "</white> 的数据（等级 "
                + profile.level() + "，镇压记录与 E.G.O 解锁已清空）");
        return true;
    }

    /** /cogito data <玩家> —— 查看数据，排查用。 */
    private boolean showData(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 2) {
            Messages.send(sender, "<red>用法：<white>/" + label + " data <玩家>");
            return true;
        }
        Optional<PlayerProfile> found = plugin.data().lookup(args[1]);
        if (found.isEmpty()) {
            Messages.send(sender, "<red>没有 <white>" + args[1] + "</white> 的数据");
            return true;
        }
        PlayerProfile profile = found.get();
        Messages.raw(sender, "<gray>—— 玩家数据：" + profile.name() + " ——");
        Messages.raw(sender, "<gray>UUID：<white>" + profile.uuid());
        Messages.raw(sender, "<gray>等级：<white>" + profile.level());
        Messages.raw(sender, "<gray>镇压记录：<white>"
                + (profile.allSuppressions().isEmpty() ? "无" : profile.allSuppressions().toString()));
        Messages.raw(sender, "<gray>已解锁 E.G.O：<white>"
                + (profile.unlockedEgo().isEmpty() ? "无" : String.join(", ", profile.unlockedEgo())));
        Messages.raw(sender, "<gray>首次上线：<white>" + java.time.Instant.ofEpochMilli(profile.firstSeen())
                + " <gray>最近上线：<white>" + java.time.Instant.ofEpochMilli(profile.lastSeen()));
        Messages.raw(sender, "<gray>数据库里共有 <white>" + plugin.data().storedCount() + " <gray>名玩家的数据");
        return true;
    }

    /** /cogito debug db —— 数据库自检：写一条临时数据、读回来、再删掉。 */
    private boolean debugDatabase(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        UUID probe = UUID.randomUUID();
        PlayerDataStore store = plugin.data().store();
        PlayerProfile profile = PlayerProfile.create(probe, "self-test");
        profile.level(7);
        profile.addSuppression("self-test-abnormality", 3);
        profile.unlockEgo("self-test-ego");
        try {
            store.save(profile);
            Optional<PlayerProfile> loaded = store.load(probe);
            boolean pass = loaded.isPresent()
                    && loaded.get().level() == 7
                    && loaded.get().suppressions("self-test-abnormality") == 3
                    && loaded.get().isEgoUnlocked("self-test-ego");
            store.delete(probe);
            Messages.raw(sender, "<gray>—— 数据库自检 ——");
            Messages.raw(sender, "<gray>写入/读取/删除：<white>" + (pass ? "通过" : "失败"));
            loaded.ifPresent(value -> Messages.raw(sender, "<gray>读回的等级：<white>" + value.level()
                    + "<gray>，镇压记录：<white>" + value.allSuppressions()
                    + "<gray>，E.G.O：<white>" + value.unlockedEgo()));
            Messages.raw(sender, "<gray>库中玩家数：<white>" + store.count());
        } catch (RuntimeException error) {
            Messages.raw(sender, "<red>自检失败：" + error.getMessage());
        }
        return true;
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
            Messages.send(sender, "<red>用法：<white>/cogito debug <物品id|db>");
            return true;
        }
        if (args[1].equalsIgnoreCase("db") || args[1].equalsIgnoreCase("data")) {
            return debugDatabase(sender);
        }
        if (args[1].equalsIgnoreCase("ego")) {
            return debugEgo(sender, args);
        }
        if (args[1].equalsIgnoreCase("attack")) {
            return debugAttack(sender, args);
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

    /** /cogito debug ego <玩家> —— 查看玩家当前套装、件数和最终倍率。 */
    private boolean debugEgo(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 3) {
            Messages.send(sender, "<red>用法：<white>/cogito debug ego <玩家>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(sender, "<red>找不到在线玩家 <white>" + args[2]);
            return true;
        }

        EgoEquipped equipped = plugin.ego().resolve(target);
        Messages.raw(sender, "<gray>—— E.G.O. 抗性自检：<white>" + target.getName() + " <gray>——");
        Messages.raw(sender, "<gray>套装：<white>"
                + (equipped.setId() == null ? "未穿戴" : equipped.setId())
                + "<gray>，防具件数 y=<white>" + equipped.pieces());
        Messages.raw(sender, "<gray>状态：<white>"
                + (equipped.mixed() ? "混搭，抗性失效" : equipped.active() ? "生效" : "未生效"));
        Messages.raw(sender, "<gray>标称抗性 x=<white>" + equipped.resistance()
                + "<gray>，最终倍率 r=<white>" + equipped.factor());
        return true;
    }

    /** /cogito debug attack <red|blue> <攻击者> <目标> <伤害> —— 验证蓝伤来源限制。 */
    private boolean debugAttack(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 6) {
            Messages.send(sender, "<red>用法：<white>/cogito debug attack <red|blue> <攻击者> <目标> <伤害>");
            return true;
        }
        DamageChannel channel = DamageChannel.parse(args[2]);
        if (channel == null) {
            Messages.send(sender, "<red>伤害通道只能是 <white>red</white> 或 <white>blue");
            return true;
        }
        Player attacker = Bukkit.getPlayerExact(args[3]);
        Player target = Bukkit.getPlayerExact(args[4]);
        if (attacker == null || target == null) {
            Messages.send(sender, "<red>攻击者与目标都必须在线上");
            return true;
        }
        double damage;
        try {
            damage = Double.parseDouble(args[5]);
        } catch (NumberFormatException error) {
            Messages.send(sender, "<red>伤害必须是数字");
            return true;
        }
        if (!Double.isFinite(damage) || damage <= 0.0D) {
            Messages.send(sender, "<red>伤害必须大于 0");
            return true;
        }

        boolean accepted = plugin.egoDamage().dealDamage(attacker, target, damage, channel);
        if (!accepted) {
            Messages.send(sender, "<red>攻击被拒绝：蓝伤只能由手持 BLUE E.G.O. 武器的玩家发起");
            return true;
        }
        Messages.send(sender, "<green>已通过 <white>" + channel.displayName()
                + "</white> 对 <white>" + target.getName() + "</white> 结算伤害 <white>" + damage);
        return true;
    }

    /** /cogito give <物品id> <玩家> <数量> —— 给玩家注册物品（仅 OP）。 */
    /**
     * /cogito give blueprint <玩家> <套装id> [数量] —— 发放某套 E.G.O. 的专属图纸。
     *
     * <p>图纸不是消耗品：右键解锁研发能力后仍然留在玩家手上。
     */
    private boolean giveBlueprint(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, "<red>你没有权限执行这个操作");
            return true;
        }
        if (args.length < 4) {
            Messages.send(sender, "<red>用法：<white>/" + label + " give blueprint <玩家> <套装id> [数量]");
            Messages.send(sender, "<gray>可用套装：<white>" + String.join(", ",
                    plugin.ego().sets().stream().map(EgoSetDefinition::id).toList()));
            return true;
        }
        EgoSetDefinition set = plugin.ego().set(args[3]);
        if (set == null) {
            Messages.send(sender, "<red>没有这套 E.G.O.：<white>" + args[3] + "</white>，可用："
                    + String.join(", ", plugin.ego().sets().stream().map(EgoSetDefinition::id).toList()));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(sender, "<red>找不到在线玩家 <white>" + args[2]);
            return true;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException error) {
                Messages.send(sender, "<red>数量必须是整数");
                return true;
            }
        }
        if (amount <= 0) {
            Messages.send(sender, "<red>数量必须大于 0");
            return true;
        }
        CustomItem blueprint = plugin.items().find(set.blueprintId());
        if (blueprint == null) {
            Messages.send(sender, "<red>这张图纸没有注册成功，请检查 ego.yml");
            return true;
        }
        give(target, blueprint, amount);
        Messages.send(sender, "<green>已给 <white>" + target.getName() + "</white> " + amount
                + " 张「" + set.displayName() + "<green>」专属图纸");
        Messages.send(target, "<green>你收到了 <white>" + set.displayName()
                + "<green> 的专属图纸，右键即可解锁研发能力（图纸不会消耗）");
        return true;
    }

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
        if (rawId.equals("ego")) {
            return giveEgo(sender, label, args);
        }
        if (rawId.equals("blueprint")) {
            return giveBlueprint(sender, label, args);
        }
        if (rawId.equals("aberrations") || rawId.equals("tool")) {
            Messages.send(sender, "<yellow>" + rawId + " 的发放还没实现（等异想体系统）");
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

    /** /cogito give ego <玩家> <套装id> <数量> —— 发放整套 E.G.O. 物品。 */
    private boolean giveEgo(CommandSender sender, String label, String[] args) {
        if (args.length < 5) {
            Messages.send(sender, "<red>用法：<white>/" + label + " give ego <玩家> <套装id> <数量>");
            Messages.send(sender, "<gray>可用套装：<white>" + String.join(", ", plugin.ego().sets().stream()
                    .map(value -> value.id()).toList()));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(sender, "<red>找不到在线玩家 <white>" + args[2]);
            return true;
        }
        String setId = args[3].toLowerCase(Locale.ROOT);
        if (plugin.ego().set(setId) == null) {
            Messages.send(sender, "<red>没有这套 E.G.O.：<white>" + setId);
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException error) {
            Messages.send(sender, "<red>数量必须是整数");
            return true;
        }
        if (amount <= 0) {
            Messages.send(sender, "<red>数量必须大于 0");
            return true;
        }

        int count = 0;
        for (CustomItem item : plugin.ego().itemsForSet(setId)) {
            give(target, item, amount);
            count++;
        }
        Messages.send(sender, "<green>已给 <white>" + target.getName() + "</white> 发放整套 E.G.O. <white>"
                + setId + "</white> × " + amount + "（" + count + " 件）");
        Messages.send(target, "<green>你收到了整套 E.G.O. <white>" + setId + "</white> × " + amount);
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

        if (sub.equals("set")) {
            if (args.length == 2) {
                return List.of("Lv");
            }
            if (args.length == 3) {
                return onlinePlayers(prefix);
            }
            if (args.length == 4) {
                return List.of("1", "10", "50");
            }
            return result;
        }
        if (sub.equals("reset")) {
            return args.length == 2 ? List.of("player_information") : result;
        }
        if (sub.equals("data")) {
            return args.length == 2 ? onlinePlayers(prefix) : result;
        }

        boolean giveLike = sub.equals("give") || sub.equals("debug");
        if (!giveLike) {
            return result;
        }
        if (args.length == 2) {
            if (sub.equals("debug")) {
                result.addAll(List.of("db", "ego", "attack"));
            }
            for (CustomItem item : plugin.items().all()) {
                if (item.id().startsWith(prefix)) {
                    result.add(item.id());
                }
            }
        } else if (args.length == 3 && sub.equals("give") && !args[1].equalsIgnoreCase("ego")) {
            return onlinePlayers(prefix);
        } else if (sub.equals("give") && args.length == 3 && args[1].equalsIgnoreCase("ego")) {
            return onlinePlayers(prefix);
        } else if (sub.equals("give") && args.length == 4
                && (args[1].equalsIgnoreCase("ego") || args[1].equalsIgnoreCase("blueprint"))) {
            return plugin.ego().sets().stream().map(value -> value.id()).filter(id -> id.startsWith(prefix)).toList();
        } else if (sub.equals("give") && args.length == 5
                && (args[1].equalsIgnoreCase("ego") || args[1].equalsIgnoreCase("blueprint"))) {
            result.addAll(List.of("1", "2", "4"));
        } else if (args.length == 4 && sub.equals("give")) {
            result.addAll(List.of("1", "8", "64"));
        } else if (sub.equals("debug") && args.length == 3 && args[1].equalsIgnoreCase("ego")) {
            return onlinePlayers(prefix);
        } else if (sub.equals("debug") && args.length == 3 && args[1].equalsIgnoreCase("attack")) {
            result.addAll(List.of("red", "blue"));
        } else if (sub.equals("debug") && args.length >= 4 && args[1].equalsIgnoreCase("attack")) {
            return onlinePlayers(prefix);
        }
        return result;
    }

    private List<String> onlinePlayers(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
