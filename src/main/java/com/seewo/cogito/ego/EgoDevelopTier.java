// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

/** 定向研发的四档费用与成功率。 */
public enum EgoDevelopTier {
    NORMAL("普通研发", 0.75D, 25),
    ADVANCED("进阶研发", 1.00D, 50),
    HIGH("高级研发", 1.50D, 75),
    FULL("完全研发", 2.00D, 100);

    private final String displayName;
    private final double costMultiplier;
    private final int successPercent;

    EgoDevelopTier(String displayName, double costMultiplier, int successPercent) {
        this.displayName = displayName;
        this.costMultiplier = costMultiplier;
        this.successPercent = successPercent;
    }

    public String displayName() {
        return displayName;
    }

    public double costMultiplier() {
        return costMultiplier;
    }

    public int successPercent() {
        return successPercent;
    }

    public int costFor(int baseCost) {
        if (baseCost <= 0) {
            return 0;
        }
        return (int) Math.ceil(baseCost * costMultiplier);
    }
}
