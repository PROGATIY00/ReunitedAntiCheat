package com.reunitedanticheat.joinintelligence.model;

import org.bukkit.ChatColor;

public class DetectionSeverity {

    private final Urgency urgency;
    private final ChatColor color;
    private final String prefix;
    private final boolean staffAlert;
    private final boolean autoLog;
    private final boolean discordWebhook;

    public DetectionSeverity(Urgency urgency, ChatColor color, String prefix,
                             boolean staffAlert, boolean autoLog, boolean discordWebhook) {
        this.urgency = urgency;
        this.color = color;
        this.prefix = prefix;
        this.staffAlert = staffAlert;
        this.autoLog = autoLog;
        this.discordWebhook = discordWebhook;
    }

    public Urgency getUrgency() { return urgency; }
    public ChatColor getColor() { return color; }
    public String getPrefix() { return prefix; }
    public boolean isStaffAlert() { return staffAlert; }
    public boolean isAutoLog() { return autoLog; }
    public boolean isDiscordWebhook() { return discordWebhook; }

    public enum Urgency {
        LOW,
        MEDIUM,
        HIGH
    }
}
