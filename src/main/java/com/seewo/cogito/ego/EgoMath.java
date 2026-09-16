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
     * <p>公式：{@code 1 - (1 - x) * y / 4}。x 不设范围，y 是当前穿着的有效防具件数。
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

    /** 保留 double 精度，不做取整；是否接受负数由 Minecraft 最终处理。 */
    public static double apply(double damage, double resistance, int pieces) {
        return damage * resistanceFactor(resistance, pieces);
    }
}
