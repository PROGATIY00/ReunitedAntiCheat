package com.reunitedanticheat.joinintelligence.database;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import com.reunitedanticheat.joinintelligence.model.DetectionEvent;
import com.reunitedanticheat.joinintelligence.model.DetectionSeverity;
import com.reunitedanticheat.joinintelligence.model.FlaggedPlayer;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final JoinIntelligencePlugin plugin;
    private final ConfigManager config;
    private Connection connection;
    private boolean enabled;

    public DatabaseManager(JoinIntelligencePlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.enabled = config.isDatabaseLogging();
    }

    public void connect() {
        if (!enabled) return;

        try {
            String dbPath = config.getDatabaseFile();
            if (!dbPath.contains(File.separator)) {
                dbPath = "plugins/JoinIntelligence/" + dbPath;
            }

            File dbFile = new File(dbPath + ".mv.db");
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            String url = "jdbc:h2:" + dbPath + ";AUTO_SERVER=TRUE";
            connection = DriverManager.getConnection(url, "sa", "");
            plugin.getLogger().info("Connected to H2 database at " + dbPath);

            if (config.isDatabaseAutoCreate()) {
                createTable();
            }

            scheduleCleanup();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to connect to database: " + e.getMessage());
            this.enabled = false;
        }
    }

    private void createTable() throws SQLException {
        String tableName = config.getDatabaseTable();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id IDENTITY PRIMARY KEY, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "ip_address VARCHAR(45), " +
                "minecraft_version VARCHAR(32), " +
                "client_type VARCHAR(32), " +
                "client_brand VARCHAR(128), " +
                "mod_count INT DEFAULT 0, " +
                "mod_list CLOB, " +
                "suspicious_mods CLOB, " +
                "cheat_client BOOLEAN DEFAULT FALSE, " +
                "join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_join_logs_player_uuid ON " + tableName + "(player_uuid)"
            );
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_join_logs_join_time ON " + tableName + "(join_time)"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS detection_logs (" +
                "id IDENTITY PRIMARY KEY, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "category VARCHAR(64) NOT NULL, " +
                "details CLOB, " +
                "urgency VARCHAR(16), " +
                "detection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_detection_logs_player ON detection_logs(player_name)"
            );
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_detection_logs_time ON detection_logs(detection_time)"
            );
        }
    }

    public void logJoin(PlayerJoinInfo info) {
        if (!enabled || connection == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                String tableName = config.getDatabaseTable();
                String sql = "INSERT INTO " + tableName +
                    " (player_name, player_uuid, ip_address, minecraft_version, " +
                    "client_type, client_brand, mod_count, mod_list, suspicious_mods, cheat_client) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, info.getPlayerName());
                    stmt.setString(2, info.getPlayerUuid().toString());
                    stmt.setString(3, info.getIpAddress());
                    stmt.setString(4, info.getMinecraftVersion());
                    stmt.setString(5, info.getClientType().name());
                    stmt.setString(6, info.getClientBrand());
                    stmt.setInt(7, info.getModCount());
                    stmt.setString(8, info.getModList() != null
                        ? String.join(", ", info.getModList()) : "");
                    stmt.setString(9, info.getSuspiciousMods() != null
                        ? String.join(", ", info.getSuspiciousMods()) : "");
                    stmt.setBoolean(10, info.isCheatClient());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log join to database: " + e.getMessage());
            }
        });
    }

    public void logDetection(DetectionEvent event) {
        if (!enabled || connection == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT INTO detection_logs " +
                    "(player_name, player_uuid, category, details, urgency) " +
                    "VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, event.getPlayerName());
                    stmt.setString(2, event.getPlayerUuid().toString());
                    stmt.setString(3, event.getCategory());
                    stmt.setString(4, event.getDetails());
                    stmt.setString(5, event.getSeverity().getUrgency().name());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log detection: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<DetectionEvent>> getRecentDetections(String playerName, int limit) {
        List<DetectionEvent> results = new ArrayList<>();
        if (!enabled || connection == null) {
            return CompletableFuture.completedFuture(results);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM detection_logs WHERE player_name = ? " +
                    "ORDER BY detection_time DESC LIMIT ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    stmt.setInt(2, Math.min(limit, 100));
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            DetectionSeverity severity = config.getSeverity(rs.getString("category"));
                            results.add(new DetectionEvent(
                                rs.getString("player_name"),
                                java.util.UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("category"),
                                rs.getString("details") != null ? rs.getString("details") : "",
                                severity
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query detections: " + e.getMessage());
            }
            return results;
        });
    }

    public CompletableFuture<Integer> getDetectionCount(String playerName) {
        if (!enabled || connection == null) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM detection_logs WHERE player_name = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query detection count: " + e.getMessage());
            }
            return 0;
        });
    }

    public CompletableFuture<List<FlaggedPlayer>> getFlaggedPlayers(int minCount) {
        List<FlaggedPlayer> results = new ArrayList<>();
        if (!enabled || connection == null) {
            return CompletableFuture.completedFuture(results);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT player_name, player_uuid, category, detection_time, COUNT(*) AS cnt " +
                    "FROM detection_logs " +
                    "GROUP BY player_name, player_uuid, category, detection_time " +
                    "HAVING cnt >= ? " +
                    "ORDER BY cnt DESC, detection_time DESC";

                try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT player_name, MAX(player_uuid) AS player_uuid, " +
                    "MAX(category) AS last_category, " +
                    "MAX(detection_time) AS last_time, " +
                    "COUNT(*) AS cnt " +
                    "FROM detection_logs " +
                    "GROUP BY player_name " +
                    "HAVING cnt >= ? " +
                    "ORDER BY cnt DESC, last_time DESC")) {
                    stmt.setInt(1, minCount);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            results.add(new FlaggedPlayer(
                                rs.getString("player_name"),
                                rs.getString("player_uuid"),
                                rs.getString("last_category"),
                                rs.getInt("cnt"),
                                rs.getTimestamp("last_time").getTime()
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query flagged players: " + e.getMessage());
            }
            return results;
        });
    }

    public CompletableFuture<Integer> getJoinCount(String playerName) {
        if (!enabled || connection == null) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String tableName = config.getDatabaseTable();
                String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE player_name = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query join count: " + e.getMessage());
            }
            return 0;
        });
    }

    private void scheduleCleanup() {
        int retentionDays = config.getDatabaseRetentionDays();
        if (retentionDays <= 0) return;

        long interval = 24 * 60 * 60 * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                String tableName = config.getDatabaseTable();
                String sql = "DELETE FROM " + tableName +
                    " WHERE join_time < CURRENT_TIMESTAMP - INTERVAL '" + retentionDays + "' DAY";
                try (Statement stmt = connection.createStatement()) {
                    int deleted = stmt.executeUpdate(sql);
                    if (deleted > 0) {
                        plugin.getLogger().info("Cleaned up " + deleted + " old join log entries");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to clean up old logs: " + e.getMessage());
            }
        }, interval, interval);
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Disconnected from database");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
