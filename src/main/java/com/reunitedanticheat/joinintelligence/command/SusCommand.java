package com.reunitedanticheat.joinintelligence.command;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.api.DetectionAPI;
import com.reunitedanticheat.joinintelligence.model.DetectionEvent;
import com.reunitedanticheat.joinintelligence.model.FlaggedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SusCommand implements CommandExecutor {

    private final JoinIntelligencePlugin plugin;
    private final DetectionAPI api;

    public SusCommand(JoinIntelligencePlugin plugin, DetectionAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("joinintelligence.alerts")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add":
                return handleAdd(sender, args);
            case "check":
                return handleCheck(sender, args);
            case "flagged":
            case "list":
                return handleFlagged(sender);
            case "help":
                sendUsage(sender);
                return true;
            default:
                if (args.length == 1) {
                    return handleAdd(sender, args);
                }
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sus <player> [category] [details]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        String category = args.length >= 3 ? args[2] : "suspect";
        String details = args.length >= 4 ? args[3] : "Flagged by " + sender.getName();

        api.reportDetection(target, category, details);

        String msg = ChatColor.GREEN + "Reported " + target.getName()
            + " for " + category + " (" + details + ")";
        sender.sendMessage(msg);

        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sus check <player>");
            return true;
        }

        String playerName = args[1];

        try {
            List<DetectionEvent> detections = api.getRecentDetections(playerName, 10).get();

            if (detections.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + playerName + " has no recent detections.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "─── Recent Detections for " + playerName + " ───");
            for (DetectionEvent event : detections) {
                sender.sendMessage(event.getSeverity().getPrefix() + " "
                    + event.getSeverity().getColor() + event.getCategory()
                    + ChatColor.GRAY + " (" + event.getDetails() + ")");
            }
        } catch (InterruptedException | ExecutionException e) {
            sender.sendMessage(ChatColor.RED + "Error checking player: " + e.getMessage());
        }

        return true;
    }

    private boolean handleFlagged(CommandSender sender) {
        int threshold = plugin.getConfigManager().getAutoFlagThreshold();

        try {
            List<FlaggedPlayer> flagged = plugin.getDatabaseManager()
                .getFlaggedPlayers(threshold).get();

            if (flagged.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "No players have reached "
                    + threshold + "+ detections.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "─── Flagged Players (" + threshold + "+ detections) ───");
            for (FlaggedPlayer fp : flagged) {
                long minutesAgo = (System.currentTimeMillis() - fp.getLastDetection()) / 60000;
                String timeAgo = minutesAgo < 1 ? "just now"
                    : minutesAgo < 60 ? minutesAgo + "m ago"
                    : (minutesAgo / 60) + "h ago";

                sender.sendMessage(ChatColor.RED + "⚠ " + ChatColor.WHITE + fp.getPlayerName()
                    + ChatColor.GRAY + " - " + fp.getDetectionCount() + " detections"
                    + ChatColor.DARK_GRAY + " (Last: " + fp.getLastCategory() + ", " + timeAgo + ")");
            }
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW
                + "/sus check <player>" + ChatColor.GRAY + " for details");
        } catch (InterruptedException | ExecutionException e) {
            sender.sendMessage(ChatColor.RED + "Error fetching flagged players: " + e.getMessage());
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "─── /sus Command Help ───");
        sender.sendMessage(ChatColor.YELLOW + "/sus <player> [category] [details]"
            + ChatColor.GRAY + " - Flag a player");
        sender.sendMessage(ChatColor.YELLOW + "/sus check <player>"
            + ChatColor.GRAY + " - Check recent detections");
        sender.sendMessage(ChatColor.YELLOW + "/sus flagged"
            + ChatColor.GRAY + " - List players flagged 3+ times");
        sender.sendMessage(ChatColor.YELLOW + "/sus help"
            + ChatColor.GRAY + " - Show this help");
    }
}
