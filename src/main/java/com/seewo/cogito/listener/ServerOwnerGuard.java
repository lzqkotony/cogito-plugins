// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** 彩蛋套装「服主」的保护判定：控制台保留权限，自己不受限制。 */
final class ServerOwnerGuard {

    static final String SET_ID = "server-owner";

    private ServerOwnerGuard() {
    }

    static boolean isProtected(CogitoPlugin plugin, CommandSender source, Player target) {
        if (target == null || !plugin.ego().wearsAnyArmorPiece(target, SET_ID)) {
            return false;
        }
        if (source == target) {
            return false;
        }
        if (source instanceof BlockCommandSender) {
            return true;
        }
        return source instanceof Player player && player.isOp();
    }

    /** Citizens 会给 NPC 实体写入 NPC metadata；服主 E.G.O. 不应对其造成击杀或踢出。 */
    static boolean isCitizensNpc(Entity entity) {
        return entity != null && entity.hasMetadata("NPC");
    }
}
