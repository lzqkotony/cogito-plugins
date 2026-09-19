// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import com.seewo.cogito.CogitoPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * E.G.O. 伤害入口。
 *
 * <p>红伤没有来源限制；蓝伤必须由手持 BLUE E.G.O. 武器的玩家发起。插件后续的
 * 异想体技能、武器技能也应通过这里结算，避免出现非 E.G.O. 蓝伤。
 */
public final class EgoDamageService {

    private final CogitoPlugin plugin;

    public EgoDamageService(CogitoPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean dealDamage(Player attacker, LivingEntity target, double amount, DamageChannel channel) {
        if (attacker == null || target == null || channel == null || !isValidAmount(amount)) {
            return false;
        }
        if (channel == DamageChannel.BLUE && !plugin.ego().canDealBlue(attacker)) {
            return false;
        }
        target.damage(resolveAmount(target, amount, channel), attacker);
        return true;
    }

    /**
     * 结算 E.G.O. 技能伤害。技能不要求当前手持 BLUE 武器，但仍从这里进入，保证
     * 目标防具的 E.G.O. 抗性照常生效。
     */
    public boolean dealSkillDamage(Player source, LivingEntity target, double amount, DamageChannel channel) {
        if (source == null || target == null || channel == null || !isValidAmount(amount)) {
            return false;
        }
        target.damage(resolveAmount(target, amount, channel), source);
        return true;
    }

    private double resolveAmount(LivingEntity target, double amount, DamageChannel channel) {
        return channel == DamageChannel.BLUE
                ? EgoMath.blueDamage(target.getMaxHealth(), amount)
                : amount;
    }

    private boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0.0D;
    }
}
