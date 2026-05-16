package com.reunitedanticheat.joinintelligence.model;

public class FlaggedPlayer {

    private final String playerName;
    private final String playerUuid;
    private final String lastCategory;
    private final int detectionCount;
    private final long lastDetection;

    public FlaggedPlayer(String playerName, String playerUuid,
                         String lastCategory, int detectionCount, long lastDetection) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.lastCategory = lastCategory;
        this.detectionCount = detectionCount;
        this.lastDetection = lastDetection;
    }

    public String getPlayerName() { return playerName; }
    public String getPlayerUuid() { return playerUuid; }
    public String getLastCategory() { return lastCategory; }
    public int getDetectionCount() { return detectionCount; }
    public long getLastDetection() { return lastDetection; }
}
