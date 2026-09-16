// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.listener;

import com.seewo.cogito.CogitoPlugin;
import com.seewo.cogito.ego.EgoEquipped;
import com.seewo.cogito.ego.EgoMath;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
        double after = EgoMath.apply(before, equipped.resistance(), equipped.pieces());
        event.setDamage(after);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("E.G.O. 抗性结算：" + player.getName()
                    + " set=" + equipped.setId()
                    + " y=" + equipped.pieces()
                    + " x=" + equipped.resistance()
                    + " before=" + before
                    + " after=" + after);
        }
    }
}
