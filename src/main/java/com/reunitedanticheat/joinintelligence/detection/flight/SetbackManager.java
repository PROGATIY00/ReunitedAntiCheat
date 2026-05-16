package com.reunitedanticheat.joinintelligence.detection.flight;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetbackManager {

    private static final int MAX_SETBACK_ATTEMPTS = 5;
    private static final long SETBACK_COOLDOWN = 500;

    private final Map<UUID, Location> safeLocations = new HashMap<>();
    private final Map<UUID, Long> lastSetbackTime = new HashMap<>();
    private final Map<UUID, Integer> setbackCount = new HashMap<>();

    public void storeSafeLocation(Player player) {
        Location loc = player.getLocation();
        if (isSafeLocation(loc)) {
            safeLocations.put(player.getUniqueId(), loc.clone());
        }
    }

    public void storeSafeLocation(Player player, Location loc) {
        if (loc != null && isSafeLocation(loc)) {
            safeLocations.put(player.getUniqueId(), loc.clone());
        }
    }

    public Location getSafeLocation(Player player) {
        Location loc = safeLocations.get(player.getUniqueId());
        if (loc != null && isChunkLoaded(loc)) {
            return loc;
        }

        Location bedSpawn = player.getBedSpawnLocation();
        if (bedSpawn != null && isChunkLoaded(bedSpawn)) {
            return bedSpawn;
        }

        return player.getWorld().getSpawnLocation();
    }

    public void setback(Player player) {
        UUID uuid = player.getUniqueId();

        long now = System.currentTimeMillis();
        Long lastTime = lastSetbackTime.get(uuid);
        if (lastTime != null && (now - lastTime) < SETBACK_COOLDOWN) return;

        int attempts = setbackCount.getOrDefault(uuid, 0);
        if (attempts >= MAX_SETBACK_ATTEMPTS) return;
        setbackCount.put(uuid, attempts + 1);

        Location safe = getSafeLocation(player);
        if (safe == null || !isChunkLoaded(safe)) return;

        player.teleportAsync(safe).thenAccept(success -> {
            if (success) {
                lastSetbackTime.put(uuid, now);
                player.setFallDistance(0);
            }
        });
    }

    public void resetSetbackCount(Player player) {
        setbackCount.remove(player.getUniqueId());
    }

    public int getSetbackCount(Player player) {
        return setbackCount.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean isSafeLocation(Location loc) {
        if (loc == null || !isChunkLoaded(loc)) return false;

        Block feet = loc.getBlock();
        Block below = feet.getRelative(0, -1, 0);
        Block above = feet.getRelative(0, 1, 0);

        if (isDangerous(feet.getType()) || isDangerous(above.getType())) {
            return false;
        }

        return below.getType().isSolid();
    }

    private boolean isDangerous(Material mat) {
        return mat == Material.LAVA || mat == Material.WATER
            || mat == Material.CACTUS || mat == Material.MAGMA_BLOCK
            || mat == Material.FIRE || mat == Material.SOUL_FIRE
            || mat.name().contains("LAVA") || mat.name().contains("WATER");
    }

    private boolean isChunkLoaded(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        safeLocations.remove(uuid);
        lastSetbackTime.remove(uuid);
        setbackCount.remove(uuid);
    }
}
