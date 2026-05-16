package com.reunitedanticheat.joinintelligence.command;

import com.reunitedanticheat.joinintelligence.JoinIntelligencePlugin;
import com.reunitedanticheat.joinintelligence.detection.flight.FlightData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommand implements CommandExecutor {

    private final JoinIntelligencePlugin plugin;

    public DebugCommand(JoinIntelligencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("joinintelligence.alerts")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rac debug flight <player>");
            return true;
        }

        if (!args[0].equalsIgnoreCase("debug") || !args[1].equalsIgnoreCase("flight")) {
            sender.sendMessage(ChatColor.RED + "Usage: /rac debug flight <player>");
            return true;
        }

        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player ? (Player) sender : null);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + (args.length >= 3 ? args[2] : ""));
            return true;
        }

        FlightData data = plugin.getMovementListener().getFlightData(target);
        if (data == null) {
            sender.sendMessage(ChatColor.YELLOW + "No flight data for " + target.getName());
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "─── Flight Debug: " + target.getName() + " ───");
        sender.sendMessage(ChatColor.GRAY + "Ping: " + ChatColor.WHITE + target.getPing() + "ms");
        sender.sendMessage(ChatColor.GRAY + "TPS: " + ChatColor.WHITE + String.format("%.1f", Bukkit.getTPS()[0]));
        sender.sendMessage(ChatColor.GRAY + "Air Ticks: " + ChatColor.WHITE + data.getAirTicks());
        sender.sendMessage(ChatColor.GRAY + "Vertical Velocity: " + ChatColor.WHITE + String.format("%.4f", data.getVerticalVelocity()));
        sender.sendMessage(ChatColor.GRAY + "Last Vertical Vel: " + ChatColor.WHITE + String.format("%.4f", data.getLastVerticalVelocity()));
        sender.sendMessage(ChatColor.GRAY + "Accumulated ΔY: " + ChatColor.WHITE + String.format("%.4f", data.getAccumulatedDeltaY()));
        sender.sendMessage(ChatColor.GRAY + "On Ground: " + ChatColor.WHITE + target.isOnGround());
        sender.sendMessage(ChatColor.GRAY + "Expected Air Time: " + ChatColor.WHITE + String.format("%.1fs", data.getLastExpectedAirTime()));
        sender.sendMessage(ChatColor.GRAY + "Actual Air Time: " + ChatColor.WHITE + String.format("%.1fs", data.getLastActualAirTime()));
        sender.sendMessage(ChatColor.GRAY + "Last Fail: " + ChatColor.WHITE + (data.getLastFailReason() != null ? data.getLastFailReason() : "none"));
        sender.sendMessage(ChatColor.GRAY + "Setback Count: " + ChatColor.WHITE + data.getTotalSetbacks());
        sender.sendMessage(ChatColor.GRAY + "Expected Max Ticks: " + ChatColor.WHITE + data.getMaxExpectedAirTicks());
        sender.sendMessage(ChatColor.GRAY + "Check State: " + ChatColor.WHITE + (data.getAirTicks() > 3 ? "ACTIVE" : "IDLE"));

        return true;
    }
}
