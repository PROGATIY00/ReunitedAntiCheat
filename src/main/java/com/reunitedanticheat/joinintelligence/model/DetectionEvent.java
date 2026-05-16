package com.reunitedanticheat.joinintelligence.model;

import java.util.UUID;

public class DetectionEvent {

    private final String playerName;
    private final UUID playerUuid;
    private final String category;
    private final String details;
    private final DetectionSeverity severity;
    private final long timestamp;

    public DetectionEvent(String playerName, UUID playerUuid, String category,
                          String details, DetectionSeverity severity) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.category = category;
        this.details = details;
        this.severity = severity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getPlayerName() { return playerName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getCategory() { return category; }
    public String getDetails() { return details; }
    public DetectionSeverity getSeverity() { return severity; }
    public long getTimestamp() { return timestamp; }

    public String formatAlert() {
        return severity.getPrefix() + " " + severity.getColor()
            + playerName + " detected for " + category
            + (details.isEmpty() ? "" : " (" + details + ")");
    }
}
