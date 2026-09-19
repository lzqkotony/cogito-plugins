// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.develop;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.data.PlayerProfile;
import com.seewo.cogito.ego.EgoDevelopCategory;
import com.seewo.cogito.ego.EgoDevelopTier;
import com.seewo.cogito.ego.EgoSetDefinition;
import com.seewo.cogito.item.CustomItem;
import com.seewo.cogito.text.Messages;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 开发台共用的解锁与四档定向研发逻辑。 */
public final class EgoDevelopmentService {

    private final CogitoPlugin plugin;

    public EgoDevelopmentService(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    public int cost(EgoSetDefinition set, EgoDevelopCategory category, EgoDevelopTier tier) {
        if (set == null || category == null || tier == null) {
            return -1;
        }
        int base = set.developCost(category);
        return base < 0 ? -1 : tier.costFor(base);
    }

    public boolean isUnlocked(Player player, EgoSetDefinition set) {
        PlayerProfile profile = profile(player);
        return profile != null && set != null && profile.isEgoUnlocked(set.id());
    }

    /** 装载一张一次性蓝图并解锁套装；consume=false 仅保留给旧调用方做兼容。 */
    public boolean unlock(Player player, EgoSetDefinition set, boolean consume) {
        if (player == null || set == null || !set.developable()) {
            Messages.send(player, "<red>这套 E.G.O. 不能通过图纸研发");
            return false;
        }
        PlayerProfile profile = profile(player);
        if (profile == null) {
            Messages.send(player, "<red>玩家数据尚未加载，请稍后再试");
            return false;
        }
        if (profile.isEgoUnlocked(set.id())) {
            Messages.send(player, "<yellow>你已经装载过 <white>" + set.displayName() + "</white> 的蓝图");
            return false;
        }

        if (consume) {
            CustomItem blueprint = plugin.items().find(set.blueprintId());
            if (blueprint == null || blueprint.removeFrom(player, 1) < 1) {
                Messages.send(player, "<red>你的背包里没有这套 E.G.O. 的蓝图");
                return false;
            }
        }

        profile.unlockEgo(set.id());
        plugin.data().saveAsync(profile);
        Messages.send(player, "<green>蓝图装载完成：<white>" + set.displayName()
                + "</white> <gray>已与该玩家绑定，蓝图已被消耗");
        return true;
    }

    /** 执行一次定向研发。失败同样消耗脑啡肽。 */
    public boolean attempt(
            Player player,
            EgoSetDefinition set,
            EgoDevelopCategory category,
            EgoDevelopTier tier) {
        if (player == null || set == null || category == null || tier == null) {
            return false;
        }
        if (!set.canDevelop(category)) {
            Messages.send(player, "<red>这套 E.G.O. 没有可研发的" + category.displayName());
            return false;
        }
        if (!isUnlocked(player, set)) {
            Messages.send(player, "<red>还没有装载这套 E.G.O. 的蓝图");
            return false;
        }

        int cost = cost(set, category, tier);
        int held = plugin.enkephalinItem().count(player);
        if (held < cost) {
            Messages.send(player, "<red>脑啡肽不足：需要 <white>" + cost + "</white>，你只有 <white>" + held);
            return false;
        }
        if (plugin.enkephalinItem().remove(player, cost) < cost) {
            Messages.send(player, "<red>扣除脑啡肽失败，请再试一次");
            return false;
        }

        boolean success = tier.successPercent() >= 100
                || ThreadLocalRandom.current().nextInt(100) < tier.successPercent();
        if (!success) {
            Messages.send(player, "<red>" + tier.displayName() + "失败：<gray>消耗 "
                    + cost + " 脑啡肽，没有获得 E.G.O.");
            return false;
        }

        Collection<CustomItem> rewards = plugin.ego().itemsForSet(set.id(), category);
        int count = 0;
        for (CustomItem item : rewards) {
            give(player, item.create(1));
            count++;
        }
        Messages.send(player, "<green>" + tier.displayName() + "成功：<white>" + set.displayName()
                + "</white> " + category.displayName() + " × " + count
                + " <gray>（消耗 " + cost + " 脑啡肽）");
        return true;
    }

    private void give(Player player, ItemStack stack) {
        if (!player.getInventory().addItem(stack).isEmpty()) {
            Location location = player.getLocation();
            player.getWorld().dropItemNaturally(location, stack);
        }
    }

    private PlayerProfile profile(Player player) {
        if (player == null) {
            return null;
        }
        PlayerProfile profile = plugin.data().cached(player.getUniqueId());
        return profile != null ? profile : plugin.data().join(player);
    }
}
