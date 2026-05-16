package com.reunitedanticheat.joinintelligence.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerJoinInfo {

    private final String playerName;
    private UUID playerUuid;
    private String ipAddress;
    private int protocolVersion;
    private String minecraftVersion;
    private ClientType clientType;
    private String clientBrand;
    private int modCount;
    private List<String> modList;
    private List<String> suspiciousMods;
    private boolean isCheatClient;
    private long joinTime;

    public PlayerJoinInfo(String playerName, UUID playerUuid) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.clientType = ClientType.UNKNOWN;
        this.clientBrand = "unknown";
        this.modCount = 0;
        this.modList = new ArrayList<>();
        this.suspiciousMods = new ArrayList<>();
        this.isCheatClient = false;
        this.joinTime = System.currentTimeMillis();
    }

    public String getPlayerName() { return playerName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public int getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(int protocolVersion) { this.protocolVersion = protocolVersion; }
    public String getMinecraftVersion() { return minecraftVersion; }
    public void setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; }
    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }
    public String getClientBrand() { return clientBrand; }
    public void setClientBrand(String clientBrand) { this.clientBrand = clientBrand; }
    public int getModCount() { return modCount; }
    public void setModCount(int modCount) { this.modCount = modCount; }
    public List<String> getModList() { return modList; }
    public void setModList(List<String> modList) { this.modList = modList; }
    public List<String> getSuspiciousMods() { return suspiciousMods; }
    public void setSuspiciousMods(List<String> suspiciousMods) { this.suspiciousMods = suspiciousMods; }
    public boolean isCheatClient() { return isCheatClient; }
    public void setCheatClient(boolean cheatClient) { isCheatClient = cheatClient; }
    public long getJoinTime() { return joinTime; }

    public boolean hasSuspiciousMods() {
        return suspiciousMods != null && !suspiciousMods.isEmpty();
    }

    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getClientTypeDisplay() {
        switch (clientType) {
            case VANILLA: return "Vanilla";
            case FABRIC: return "Fabric";
            case FORGE: return "Forge";
            case NEOFORGE: return "NeoForge";
            case CHEAT_CLIENT: return cheatBrandDisplay();
            case UNKNOWN:
            default: return "Unknown";
        }
    }

    private String cheatBrandDisplay() {
        return clientBrand != null && !clientBrand.isEmpty() ? clientBrand : "Cheat Client";
    }

    public enum ClientType {
        VANILLA,
        FABRIC,
        FORGE,
        NEOFORGE,
        CHEAT_CLIENT,
        UNKNOWN
    }
}
