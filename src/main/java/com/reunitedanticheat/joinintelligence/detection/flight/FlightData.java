package com.reunitedanticheat.joinintelligence.detection.flight;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FlightData {

    private final UUID playerUuid;
    private String playerName;

    private Location lastGroundLocation;
    private Location lastSafeLocation;
    private Location lastLegalMovement;

    private int airTicks;
    private int groundTicks;
    private int setbackCount;
    private int totalSetbacks;

    private double lastY;
    private double verticalVelocity;
    private double lastVerticalVelocity;
    private double accumulatedDeltaY;

    private int jumpAirTicks;
    private double jumpStartY;
    private int maxExpectedAirTicks;

    private long lastFlightAlert;
    private double lastExpectedAirTime;
    private double lastActualAirTime;
    private String lastFailReason;

    private boolean wasOnGround;
    private boolean init;

    public FlightData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Location getLastGroundLocation() { return lastGroundLocation; }
    public void setLastGroundLocation(Location loc) { this.lastGroundLocation = loc; }

    public Location getLastSafeLocation() { return lastSafeLocation; }
    public void setLastSafeLocation(Location loc) { this.lastSafeLocation = loc; }

    public Location getLastLegalMovement() { return lastLegalMovement; }
    public void setLastLegalMovement(Location loc) { this.lastLegalMovement = loc; }

    public int getAirTicks() { return airTicks; }
    public void setAirTicks(int airTicks) { this.airTicks = airTicks; }
    public void incrementAirTicks() { this.airTicks++; }

    public int getGroundTicks() { return groundTicks; }
    public void setGroundTicks(int groundTicks) { this.groundTicks = groundTicks; }
    public void incrementGroundTicks() { this.groundTicks++; }

    public int getSetbackCount() { return setbackCount; }
    public void setSetbackCount(int setbackCount) { this.setbackCount = setbackCount; }
    public void incrementSetbackCount() { this.setbackCount++; }

    public int getTotalSetbacks() { return totalSetbacks; }
    public void setTotalSetbacks(int totalSetbacks) { this.totalSetbacks = totalSetbacks; }

    public double getLastY() { return lastY; }
    public void setLastY(double lastY) { this.lastY = lastY; }

    public double getVerticalVelocity() { return verticalVelocity; }
    public void setVerticalVelocity(double v) { this.verticalVelocity = v; }

    public double getLastVerticalVelocity() { return lastVerticalVelocity; }
    public void setLastVerticalVelocity(double v) { this.lastVerticalVelocity = v; }

    public double getAccumulatedDeltaY() { return accumulatedDeltaY; }
    public void setAccumulatedDeltaY(double d) { this.accumulatedDeltaY = d; }

    public int getJumpAirTicks() { return jumpAirTicks; }
    public void setJumpAirTicks(int ticks) { this.jumpAirTicks = ticks; }

    public double getJumpStartY() { return jumpStartY; }
    public void setJumpStartY(double jumpStartY) { this.jumpStartY = jumpStartY; }

    public int getMaxExpectedAirTicks() { return maxExpectedAirTicks; }
    public void setMaxExpectedAirTicks(int ticks) { this.maxExpectedAirTicks = ticks; }

    public long getLastFlightAlert() { return lastFlightAlert; }
    public void setLastFlightAlert(long time) { this.lastFlightAlert = time; }

    public double getLastExpectedAirTime() { return lastExpectedAirTime; }
    public double getLastActualAirTime() { return lastActualAirTime; }
    public String getLastFailReason() { return lastFailReason; }

    public void setLastFailData(double expected, double actual, String reason) {
        this.lastExpectedAirTime = expected;
        this.lastActualAirTime = actual;
        this.lastFailReason = reason;
    }

    public boolean isWasOnGround() { return wasOnGround; }
    public void setWasOnGround(boolean wasOnGround) { this.wasOnGround = wasOnGround; }

    public boolean isInit() { return init; }
    public void setInit(boolean init) { this.init = init; }

    public void onGround() {
        airTicks = 0;
        accumulatedDeltaY = 0;
        verticalVelocity = 0;
    }

    public void resetAir() {
        airTicks = 0;
        accumulatedDeltaY = 0;
        jumpStartY = lastY;
        jumpAirTicks = 0;
    }
}
