package com.reunitedanticheat.joinintelligence.detection.flight;

import org.bukkit.Location;

public class FlightCheckResult {

    private final boolean illegalFlight;
    private final String reason;
    private final double expectedAirTime;
    private final double actualAirTime;
    private final int ping;
    private final int airTicks;
    private final double verticalVelocity;
    private final boolean highPing;

    public FlightCheckResult(boolean illegalFlight, String reason,
                             double expectedAirTime, double actualAirTime,
                             int ping, int airTicks, double verticalVelocity, boolean highPing) {
        this.illegalFlight = illegalFlight;
        this.reason = reason;
        this.expectedAirTime = expectedAirTime;
        this.actualAirTime = actualAirTime;
        this.ping = ping;
        this.airTicks = airTicks;
        this.verticalVelocity = verticalVelocity;
        this.highPing = highPing;
    }

    public boolean isIllegalFlight() { return illegalFlight; }
    public String getReason() { return reason; }
    public double getExpectedAirTime() { return expectedAirTime; }
    public double getActualAirTime() { return actualAirTime; }
    public int getPing() { return ping; }
    public int getAirTicks() { return airTicks; }
    public double getVerticalVelocity() { return verticalVelocity; }
    public boolean isHighPing() { return highPing; }
}
