package com.reunitedanticheat.joinintelligence.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.detection.ClientDetector;
import com.reunitedanticheat.joinintelligence.detection.ModScanner;
import com.reunitedanticheat.joinintelligence.detection.VersionMapper;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JoinPacketListener {

    private final JoinIntelligencePlugin plugin;
    private final ProtocolManager protocolManager;
    private final ClientDetector clientDetector;
    private final ModScanner modScanner;

    private final Map<UUID, PlayerJoinInfo> pendingJoins = new ConcurrentHashMap<>();

    public JoinPacketListener(JoinIntelligencePlugin plugin, ProtocolManager protocolManager,
                              ClientDetector clientDetector, ModScanner modScanner) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.clientDetector = clientDetector;
        this.modScanner = modScanner;
    }

    public void register() {
        protocolManager.addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Handshake.Client.SET_PROTOCOL) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    onHandshake(event);
                }
            }
        );

        protocolManager.addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Login.Client.CUSTOM_PAYLOAD) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    onLoginPayload(event);
                }
            }
        );

        protocolManager.addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.CUSTOM_PAYLOAD) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    onPlayPayload(event);
                }
            }
        );
    }

    public void unregister() {
        protocolManager.removePacketListeners(plugin);
    }

    private void onHandshake(PacketEvent event) {
        try {
            int protocolVersion = event.getPacket().getIntegers().read(0);
            if (!VersionMapper.isValidProtocol(protocolVersion)) return;

            String username = extractUsername(event);
            if (username == null) return;

            PlayerJoinInfo info = new PlayerJoinInfo(username, UUID.randomUUID());
            info.setProtocolVersion(protocolVersion);
            info.setMinecraftVersion(VersionMapper.getVersion(protocolVersion));

            if (event.getPlayer() != null && event.getPlayer().getAddress() != null) {
                info.setIpAddress(event.getPlayer().getAddress().getAddress().getHostAddress());
            }

            pendingJoins.put(info.getPlayerUuid(), info);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process handshake: " + e.getMessage());
        }
    }

    private void onLoginPayload(PacketEvent event) {
        try {
            if (event.getPlayer() == null) return;
            UUID uuid = event.getPlayer().getUniqueId();

            PlayerJoinInfo info = pendingJoins.get(uuid);
            if (info == null) {
                info = new PlayerJoinInfo(event.getPlayer().getName(), uuid);
                if (event.getPlayer().getAddress() != null) {
                    info.setIpAddress(event.getPlayer().getAddress().getAddress().getHostAddress());
                }
                pendingJoins.put(uuid, info);
            }

            String channel = readChannel(event);
            byte[] data = event.getPacket().getByteArrays().readSafely(0);
            processPayload(info, channel, data);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process login payload: " + e.getMessage());
        }
    }

    private void onPlayPayload(PacketEvent event) {
        try {
            if (event.getPlayer() == null) return;
            Player player = event.getPlayer();

            String channel = readChannel(event);
            byte[] data = event.getPacket().getByteArrays().readSafely(0);
            if (channel == null) return;

            PlayerJoinInfo info = pendingJoins.get(player.getUniqueId());
            if (info == null) {
                info = new PlayerJoinInfo(player.getName(), player.getUniqueId());
                if (player.getAddress() != null) {
                    info.setIpAddress(player.getAddress().getAddress().getHostAddress());
                }
                info.setMinecraftVersion(getServerVersion());
                pendingJoins.put(player.getUniqueId(), info);
            }

            boolean processed = processPayload(info, channel, data);

            if (processed && channel.equals("minecraft:brand")) {
                finalizeJoin(player, info);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process play payload: " + e.getMessage());
        }
    }

    private boolean processPayload(PlayerJoinInfo info, String channel, byte[] data) {
        if (channel == null || data == null) return false;

        String channelLower = channel.toLowerCase();

        if (channelLower.equals("minecraft:brand") || channelLower.equals("mc|brand")) {
            String brand = new String(data, StandardCharsets.UTF_8).trim();
            brand = brand.replace("\u0000", "").replace("\"", "").trim();
            info.setClientBrand(brand);
            clientDetector.detect(info);
            return true;

        } else if (channelLower.startsWith("fabric:") || channelLower.contains("fabric")) {
            handleFabricChannel(info, channel, data);
            return true;

        } else if (channelLower.startsWith("fml") || channelLower.startsWith("forge:") || channelLower.contains("forge")) {
            handleForgeChannel(info, channel, data);
            return true;
        }

        return false;
    }

    private void handleFabricChannel(PlayerJoinInfo info, String channel, byte[] data) {
        if (channel.equals("fabric:mod/list") || channel.equals("fml:mod_list")) {
            String modData = new String(data, StandardCharsets.UTF_8);
            parseFabricModList(info, modData);
        }

        if (channel.equals("fabric:register") || channel.startsWith("fabric:registry/")) {
            String content = new String(data, StandardCharsets.UTF_8);
            String[] parts = content.split("\0");
            for (String part : parts) {
                if (part.startsWith("fabric:")) continue;
                addMod(info, part.trim());
            }
        }

        if (info.getClientType() == PlayerJoinInfo.ClientType.UNKNOWN) {
            info.setClientType(PlayerJoinInfo.ClientType.FABRIC);
        }
    }

    private void handleForgeChannel(PlayerJoinInfo info, String channel, byte[] data) {
        boolean isNeoForge = channel.toLowerCase().contains("neoforge");

        if (isNeoForge) {
            if (info.getClientType() != PlayerJoinInfo.ClientType.NEOFORGE) {
                info.setClientType(PlayerJoinInfo.ClientType.NEOFORGE);
            }
        } else if (info.getClientType() != PlayerJoinInfo.ClientType.NEOFORGE) {
            info.setClientType(PlayerJoinInfo.ClientType.FORGE);
        }

        String content = new String(data, StandardCharsets.UTF_8);
        parseForgeModList(info, content);
    }

    private void parseFabricModList(PlayerJoinInfo info, String data) {
        try {
            String[] mods = data.split("[\\s,;]+");
            for (String mod : mods) {
                mod = mod.trim().replaceAll("[^a-zA-Z0-9_.\\-]", "");
                if (!mod.isEmpty() && !mod.equals("fabric")) {
                    addMod(info, mod);
                }
            }
            if (!info.getModList().isEmpty()) {
                info.setModCount(info.getModList().size());
                info.setClientType(PlayerJoinInfo.ClientType.FABRIC);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse Fabric mod list: " + e.getMessage());
        }
    }

    private void parseForgeModList(PlayerJoinInfo info, String data) {
        try {
            if (data.contains("mods") || data.contains("modlist")) {
                String[] mods = data.split("[\\s,;:]+");
                for (String mod : mods) {
                    mod = mod.trim().replaceAll("[^a-zA-Z0-9_.\\-]", "");
                    if (!mod.isEmpty() && mod.length() > 2) {
                        addMod(info, mod);
                    }
                }
                info.setModCount(info.getModList().size());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse Forge mod list: " + e.getMessage());
        }
    }

    private void addMod(PlayerJoinInfo info, String mod) {
        if (mod.isEmpty() || mod.equalsIgnoreCase("minecraft")) return;
        List<String> mods = info.getModList();
        if (!mods.contains(mod)) {
            mods.add(mod);
            info.setModCount(mods.size());
        }
    }

    public void finalizeJoin(Player player, PlayerJoinInfo info) {
        if (info == null) {
            info = pendingJoins.get(player.getUniqueId());
            if (info == null) {
                info = new PlayerJoinInfo(player.getName(), player.getUniqueId());
                if (player.getAddress() != null) {
                    info.setIpAddress(player.getAddress().getAddress().getHostAddress());
                }
                info.setMinecraftVersion(getServerVersion());
            }
        }

        PlayerJoinInfo finalInfo = info;
        Bukkit.getScheduler().runTask(plugin, () -> {
            modScanner.scan(finalInfo);
            plugin.getAlertManager().processJoin(player, finalInfo);
            plugin.getDatabaseManager().logJoin(finalInfo);
            pendingJoins.remove(player.getUniqueId());
        });
    }

    private String readChannel(PacketEvent event) {
        try {
            return event.getPacket().getMinecraftKeys().readSafely(0).getFullKey();
        } catch (Exception e) {
            try {
                return event.getPacket().getStrings().readSafely(0);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private String extractUsername(PacketEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) return player.getName();
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String getServerVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }
}
