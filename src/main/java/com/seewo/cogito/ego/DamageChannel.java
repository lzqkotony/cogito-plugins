// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.ego;

import java.util.Locale;

/** E.G.O. 攻击通道。目前只区分红伤与蓝伤；蓝伤只能由 E.G.O. 来源造成。 */
public enum DamageChannel {
    RED("红伤"),
    BLUE("蓝伤");

    private final String displayName;

    DamageChannel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static DamageChannel parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DamageChannel channel : values()) {
            if (channel.name().equals(normalized)) {
                return channel;
            }
        }
        return null;
    }
}
