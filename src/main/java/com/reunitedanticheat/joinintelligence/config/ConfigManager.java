package com.reunitedanticheat.joinintelligence.config;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.reunitedanticheat.joinintelligence.model.DetectionSeverity;
import com.reunitedanticheat.joinintelligence.model.DetectionSeverity.Urgency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {

    private final JoinIntelligencePlugin plugin;
    private FileConfiguration config;

    private boolean alertOnJoin;
    private boolean databaseLogging;
    private boolean checkUpdates;

    private boolean detectVersion;
    private boolean detectBrand;
    private boolean trackModCount;
    private boolean scanMods;

    private boolean alertsEnabled;
    private boolean joinNotification;
    private boolean suspiciousModWarning;
    private boolean cheatClientWarning;
    private boolean highModCountWarning;
    private int highModCountThreshold;

    private String alertPrefix;
    private String joinFormat;
    private String clientBrandFormat;
    private String suspiciousFormat;
    private String cheatFormat;
    private String highModCountFormat;

    private boolean hoverEnabled;
    private String joinHover;
    private String suspiciousHover;

    private boolean clickEnabled;
    private String teleportCommand;
    private String inspectCommand;

    private boolean discordEnabled;
    private String discordWebhookUrl;

    private List<String> ignoreMods;
    private List<String> suspiciousKeywords;
    private List<String> cheatBrands;

    private String databaseType;
    private String databaseFile;
    private String databaseTable;
    private boolean databaseAutoCreate;
    private int databaseRetentionDays;

    private int autoFlagThreshold;

    private boolean flightEnabled;
    private int flightMaxAirtimeTicks;
    private int flightHighPingThreshold;
    private boolean flightHighPingKick;
    private String flightHighPingKickMessage;
    private boolean flightSetbackEnabled;
    private int flightVlAmount;

    private Map<String, DetectionSeverity> severities;
    private DetectionSeverity defaultSeverity;

    public ConfigManager(JoinIntelligencePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadSettings();
        loadDetection();
        loadAlerts();
        loadStaffAlert();
        loadDiscord();
        loadIgnoreMods();
        loadSuspiciousKeywords();
        loadCheatBrands();
        loadFlight();
        loadSeverities();
    }

    private void loadDatabase() {
        ConfigurationSection db = config.getConfigurationSection("database");
        if (db == null) return;
        databaseType = db.getString("type", "H2");
        databaseFile = db.getString("h2.file", "plugins/JoinIntelligence/data");
        databaseTable = db.getString("h2.table", "join_logs");
        databaseAutoCreate = db.getBoolean("h2.auto-create", true);
        databaseRetentionDays = db.getInt("retention-days", 90);
        autoFlagThreshold = db.getInt("auto-flag-threshold", 3);
    }

    private void loadSettings() {
        alertOnJoin = config.getBoolean("settings.alert-on-join", true);
        databaseLogging = config.getBoolean("settings.database-logging", true);
        checkUpdates = config.getBoolean("settings.check-updates", true);
    }

    private void loadDetection() {
        ConfigurationSection cd = config.getConfigurationSection("client-detection");
        if (cd == null) return;
        detectVersion = cd.getBoolean("detect-version", true);
        detectBrand = cd.getBoolean("detect-brand", true);
        trackModCount = cd.getBoolean("track-mod-count", true);
        scanMods = cd.getBoolean("scan-mods", true);
    }

    private void loadAlerts() {
        ConfigurationSection a = config.getConfigurationSection("alerts");
        if (a == null) return;
        alertsEnabled = a.getBoolean("enabled", true);
        joinNotification = a.getBoolean("join-notification", true);
        suspiciousModWarning = a.getBoolean("suspicious-mod-warning", true);
        cheatClientWarning = a.getBoolean("cheat-client-warning", true);
        highModCountWarning = a.getBoolean("high-mod-count-warning", true);
        highModCountThreshold = a.getInt("high-mod-count-threshold", 100);
    }

    private void loadStaffAlert() {
        ConfigurationSection sa = config.getConfigurationSection("staff-alert");
        if (sa == null) return;
        alertPrefix = color(sa.getString("prefix", "&8[&bJoinIntelligence&8]"));
        joinFormat = color(sa.getString("join-format", "&e%player% &7joined using &b%version% &f%client% &7with &e%mod_count% &7mods"));
        clientBrandFormat = color(sa.getString("client-brand-format", "&7Client: &f%brand%"));
        suspiciousFormat = color(sa.getString("suspicious-format", "&c⚠ &e%player% &7has suspicious mods: &c%mods%"));
        cheatFormat = color(sa.getString("cheat-format", "&4⚠ &e%player% &7may be using: &4%client%"));
        highModCountFormat = color(sa.getString("high-mod-count-format", "&6⚠ &e%player% &7has &6%mod_count% &7mods"));

        ConfigurationSection h = sa.getConfigurationSection("hover");
        if (h != null) {
            hoverEnabled = h.getBoolean("enabled", true);
            joinHover = color(h.getString("join-hover", "&7IP: &f%ip%\n&7Version: &f%version%\n&7Client: &f%client%\n&7Mods: &f%mod_count%\n&7UUID: &f%uuid%"));
            suspiciousHover = color(h.getString("suspicious-hover", "&7Suspicious mods: &c%mods%\n&7Click to teleport"));
        }

        ConfigurationSection c = sa.getConfigurationSection("click");
        if (c != null) {
            clickEnabled = c.getBoolean("enabled", true);
            teleportCommand = c.getString("teleport-command", "/tp %player%");
            inspectCommand = c.getString("inspect-command", "/inventory %player%");
        }
    }

    private void loadDiscord() {
        ConfigurationSection d = config.getConfigurationSection("discord");
        if (d == null) return;
        discordEnabled = d.getBoolean("enabled", false);
        discordWebhookUrl = d.getString("webhook-url", "");
    }

    private void loadIgnoreMods() {
        ignoreMods = config.getStringList("ignore-mods");
        if (ignoreMods == null) ignoreMods = new ArrayList<>();
        ignoreMods = ignoreMods.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private void loadSuspiciousKeywords() {
        suspiciousKeywords = config.getStringList("suspicious-keywords");
        if (suspiciousKeywords == null) suspiciousKeywords = new ArrayList<>();
    }

    private void loadCheatBrands() {
        cheatBrands = config.getStringList("cheat-brands");
        if (cheatBrands == null) cheatBrands = new ArrayList<>();
    }

    private void loadFlight() {
        ConfigurationSection fc = config.getConfigurationSection("flight-check");
        if (fc == null) return;
        flightEnabled = fc.getBoolean("enabled", true);
        flightMaxAirtimeTicks = fc.getInt("max-airtime-ticks", 40);
        flightHighPingThreshold = fc.getInt("high-ping-threshold", 400);
        ConfigurationSection hph = fc.getConfigurationSection("high-ping-handling");
        if (hph != null) {
            flightHighPingKick = hph.getBoolean("kick-player", true);
            flightHighPingKickMessage = color(hph.getString("kick-message",
                "&cUnstable connection detected. Please reconnect."));
        }
        ConfigurationSection sb = fc.getConfigurationSection("setback");
        if (sb != null) {
            flightSetbackEnabled = sb.getBoolean("enabled", true);
        }
        ConfigurationSection vl = fc.getConfigurationSection("vl");
        if (vl != null) {
            flightVlAmount = vl.getInt("amount", 10);
        }
    }

    private void loadSeverities() {
        severities = new HashMap<>();
        ConfigurationSection ds = config.getConfigurationSection("detection-severity");
        if (ds == null) return;

        for (String category : ds.getKeys(false)) {
            ConfigurationSection s = ds.getConfigurationSection(category);
            if (s == null) continue;

            try {
                Urgency urgency = Urgency.valueOf(s.getString("urgency", "MEDIUM").toUpperCase());
                ChatColor color = ChatColor.valueOf(s.getString("color", "YELLOW").toUpperCase());
                String prefix = color(s.getString("prefix", "&6[MEDIUM]"));
                boolean staffAlert = s.getBoolean("staff-alert", true);
                boolean autoLog = s.getBoolean("auto-log", true);
                boolean discordWebhook = s.getBoolean("discord-webhook", true);

                severities.put(category.toLowerCase(), new DetectionSeverity(
                    urgency, color, prefix, staffAlert, autoLog, discordWebhook
                ));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid severity config for '" + category + "': " + e.getMessage());
            }
        }

        defaultSeverity = severities.get("cheating");
        if (defaultSeverity == null) {
            defaultSeverity = new DetectionSeverity(Urgency.MEDIUM, ChatColor.YELLOW,
                "&6[MEDIUM]", true, true, true);
        }
    }

    public DetectionSeverity getSeverity(String category) {
        if (category == null) return defaultSeverity;
        return severities.getOrDefault(category.toLowerCase(), defaultSeverity);
    }

    public Map<String, DetectionSeverity> getAllSeverities() {
        return Collections.unmodifiableMap(severities);
    }

    public DetectionSeverity getDefaultSeverity() {
        return defaultSeverity;
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public boolean isAlertOnJoin() { return alertOnJoin; }
    public boolean isDatabaseLogging() { return databaseLogging; }
    public boolean isCheckUpdates() { return checkUpdates; }
    public boolean isDetectVersion() { return detectVersion; }
    public boolean isDetectBrand() { return detectBrand; }
    public boolean isTrackModCount() { return trackModCount; }
    public boolean isScanMods() { return scanMods; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public boolean isJoinNotification() { return joinNotification; }
    public boolean isSuspiciousModWarning() { return suspiciousModWarning; }
    public boolean isCheatClientWarning() { return cheatClientWarning; }
    public boolean isHighModCountWarning() { return highModCountWarning; }
    public int getHighModCountThreshold() { return highModCountThreshold; }

    public String getAlertPrefix() { return alertPrefix; }
    public String getJoinFormat() { return joinFormat; }
    public String getClientBrandFormat() { return clientBrandFormat; }
    public String getSuspiciousFormat() { return suspiciousFormat; }
    public String getCheatFormat() { return cheatFormat; }
    public String getHighModCountFormat() { return highModCountFormat; }
    public boolean isHoverEnabled() { return hoverEnabled; }
    public String getJoinHover() { return joinHover; }
    public String getSuspiciousHover() { return suspiciousHover; }
    public boolean isClickEnabled() { return clickEnabled; }
    public String getTeleportCommand() { return teleportCommand; }
    public String getInspectCommand() { return inspectCommand; }

    public boolean isDiscordEnabled() { return discordEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }

    public List<String> getIgnoreMods() { return ignoreMods; }
    public List<String> getSuspiciousKeywords() { return suspiciousKeywords; }
    public List<String> getCheatBrands() { return cheatBrands; }

    public String getDatabaseType() { return databaseType; }
    public String getDatabaseFile() { return databaseFile; }
    public String getDatabaseTable() { return databaseTable; }
    public boolean isDatabaseAutoCreate() { return databaseAutoCreate; }
    public int getDatabaseRetentionDays() { return databaseRetentionDays; }

    public int getAutoFlagThreshold() { return autoFlagThreshold; }

    public boolean isFlightEnabled() { return flightEnabled; }
    public int getFlightMaxAirtimeTicks() { return flightMaxAirtimeTicks; }
    public int getFlightHighPingThreshold() { return flightHighPingThreshold; }
    public boolean isFlightHighPingKick() { return flightHighPingKick; }
    public String getFlightHighPingKickMessage() { return flightHighPingKickMessage; }
    public boolean isFlightSetbackEnabled() { return flightSetbackEnabled; }
    public int getFlightVlAmount() { return flightVlAmount; }

    public ConfigurationSection getDiscordMessageConfig(String key) {
        return config.getConfigurationSection("discord." + key + "-message");
    }
}
