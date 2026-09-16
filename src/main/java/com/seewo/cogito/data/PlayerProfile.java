// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一个玩家的数据（core data）。
 *
 * <p>目前存三样东西：
 * <ul>
 *   <li>等级 {@code level}（对应设计稿里的 {@code /cogito set Lv}）</li>
 *   <li>各异想体的镇压次数（v0.3 异想体产出脑啡肽要用）</li>
 *   <li>已解锁的 E.G.O（v0.4 开发系统要用）</li>
 * </ul>
 *
 * <p>任何修改都会把 {@link #dirty()} 置位，由 {@link PlayerDataService} 定期异步落库。
 */
public final class PlayerProfile {

    private final UUID uuid;
    private String name;
    private int level;
    private final long firstSeen;
    private long lastSeen;

    private final Map<String, Integer> suppressions = new LinkedHashMap<>();
    private final Set<String> unlockedEgo = new LinkedHashSet<>();

    private boolean dirty;

    public PlayerProfile(UUID uuid, String name, int level, long firstSeen, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.level = Math.max(1, level);
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    /** 新玩家：等级从 1 开始。 */
    public static PlayerProfile create(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        PlayerProfile profile = new PlayerProfile(uuid, name, 1, now, now);
        profile.dirty = true;
        return profile;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String value) {
        if (value != null && !value.equals(name)) {
            this.name = value;
            this.dirty = true;
        }
    }

    public int level() {
        return level;
    }

    public void level(int value) {
        int clamped = Math.max(1, value);
        if (clamped != level) {
            this.level = clamped;
            this.dirty = true;
        }
    }

    public long firstSeen() {
        return firstSeen;
    }

    public long lastSeen() {
        return lastSeen;
    }

    /** 记录一次上线，用于统计与"最近登录"展示。 */
    public void touch() {
        this.lastSeen = System.currentTimeMillis();
        this.dirty = true;
    }

    // ------------------------------------------------------- 异想体镇压次数

    public int suppressions(String abnormalityId) {
        return suppressions.getOrDefault(abnormalityId, 0);
    }

    public int addSuppression(String abnormalityId, int delta) {
        int now = suppressions(abnormalityId) + delta;
        if (now <= 0) {
            suppressions.remove(abnormalityId);
        } else {
            suppressions.put(abnormalityId, now);
        }
        dirty = true;
        return Math.max(0, now);
    }

    public Map<String, Integer> allSuppressions() {
        return Collections.unmodifiableMap(suppressions);
    }

    public void suppressions(Map<String, Integer> values) {
        suppressions.clear();
        suppressions.putAll(values);
        dirty = true;
    }

    // ------------------------------------------------------- E.G.O 解锁

    public Set<String> unlockedEgo() {
        return Collections.unmodifiableSet(unlockedEgo);
    }

    public boolean isEgoUnlocked(String egoId) {
        return unlockedEgo.contains(egoId);
    }

    public boolean unlockEgo(String egoId) {
        boolean added = unlockedEgo.add(egoId);
        if (added) {
            dirty = true;
        }
        return added;
    }

    public void unlockedEgo(Set<String> values) {
        unlockedEgo.clear();
        unlockedEgo.addAll(values);
        dirty = true;
    }

    // ------------------------------------------------------- 其它

    /** 恢复出厂设置（保留 UUID 与首次上线时间）。 */
    public void reset() {
        this.level = 1;
        this.suppressions.clear();
        this.unlockedEgo.clear();
        this.dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}
