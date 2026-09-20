// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

/** E.G.O. 抗性的纯计算逻辑，方便单元测试与后续复用。 */
public final class EgoMath {

    private EgoMath() {
    }

    /**
     * 计算最终伤害倍率。
     *
     * <p>统一使用 {@code 1 - (1 - x) * y / 4}，不再把负抗性转换成“易伤”正倍率。
     * 负倍率由伤害监听器解释为等量治疗。例如 {@code x=-10, y=4} 得到 {@code r=-10}。
     */
    public static double resistanceFactor(double resistance, int pieces) {
        if (!Double.isFinite(resistance)) {
            return 1.0D;
        }
        int validPieces = Math.max(0, Math.min(4, pieces));
        if (validPieces == 0) {
            return 1.0D;
        }
        return 1.0D - (1.0D - resistance) * validPieces / 4.0D;
    }

    /**
     * Project Moon 的 PALE（插件界面称蓝伤）按目标最大生命值的百分比结算。
     * 配置中的 5~6 蓝伤表示每段扣除目标最大生命值的 5%~6%。
     */
    public static double blueDamage(double maxHealth, double percentage) {
        if (!Double.isFinite(maxHealth) || !Double.isFinite(percentage)
                || maxHealth <= 0.0D || percentage <= 0.0D) {
            return 0.0D;
        }
        return maxHealth * percentage / 100.0D;
    }

    /** 保留 double 精度，不做取整；是否接受负数由 Minecraft 最终处理。 */
    public static double apply(double damage, double resistance, int pieces) {
        return damage * resistanceFactor(resistance, pieces);
    }
}
