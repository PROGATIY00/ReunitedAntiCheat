package com.reunitedanticheat.joinintelligence.detection;

import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo.ClientType;

import java.util.List;

public class ClientDetector {

    private final ConfigManager config;

    public ClientDetector(ConfigManager config) {
        this.config = config;
    }

    public void detect(PlayerJoinInfo info) {
        if (!config.isDetectBrand()) return;

        String brand = info.getClientBrand();
        if (brand == null || brand.isEmpty() || brand.equals("unknown")) {
            detectByChannels(info);
            return;
        }

        String lower = brand.toLowerCase();

        if (lower.contains("fabric") || lower.contains("fabricmc")) {
            info.setClientType(ClientType.FABRIC);
        } else if (lower.contains("forge")) {
            info.setClientType(ClientType.FORGE);
        } else if (lower.contains("neoforge")) {
            info.setClientType(ClientType.NEOFORGE);
        } else if (isCheatBrand(brand)) {
            info.setClientType(ClientType.CHEAT_CLIENT);
            info.setCheatClient(true);
        } else if (lower.contains("vanilla")) {
            info.setClientType(ClientType.VANILLA);
        } else {
            info.setClientType(ClientType.UNKNOWN);
        }
    }

    public boolean isCheatBrand(String brand) {
        if (brand == null || brand.isEmpty()) return false;
        List<String> cheatBrands = config.getCheatBrands();
        for (String cb : cheatBrands) {
            if (brand.equalsIgnoreCase(cb)) return true;
        }
        return false;
    }

    private void detectByChannels(PlayerJoinInfo info) {
        List<String> mods = info.getModList();
        if (mods == null || mods.isEmpty()) return;

        boolean hasFabric = false;
        boolean hasForge = false;
        boolean hasNeoForge = false;

        for (String mod : mods) {
            String lower = mod.toLowerCase();
            if (lower.contains("fabric")) hasFabric = true;
            if (lower.contains("forge")) hasForge = true;
            if (lower.contains("neoforge")) hasNeoForge = true;
        }

        if (hasNeoForge && info.getModCount() > 0) {
            info.setClientType(ClientType.NEOFORGE);
        } else if (hasForge && info.getModCount() > 0) {
            info.setClientType(ClientType.FORGE);
        } else if (hasFabric && info.getModCount() > 0) {
            info.setClientType(ClientType.FABRIC);
        }
    }
}
