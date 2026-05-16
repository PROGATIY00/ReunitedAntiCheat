package com.reunitedanticheat.joinintelligence.alert;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final JoinIntelligencePlugin plugin;
    private final String webhookUrl;

    public DiscordWebhook(JoinIntelligencePlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
    }

    public void send(String title, String description, int color, ConfigurationSection fields) {
        if (webhookUrl == null || webhookUrl.isEmpty() || "YOUR_WEBHOOK_URL_HERE".equals(webhookUrl)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = buildJson(title, description, color, fields);
                sendJson(json);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String buildJson(String title, String description, int color, ConfigurationSection fields) {
        StringBuilder json = new StringBuilder();
        json.append("{\"embeds\":[{");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        json.append("\"color\":").append(color);

        if (fields != null) {
            json.append(",\"fields\":[");
            boolean first = true;
            for (String key : fields.getKeys(false)) {
                ConfigurationSection field = fields.getConfigurationSection(key);
                if (field == null) continue;
                if (!first) json.append(",");
                first = false;
                json.append("{");
                json.append("\"name\":\"").append(escapeJson(field.getString("name", ""))).append("\",");
                json.append("\"value\":\"").append(escapeJson(field.getString("value", ""))).append("\",");
                json.append("\"inline\":").append(field.getBoolean("inline", false));
                json.append("}");
            }
            json.append("]");
        }

        json.append(",\"footer\":{\"text\":\"JoinIntelligence v" + plugin.getDescription().getVersion() + "\"}");
        json.append(",\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
        json.append("}]}");

        return json.toString();
    }

    private void sendJson(String json) throws Exception {
        URI uri = new URI(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "JoinIntelligence/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            plugin.getLogger().warning("Discord webhook returned code " + responseCode);
        }

        conn.disconnect();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String replacePlaceholders(String template,
                                       String player, String uuid, String ip,
                                       String version, String client, String brand,
                                       int modCount, String modList, String suspiciousMods) {
        return template
            .replace("%player%", player != null ? player : "unknown")
            .replace("%uuid%", uuid != null ? uuid : "unknown")
            .replace("%ip%", ip != null ? ip : "unknown")
            .replace("%version%", version != null ? version : "unknown")
            .replace("%client%", client != null ? client : "unknown")
            .replace("%brand%", brand != null ? brand : "unknown")
            .replace("%mod_count%", String.valueOf(modCount))
            .replace("%mods%", modList != null ? modList : "none")
            .replace("%suspicious_mods%", suspiciousMods != null ? suspiciousMods : "none");
    }
}
