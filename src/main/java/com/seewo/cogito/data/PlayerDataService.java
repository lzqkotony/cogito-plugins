// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.data;

import com.seewo.cogito.CogitoPlugin;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 玩家数据的门面：内存缓存 + 异步落库。
 *
 * <p>读写规则：
 * <ul>
 *   <li>上线时同步读一次（本地 SQLite，开销很小）放进缓存，之后的读取都走内存</li>
 *   <li>数据改动只标记 dirty，由 {@link #flushDirty()} 每 30 秒异步写一次；玩家下线与插件关闭时立即写</li>
 * </ul>
 */
public final class PlayerDataService {

    private final CogitoPlugin plugin;
    private final PlayerDataStore store;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    private BukkitTask flushTask;

    public PlayerDataService(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.store = new SqlitePlayerDataStore(plugin);
    }

    /** 初始化数据库并启动定时落库；失败时返回 false（插件会禁用自己）。 */
    public boolean init() {
        try {
            store.init();
        } catch (Exception error) {
            plugin.getLogger().severe("玩家数据初始化失败：" + error.getMessage());
            return false;
        }
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, 20L * 30, 20L * 30);
        return true;
    }

    /** 玩家上线：读档（或建新档）并放进缓存。 */
    public PlayerProfile join(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = store.load(uuid).orElseGet(() -> PlayerProfile.create(uuid, player.getName()));
        profile.name(player.getName());
        profile.touch();
        cache.put(uuid, profile);
        return profile;
    }

    /** 玩家下线：写档并移出缓存。 */
    public void quit(Player player) {
        PlayerProfile profile = cache.remove(player.getUniqueId());
        if (profile != null) {
            saveAsync(profile);
        }
    }

    /** 在线玩家的数据（可能为 null，比如还没走 join）。 */
    public PlayerProfile cached(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * 按名字找玩家数据：在线优先，其次查数据库（离线玩家）。
     * 用于 {@code /cogito set Lv <玩家名>} 这类管理员命令。
     */
    public Optional<PlayerProfile> lookup(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            PlayerProfile profile = cache.get(online.getUniqueId());
            if (profile == null) {
                profile = join(online);
            }
            return Optional.of(profile);
        }
        return store.findByName(name);
    }

    /** 立即异步写一次（改动后调用）。 */
    public void saveAsync(PlayerProfile profile) {
        if (profile == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> store.save(profile));
    }

    /** 把某玩家的数据恢复默认（在线则同时更新缓存）。 */
    public void reset(PlayerProfile profile) {
        profile.reset();
        saveAsync(profile);
    }

    /** 彻底删除某玩家的数据（缓存里也会移除）。 */
    public void delete(UUID uuid) {
        cache.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> store.delete(uuid));
    }

    /** 定时把 dirty 的数据写回数据库。 */
    public void flushDirty() {
        for (PlayerProfile profile : cache.values()) {
            if (profile.dirty()) {
                store.save(profile);
            }
        }
    }

    /** 插件关闭：把缓存全部写回并关掉连接。 */
    public void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        for (PlayerProfile profile : cache.values()) {
            if (profile.dirty()) {
                store.save(profile);
            }
        }
        cache.clear();
        store.close();
    }

    public int storedCount() {
        return store.count();
    }

    public PlayerDataStore store() {
        return store;
    }
}
