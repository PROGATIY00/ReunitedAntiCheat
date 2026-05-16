package com.reunitedanticheat.joinintelligence.detection.flight;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class FlightCheck {

    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double HOVER_THRESHOLD = 0.05;
    private static final double GLIDE_RATIO = 0.6;
    private static final int HOVER_TICK_MIN = 10;

    private final JoinIntelligencePlugin plugin;
    private final ConfigManager config;
    private final SetbackManager setbackManager;

    public FlightCheck(JoinIntelligencePlugin plugin, ConfigManager config, SetbackManager setbackManager) {
        this.plugin = plugin;
        this.config = config;
        this.setbackManager = setbackManager;
    }

    public FlightCheckResult analyze(Player player, FlightData data, double y, boolean onGround, int ping) {
        if (!config.isFlightEnabled()) return null;

        if (isExempt(player)) return null;

        boolean highPing = ping >= config.getFlightHighPingThreshold();
        int maxDeltaTicks = config.getFlightMaxAirtimeTicks();
        double verticalVelocity = data.getVerticalVelocity();
        int airTicks = data.getAirTicks();

        if (!data.isWasOnGround() && onGround) {
            data.onGround();
            return null;
        }

        if (onGround) {
            data.setLastGroundLocation(player.getLocation());
            data.setLastSafeLocation(player.getLocation());
            data.onGround();
            return null;
        }

        if (airTicks < 3) return null;

        int expectedMaxTicks = calculateMaxAirTicks(data, verticalVelocity);
        data.setMaxExpectedAirTicks(expectedMaxTicks);

        int allowedTicks = expectedMaxTicks + maxDeltaTicks;

        if (airTicks > allowedTicks) {
            double actualTime = airTicks * 0.05;
            double expectedTime = expectedMaxTicks * 0.05;

            data.setLastFailData(expectedTime, actualTime, "AIR_TIME");

            return new FlightCheckResult(true, "AIR_TIME",
                expectedTime, actualTime, ping, airTicks, verticalVelocity, highPing);
        }

        if (airTicks > HOVER_TICK_MIN && Math.abs(verticalVelocity) < HOVER_THRESHOLD) {
            double actualTime = airTicks * 0.05;
            double expectedTime = expectedMaxTicks * 0.05;

            data.setLastFailData(expectedTime, actualTime, "HOVER");

            return new FlightCheckResult(true, "HOVER",
                expectedTime, actualTime, ping, airTicks, verticalVelocity, highPing);
        }

        if (verticalVelocity < 0) {
            double expectedVelocity = calculateExpectedFallVelocity(airTicks);
            if (verticalVelocity > expectedVelocity * GLIDE_RATIO && verticalVelocity < -HOVER_THRESHOLD) {
                double actualTime = airTicks * 0.05;
                double expectedTime = expectedMaxTicks * 0.05;

                data.setLastFailData(expectedTime, actualTime, "GLIDE");

                return new FlightCheckResult(true, "GLIDE",
                    expectedTime, actualTime, ping, airTicks, verticalVelocity, highPing);
            }
        }

        return null;
    }

    private int calculateMaxAirTicks(FlightData data, double verticalVelocity) {
        if (data.getJumpAirTicks() > 0 && data.getJumpAirTicks() < 10) {
            int jumpTicks = 12;
            double deltaY = data.getLastY() - data.getJumpStartY();
            if (deltaY > 0) {
                jumpTicks = (int) Math.ceil((JUMP_VELOCITY / GRAVITY) * 2) + 4;
            }
            return jumpTicks;
        }

        if (verticalVelocity >= 0) {
            int upwardTicks = (int) Math.ceil(verticalVelocity / GRAVITY);
            return upwardTicks + upwardTicks + 2;
        }

        if (verticalVelocity < -3.8) {
            return Integer.MAX_VALUE;
        }

        return 40;
    }

    private double calculateExpectedFallVelocity(int airTicks) {
        double velocity = -GRAVITY;
        for (int i = 0; i < airTicks; i++) {
            velocity = (velocity - GRAVITY) * AIR_DRAG;
            if (velocity < -3.92) velocity = -3.92;
        }
        return velocity;
    }

    public boolean isExempt(Player player) {
        if (player == null) return true;

        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return true;

        if (player.isGliding()) return true;
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return true;
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return true;
        if (player.getVehicle() != null) return true;

        Location loc = player.getLocation();
        Material ground = loc.getBlock().getRelative(0, -1, 0).getType();
        if (isBouncyBlock(ground)) return true;

        Material feet = loc.getBlock().getType();
        if (isLiquid(feet)) return true;
        if (feet == Material.BUBBLE_COLUMN) return true;

        if (loc.getBlock().isLiquid()) return true;

        return false;
    }

    private boolean isBouncyBlock(Material mat) {
        return mat == Material.SLIME_BLOCK;
    }

    private boolean isLiquid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA
            || mat.name().contains("WATER") || mat.name().contains("LAVA");
    }
}
