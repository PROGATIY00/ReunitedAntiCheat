package com.reunitedanticheat.joinintelligence.api;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.model.DetectionEvent;
import com.reunitedanticheat.joinintelligence.model.DetectionSeverity;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DetectionAPI {

    private final JoinIntelligencePlugin plugin;

    public DetectionAPI(JoinIntelligencePlugin plugin) {
        this.plugin = plugin;
    }

    public void reportDetection(Player player, String category, String details) {
        DetectionSeverity severity = plugin.getConfigManager().getSeverity(category);
        if (severity == null) severity = plugin.getConfigManager().getDefaultSeverity();

        DetectionEvent event = new DetectionEvent(
            player.getName(), player.getUniqueId(), category, details, severity
        );

        plugin.getAlertManager().processDetection(event, player);
        plugin.getDatabaseManager().logDetection(event);
    }

    public void reportDetection(String playerName, java.util.UUID playerUuid,
                                String category, String details) {
        DetectionSeverity severity = plugin.getConfigManager().getSeverity(category);
        if (severity == null) severity = plugin.getConfigManager().getDefaultSeverity();

        DetectionEvent event = new DetectionEvent(
            playerName, playerUuid, category, details, severity
        );

        plugin.getAlertManager().processDetection(event, null);
        plugin.getDatabaseManager().logDetection(event);
    }

    public DetectionSeverity getSeverity(String category) {
        return plugin.getConfigManager().getSeverity(category);
    }

    public Map<String, DetectionSeverity> getAllSeverities() {
        return plugin.getConfigManager().getAllSeverities();
    }

    public CompletableFuture<List<DetectionEvent>> getRecentDetections(String playerName, int limit) {
        return plugin.getDatabaseManager().getRecentDetections(playerName, limit);
    }

    public boolean isSuspicious(PlayerJoinInfo info) {
        return info != null && (info.hasSuspiciousMods() || info.isCheatClient());
    }
}
