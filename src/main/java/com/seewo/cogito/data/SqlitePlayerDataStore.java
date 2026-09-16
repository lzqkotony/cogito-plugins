// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 lzqkotony

package com.seewo.cogito.data;

import com.seewo.cogito.CogitoPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite 存储。
 *
 * <p>驱动由服务端自带（Paper 1.21.x 里是 {@code org.xerial:sqlite-jdbc}），所以插件不需要额外依赖，
 * 也不会把驱动打进 jar。数据库文件在 {@code plugins/Cogito/cogito.db}，开了 WAL 模式。
 */
public final class SqlitePlayerDataStore implements PlayerDataStore {

    private static final String CREATE_PLAYERS = """
            CREATE TABLE IF NOT EXISTS players (
                uuid       TEXT PRIMARY KEY,
                name       TEXT NOT NULL,
                level      INTEGER NOT NULL DEFAULT 1,
                first_seen INTEGER NOT NULL,
                last_seen  INTEGER NOT NULL
            )""";

    private static final String CREATE_SUPPRESSIONS = """
            CREATE TABLE IF NOT EXISTS abnormality_suppression (
                uuid        TEXT NOT NULL,
                abnormality TEXT NOT NULL,
                count       INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, abnormality)
            )""";

    private static final String CREATE_EGO = """
            CREATE TABLE IF NOT EXISTS ego_unlocked (
                uuid TEXT NOT NULL,
                ego  TEXT NOT NULL,
                PRIMARY KEY (uuid, ego)
            )""";

    private final CogitoPlugin plugin;
    private final File file;
    private Connection connection;

    public SqlitePlayerDataStore(CogitoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cogito.db");
    }

    @Override
    public synchronized void init() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建数据目录：" + parent);
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA journal_mode=WAL");
            statement.executeUpdate(CREATE_PLAYERS);
            statement.executeUpdate(CREATE_SUPPRESSIONS);
            statement.executeUpdate(CREATE_EGO);
        }
        plugin.getLogger().info("玩家数据库就绪：" + file.getName());
    }

    @Override
    public synchronized Optional<PlayerProfile> load(UUID uuid) {
        requireConnection();
        String key = uuid.toString();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, level, first_seen, last_seen FROM players WHERE uuid = ?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                PlayerProfile profile = new PlayerProfile(uuid,
                        result.getString("name"),
                        result.getInt("level"),
                        result.getLong("first_seen"),
                        result.getLong("last_seen"));
                Map<String, Integer> suppressions = new LinkedHashMap<>();
                try (PreparedStatement rows = connection.prepareStatement(
                        "SELECT abnormality, count FROM abnormality_suppression WHERE uuid = ?")) {
                    rows.setString(1, key);
                    try (ResultSet data = rows.executeQuery()) {
                        while (data.next()) {
                            suppressions.put(data.getString("abnormality"), data.getInt("count"));
                        }
                    }
                }
                profile.suppressions(suppressions);

                Set<String> ego = new LinkedHashSet<>();
                try (PreparedStatement rows = connection.prepareStatement(
                        "SELECT ego FROM ego_unlocked WHERE uuid = ?")) {
                    rows.setString(1, key);
                    try (ResultSet data = rows.executeQuery()) {
                        while (data.next()) {
                            ego.add(data.getString("ego"));
                        }
                    }
                }
                profile.unlockedEgo(ego);
                profile.clearDirty();
                return Optional.of(profile);
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "读取玩家数据失败：" + uuid, error);
            return Optional.empty();
        }
    }

    @Override
    public synchronized Optional<PlayerProfile> findByName(String name) {
        requireConnection();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uuid FROM players WHERE LOWER(name) = LOWER(?) ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, name.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return load(UUID.fromString(result.getString("uuid")));
            }
        } catch (SQLException | IllegalArgumentException error) {
            plugin.getLogger().log(Level.WARNING, "按名字查询玩家失败：" + name, error);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void save(PlayerProfile profile) {
        requireConnection();
        String key = profile.uuid().toString();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO players (uuid, name, level, first_seen, last_seen)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        name = excluded.name,
                        level = excluded.level,
                        last_seen = excluded.last_seen""")) {
                statement.setString(1, key);
                statement.setString(2, profile.name() == null ? "unknown" : profile.name());
                statement.setInt(3, profile.level());
                statement.setLong(4, profile.firstSeen());
                statement.setLong(5, profile.lastSeen());
                statement.executeUpdate();
            }

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM abnormality_suppression WHERE uuid = ?")) {
                delete.setString(1, key);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO abnormality_suppression (uuid, abnormality, count) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Integer> entry : profile.allSuppressions().entrySet()) {
                    insert.setString(1, key);
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, entry.getValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM ego_unlocked WHERE uuid = ?")) {
                delete.setString(1, key);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO ego_unlocked (uuid, ego) VALUES (?, ?)")) {
                for (String ego : profile.unlockedEgo()) {
                    insert.setString(1, key);
                    insert.setString(2, ego);
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            connection.commit();
            profile.clearDirty();
        } catch (SQLException error) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // 回滚失败就不管了，下面已经记了原始错误
            }
            plugin.getLogger().log(Level.WARNING, "保存玩家数据失败：" + key, error);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // 忽略
            }
        }
    }

    @Override
    public synchronized void delete(UUID uuid) {
        requireConnection();
        String key = uuid.toString();
        try {
            connection.setAutoCommit(false);
            for (String sql : new String[]{
                    "DELETE FROM players WHERE uuid = ?",
                    "DELETE FROM abnormality_suppression WHERE uuid = ?",
                    "DELETE FROM ego_unlocked WHERE uuid = ?"}) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, key);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "删除玩家数据失败：" + key, error);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // 忽略
            }
        }
    }

    @Override
    public synchronized int count() {
        requireConnection();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM players")) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException error) {
            return 0;
        }
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "关闭数据库失败", error);
        } finally {
            connection = null;
        }
    }

    public File file() {
        return file;
    }

    private void requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("数据库尚未初始化");
        }
    }
}
