// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.EgoEquipped;
import com.seewo.cogito.ego.EgoMath;
import com.seewo.cogito.ego.EgoSetBonusService.HolyShieldResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityDamageEvent;

/** 把 E.G.O. 防具的统一抗性应用到所有传入伤害。 */
public final class EgoDamageListener implements Listener {

    private final CogitoPlugin plugin;

    public EgoDamageListener(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EgoEquipped equipped = plugin.ego().resolve(player);
        if (!equipped.active()) {
            return;
        }

        double before = event.getDamage();
        if (equipped.pieces() >= 4
                && plugin.ego().set(equipped.setId()).bonus().blocksDamage(before, equipped.pieces())) {
            event.setCancelled(true);
            debug(player, equipped, before, 0.0D, equipped.resistance(), 0.0D,
                    "immunity:raw<5", 0.0D);
            return;
        }

        double protectedDamage = vanillaProtectedDamage(event);
        double resistance = plugin.mimicService() == null
                ? equipped.resistance()
                : plugin.mimicService().effectiveResistance(player, equipped.resistance());
        double after = EgoMath.apply(protectedDamage, resistance, equipped.pieces());

        double healed = 0.0D;

        if (equipped.resistance() < 0.0D && after < 0.0D) {
            // Negative resistance = invert damage into healing. Cancel the vanilla damage first.
            event.setCancelled(true);
            healed = -after;
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healed));
        } else if (after > 0.0D) {
            HolyShieldResult shieldResult = plugin.egoBonuses().absorbWithHolyShield(player, after);
            if (shieldResult != HolyShieldResult.NONE) {
                event.setCancelled(true);
                debug(player, equipped, before, protectedDamage, resistance, after,
                        "holy-shield:" + shieldResult, healed);
                return;
            }
            restoreBaseDamage(event, protectedDamage, after);
        } else {
            event.setDamage(0.0D);
        }

        debug(player, equipped, before, protectedDamage, resistance, after, null, healed);
    }

    /** 原版保护结算后的实际伤害；神圣黄盾只负责显示，不计入这里的减伤。 */
    private double vanillaProtectedDamage(EntityDamageEvent event) {
        double damage = event.getFinalDamage();
        if (event.isApplicable(DamageModifier.ABSORPTION)) {
            damage -= event.getDamage(DamageModifier.ABSORPTION);
        }
        return Math.max(0.0D, damage);
    }

    /** 保持原版剩余修饰符不变，只反向缩放 BASE，使最终伤害等于 E.G.O. 计算值。 */
    private void restoreBaseDamage(EntityDamageEvent event, double protectedDamage, double desiredDamage) {
        if (protectedDamage <= 1.0E-9D || desiredDamage <= 0.0D) {
            event.setDamage(Math.max(0.0D, desiredDamage));
            return;
        }
        event.setDamage(event.getDamage() * (desiredDamage / protectedDamage));
    }

    private void debug(
            Player player,
            EgoEquipped equipped,
            double before,
            double protectedDamage,
            double resistance,
            double after,
            String result,
            double healed) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("E.G.O. 抗性结算：" + player.getName()
                    + " set=" + equipped.setId()
                    + " y=" + equipped.pieces()
                    + " x=" + resistance
                    + " before=" + before
                    + " protected=" + protectedDamage
                    + " after=" + after
                    + (result == null ? "" : " result=" + result)
                    + (healed > 0.0D ? " healed=" + healed : ""));
        }
    }
}
