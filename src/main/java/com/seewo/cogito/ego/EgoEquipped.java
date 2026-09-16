// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

/** 玩家当前 E.G.O. 防具结算结果。 */
public record EgoEquipped(
        boolean active,
        boolean mixed,
        String setId,
        String displayName,
        int pieces,
        double resistance,
        double factor) {

    public static EgoEquipped none() {
        return new EgoEquipped(false, false, null, null, 0, 1.0D, 1.0D);
    }

    public static EgoEquipped invalid(String setId, int pieces) {
        return new EgoEquipped(false, true, setId, null, pieces, 1.0D, 1.0D);
    }
}
