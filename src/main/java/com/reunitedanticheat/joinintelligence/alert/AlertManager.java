package com.reunitedanticheat.joinintelligence.alert;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import com.reunitedanticheat.joinintelligence.model.DetectionEvent;
import com.reunitedanticheat.joinintelligence.model.DetectionSeverity;
import com.reunitedanticheat.joinintelligence.model.DetectionSeverity.Urgency;
import com.reunitedanticheat.joinintelligence.model.PlayerJoinInfo;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class AlertManager {

    private final JoinIntelligencePlugin plugin;
    private final ConfigManager config;
    private DiscordWebhook discord;

    public AlertManager(JoinIntelligencePlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        setupDiscord();
    }

    private void setupDiscord() {
        if (config.isDiscordEnabled()) {
            this.discord = new DiscordWebhook(plugin, config.getDiscordWebhookUrl());
        } else {
            this.discord = null;
        }
    }

    public void processDetection(DetectionEvent event, Player player) {
        DetectionSeverity severity = event.getSeverity();

        if (severity.isStaffAlert()) {
            String message = event.formatAlert();

            TextComponent component = new TextComponent(message);

            if (config.isClickEnabled() && player != null) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    config.getTeleportCommand().replace("%player%", player.getName())));
            }

            broadcastToStaff(component);

            if (severity.getUrgency() == Urgency.HIGH) {
                playAlertSound();
            }
        }

        if (severity.isDiscordWebhook() && discord != null && player != null) {
            String title = severity.getPrefix() + " " + event.getCategory();
            String desc = event.formatAlert();
            discord.send(title, desc, severity.getUrgency() == Urgency.HIGH ? 16711680
                : severity.getUrgency() == Urgency.MEDIUM ? 16755200 : 65280, null);
        }

        checkAutoFlag(event);
    }

    private void checkAutoFlag(DetectionEvent event) {
        int threshold = config.getAutoFlagThreshold();
        if (threshold <= 0) return;

        plugin.getDatabaseManager().getDetectionCount(event.getPlayerName())
            .thenAccept(count -> {
                if (count == threshold) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = ChatColor.RED + "⚠ " + ChatColor.WHITE
                            + event.getPlayerName() + ChatColor.GRAY
                            + " has been flagged " + count + " times. "
                            + ChatColor.YELLOW + "/sus flagged";

                        for (Player staff : Bukkit.getOnlinePlayers()) {
                            if (staff.hasPermission("joinintelligence.alerts")) {
                                staff.sendMessage(msg);
                            }
                        }
                        Bukkit.getConsoleSender().sendMessage(msg);
                    });
                }
            });
    }

    public void processJoin(Player player, PlayerJoinInfo info) {
        if (!config.isAlertsEnabled()) return;

        sendJoinAlert(player, info);

        if (info.hasSuspiciousMods() && config.isSuspiciousModWarning()) {
            sendSuspiciousAlert(player, info);
        }

        if (info.isCheatClient() && config.isCheatClientWarning()) {
            sendCheatAlert(player, info);
        }

        if (config.isHighModCountWarning() && info.getModCount() >= config.getHighModCountThreshold()) {
            sendHighModCountAlert(player, info);
        }

        sendDiscordAlerts(info);
    }

    private void sendJoinAlert(Player player, PlayerJoinInfo info) {
        if (!config.isJoinNotification()) return;

        String message = buildMessage(config.getJoinFormat(), player, info);

        if (config.isHoverEnabled() || config.isClickEnabled()) {
            TextComponent component = new TextComponent(message);

            if (config.isHoverEnabled()) {
                String hoverText = buildMessage(config.getJoinHover(), player, info);
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(hoverText))));
            }

            if (config.isClickEnabled()) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    config.getTeleportCommand().replace("%player%", player.getName())));
            }

            broadcastToStaff(component);
        } else {
            broadcastToStaff(message);
        }
    }

    private void sendSuspiciousAlert(Player player, PlayerJoinInfo info) {
        String modsStr = String.join("&7, &c", info.getSuspiciousMods());
        String message = config.getAlertPrefix() + " " +
            config.getSuspiciousFormat()
                .replace("%player%", player.getName())
                .replace("%mods%", "&c" + modsStr)
                .replace("%mod_count%", String.valueOf(info.getModCount()));

        if (config.isHoverEnabled() || config.isClickEnabled()) {
            TextComponent component = new TextComponent(message);

            if (config.isHoverEnabled()) {
                String hoverText = config.getSuspiciousHover()
                    .replace("%player%", player.getName())
                    .replace("%mods%", "&c" + modsStr)
                    .replace("%mod_count%", String.valueOf(info.getModCount()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(hoverText))));
            }

            if (config.isClickEnabled()) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    config.getTeleportCommand().replace("%player%", player.getName())));
            }

            broadcastToStaff(component);
        } else {
            broadcastToStaff(message);
        }

        playAlertSound();
    }

    private void sendCheatAlert(Player player, PlayerJoinInfo info) {
        String message = config.getAlertPrefix() + " " +
            config.getCheatFormat()
                .replace("%player%", player.getName())
                .replace("%client%", info.getClientBrand())
                .replace("%version%", info.getMinecraftVersion());

        TextComponent component = new TextComponent(message);

        if (config.isClickEnabled()) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                config.getTeleportCommand().replace("%player%", player.getName())));
        }

        broadcastToStaff(component);
        playAlertSound();
    }

    private void sendHighModCountAlert(Player player, PlayerJoinInfo info) {
        String message = config.getAlertPrefix() + " " +
            config.getHighModCountFormat()
                .replace("%player%", player.getName())
                .replace("%mod_count%", String.valueOf(info.getModCount()))
                .replace("%client%", info.getClientTypeDisplay());

        broadcastToStaff(message);
    }

    private void sendDiscordAlerts(PlayerJoinInfo info) {
        if (discord == null || !config.isDiscordEnabled()) return;

        ConfigurationSection joinMsg = config.getDiscordMessageConfig("join");
        if (joinMsg != null) {
            ConfigurationSection fields = joinMsg.getConfigurationSection("fields");
            discord.send(
                replaceDiscord(joinMsg.getString("title", "Player Joined"), info),
                replaceDiscord(joinMsg.getString("description", "**%player%** joined the server"), info),
                joinMsg.getInt("color", 65280),
                fields
            );
        }

        if (info.hasSuspiciousMods()) {
            ConfigurationSection susMsg = config.getDiscordMessageConfig("suspicious");
            if (susMsg != null) {
                ConfigurationSection fields = susMsg.getConfigurationSection("fields");
                discord.send(
                    replaceDiscord(susMsg.getString("title", "Suspicious Mods Detected"), info),
                    replaceDiscord(susMsg.getString("description", "**%player%** has suspicious mods"), info),
                    susMsg.getInt("color", 16711680),
                    fields
                );
            }
        }
    }

    private String buildMessage(String format, Player player, PlayerJoinInfo info) {
        String modsStr = info.getModList() != null
            ? String.join(", ", info.getModList())
            : "none";
        String suspiciousStr = info.getSuspiciousMods() != null
            ? String.join(", ", info.getSuspiciousMods())
            : "none";

        return config.getAlertPrefix() + " " +
            format
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%ip%", info.getIpAddress() != null ? info.getIpAddress() : "unknown")
                .replace("%version%", info.getMinecraftVersion() != null ? info.getMinecraftVersion() : "unknown")
                .replace("%client%", info.getClientTypeDisplay())
                .replace("%brand%", info.getClientBrand() != null ? info.getClientBrand() : "unknown")
                .replace("%mod_count%", String.valueOf(info.getModCount()))
                .replace("%mods%", modsStr)
                .replace("%suspicious_mods%", suspiciousStr);
    }

    private String replaceDiscord(String template, PlayerJoinInfo info) {
        String modsStr = info.getModList() != null
            ? String.join(", ", info.getModList())
            : "none";
        String suspiciousStr = info.getSuspiciousMods() != null
            ? String.join(", ", info.getSuspiciousMods())
            : "none";

        return template
            .replace("%player%", info.getPlayerName())
            .replace("%uuid%", info.getPlayerUuid().toString())
            .replace("%ip%", info.getIpAddress() != null ? info.getIpAddress() : "unknown")
            .replace("%version%", info.getMinecraftVersion() != null ? info.getMinecraftVersion() : "unknown")
            .replace("%client%", info.getClientTypeDisplay())
            .replace("%brand%", info.getClientBrand() != null ? info.getClientBrand() : "unknown")
            .replace("%mod_count%", String.valueOf(info.getModCount()))
            .replace("%mods%", modsStr)
            .replace("%suspicious_mods%", suspiciousStr);
    }

    private void broadcastToStaff(String message) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("joinintelligence.alerts")) {
                staff.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void broadcastToStaff(TextComponent component) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("joinintelligence.alerts")) {
                staff.spigot().sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component.toLegacyText());
    }

    private void playAlertSound() {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("joinintelligence.alerts")) {
                staff.playSound(staff.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        }
    }

    public void reloadDiscord() {
        setupDiscord();
    }
}
