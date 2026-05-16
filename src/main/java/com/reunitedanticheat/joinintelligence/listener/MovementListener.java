package com.reunitedanticheat.joinintelligence.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.detection.flight.FlightCheck;
import com.reunitedanticheat.joinintelligence.detection.flight.FlightCheckResult;
import com.reunitedanticheat.joinintelligence.detection.flight.FlightData;
import com.reunitedanticheat.joinintelligence.detection.flight.SetbackManager;
import com.reunitedanticheat.joinintelligence.model.DetectionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MovementListener {

    private final JoinIntelligencePlugin plugin;
    private final ProtocolManager protocolManager;
    private final FlightCheck flightCheck;
    private final SetbackManager setbackManager;

    private final Map<UUID, FlightData> flightDataMap = new ConcurrentHashMap<>();

    public MovementListener(JoinIntelligencePlugin plugin, ProtocolManager protocolManager,
                            FlightCheck flightCheck, SetbackManager setbackManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.flightCheck = flightCheck;
        this.setbackManager = setbackManager;
    }

    public void register() {
        protocolManager.addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.FLYING) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    onMovement(event);
                }
            }
        );
    }

    public void unregister() {
        protocolManager.removePacketListeners(plugin);
    }

    private void onMovement(PacketEvent event) {
        if (event.isCancelled()) return;
        if (event.getPlayer() == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        FlightData data = flightDataMap.computeIfAbsent(uuid,
            k -> new FlightData(uuid, player.getName()));

        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacketType();

        try {
            double y = 0;
            Boolean onGroundObj = packet.getBooleans().readSafely(0);
            boolean onGround = onGroundObj != null && onGroundObj;

            if (type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK) {
                Double yObj = packet.getDoubles().readSafely(1);
                y = yObj != null ? yObj : 0.0;
            }

            if (!data.isInit()) {
                data.setInit(true);
                data.setLastY(y);
                data.setWasOnGround(onGround);
                if (onGround) {
                    data.setLastGroundLocation(player.getLocation());
                    data.setLastSafeLocation(player.getLocation());
                }
                return;
            }

            double deltaY = y - data.getLastY();
            data.setLastVerticalVelocity(data.getVerticalVelocity());
            data.setVerticalVelocity(deltaY);
            data.setLastY(y);

            if (onGround) {
                data.incrementGroundTicks();
                if (!data.isWasOnGround()) {
                    data.onGround();
                    setbackManager.storeSafeLocation(player);
                    setbackManager.resetSetbackCount(player);
                }
                data.setWasOnGround(true);
                return;
            }

            data.setWasOnGround(false);
            data.incrementAirTicks();

            if (data.getAirTicks() == 1) {
                data.setJumpStartY(y);
            }

            data.setAccumulatedDeltaY(data.getAccumulatedDeltaY() + deltaY);

            int ping = player.getPing();
            FlightCheckResult result = flightCheck.analyze(player, data, y, onGround, ping);

            if (result == null) return;

            FlightCheckResult finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> {
                handleResult(player, finalResult);
            });

        } catch (Exception e) {
            plugin.getLogger().warning("Movement check error: " + e.getMessage());
        }
    }

    private void handleResult(Player player, FlightCheckResult result) {
        if (!result.isIllegalFlight()) return;

        String category;
        if (result.isHighPing()) {
            category = "flight_high_ping";
        } else {
            switch (result.getReason()) {
                case "HOVER": category = "fly"; break;
                case "GLIDE": category = "fly"; break;
                default: category = "fly";
            }
        }

        DetectionEvent event = new DetectionEvent(
            player.getName(), player.getUniqueId(), category,
            result.getReason() + " | Expected: " + String.format("%.1fs", result.getExpectedAirTime())
                + " Actual: " + String.format("%.1fs", result.getActualAirTime())
                + " Ping: " + result.getPing() + "ms",
            plugin.getConfigManager().getSeverity(category)
        );

        plugin.getAlertManager().processDetection(event, player);
        plugin.getDatabaseManager().logDetection(event);

        if (result.isHighPing()) {
            if (plugin.getConfigManager().isFlightHighPingKick()) {
                player.kickPlayer(plugin.getConfigManager().getFlightHighPingKickMessage());
            }
            return;
        }

        if (plugin.getConfigManager().isFlightSetbackEnabled()) {
            setbackManager.setback(player);
        }
    }

    public FlightData getFlightData(Player player) {
        return flightDataMap.get(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        FlightData data = flightDataMap.remove(player.getUniqueId());
        setbackManager.removePlayer(player);
    }
}
