package com.reunitedanticheat.joinintelligence;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.reunitedanticheat.joinintelligence.alert.AlertManager;
import com.reunitedanticheat.joinintelligence.api.DetectionAPI;
import com.reunitedanticheat.joinintelligence.command.DebugCommand;
import com.reunitedanticheat.joinintelligence.command.SusCommand;
import com.reunitedanticheat.joinintelligence.config.ConfigManager;
import com.reunitedanticheat.joinintelligence.database.DatabaseManager;
import com.reunitedanticheat.joinintelligence.detection.ClientDetector;
import com.reunitedanticheat.joinintelligence.detection.ModScanner;
import com.reunitedanticheat.joinintelligence.detection.flight.FlightCheck;
import com.reunitedanticheat.joinintelligence.detection.flight.SetbackManager;
import com.reunitedanticheat.joinintelligence.listener.JoinPacketListener;
import com.reunitedanticheat.joinintelligence.listener.MovementListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinIntelligencePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ClientDetector clientDetector;
    private ModScanner modScanner;
    private JoinPacketListener packetListener;
    private MovementListener movementListener;
    private AlertManager alertManager;
    private DatabaseManager databaseManager;
    private DetectionAPI detectionAPI;
    private SetbackManager setbackManager;
    private FlightCheck flightCheck;
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        if (!setupProtocolLib()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.clientDetector = new ClientDetector(configManager);
        this.modScanner = new ModScanner(configManager);
        this.alertManager = new AlertManager(this, configManager);
        this.databaseManager = new DatabaseManager(this, configManager);
        this.detectionAPI = new DetectionAPI(this);

        this.setbackManager = new SetbackManager();
        this.flightCheck = new FlightCheck(this, configManager, setbackManager);

        this.packetListener = new JoinPacketListener(this, protocolManager, clientDetector, modScanner);
        this.movementListener = new MovementListener(this, protocolManager, flightCheck, setbackManager);

        databaseManager.connect();
        packetListener.register();
        movementListener.register();

        getCommand("sus").setExecutor(new SusCommand(this, detectionAPI));
        getCommand("rac").setExecutor(new DebugCommand(this));

        getLogger().info("JoinIntelligence v" + getDescription().getVersion() + " enabled");
        getLogger().info("Modules: Join Detection, Flight Check, Severity System, /sus");
    }

    @Override
    public void onDisable() {
        if (packetListener != null) packetListener.unregister();
        if (movementListener != null) movementListener.unregister();
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("JoinIntelligence disabled");
    }

    private boolean setupProtocolLib() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib not found! JoinIntelligence requires ProtocolLib.");
            getLogger().severe("Download ProtocolLib from https://www.spigotmc.org/resources/protocollib.1997/");
            return false;
        }
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("joinintelligence")) return false;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("joinintelligence.reload")) {
                sender.sendMessage("§cYou don't have permission to reload JoinIntelligence.");
                return true;
            }

            configManager.reload();
            alertManager.reloadDiscord();
            sender.sendMessage("§aJoinIntelligence configuration reloaded.");
            getLogger().info("Configuration reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage("§6JoinIntelligence v" + getDescription().getVersion());
        sender.sendMessage("§7/joinintelligence reload §8- §7Reload configuration");
        sender.sendMessage("§7Modules: §bJoin Detection§7, §bFlight Check§7, §bSuspect System");
        return true;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public AlertManager getAlertManager() { return alertManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DetectionAPI getDetectionAPI() { return detectionAPI; }
    public MovementListener getMovementListener() { return movementListener; }
    public FlightCheck getFlightCheck() { return flightCheck; }
    public SetbackManager getSetbackManager() { return setbackManager; }
}
