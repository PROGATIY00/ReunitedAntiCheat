package com.reunitedanticheat.joinintelligence.detection;

import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;

import java.util.ArrayList;
import java.util.List;

public class ModScanner {

    private final ConfigManager config;

    public ModScanner(ConfigManager config) {
        this.config = config;
    }

    public void scan(PlayerJoinInfo info) {
        if (!config.isScanMods()) return;
        List<String> mods = info.getModList();
        if (mods == null || mods.isEmpty()) return;

        List<String> suspiciousMods = new ArrayList<>();
        List<String> keywords = config.getSuspiciousKeywords();
        List<String> ignoreMods = config.getIgnoreMods();

        for (String mod : mods) {
            if (mod == null || mod.isEmpty()) continue;

            String modLower = mod.toLowerCase();

            if (isIgnored(modLower, ignoreMods)) continue;

            for (String keyword : keywords) {
                if (modLower.contains(keyword.toLowerCase())) {
                    suspiciousMods.add(mod);
                    break;
                }
            }
        }

        info.setSuspiciousMods(suspiciousMods);

        if (suspiciousMods.size() > 0) {
            info.setCheatClient(true);
        }
    }

    private boolean isIgnored(String modLower, List<String> ignoreMods) {
        for (String ignore : ignoreMods) {
            if (modLower.equals(ignore) || modLower.contains(ignore)) {
                return true;
            }
        }
        return false;
    }
}
