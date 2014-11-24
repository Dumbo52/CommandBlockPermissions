package com.michaelelin.CommandBlockPermissions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class CBPPlugin extends JavaPlugin {
    
    public boolean COMMAND_BLOCK_ENABLE;
    public boolean LOG_OUTPUT;
    public String COMMAND_BLOCK_GROUP;
    
    @Override
    public void onEnable() {
        CBPListener listener = new CBPListener(this);
        
        getServer().getPluginManager().registerEvents(listener, this);
        
        saveDefaultConfig();
        loadConfiguration();
        
        ((CraftServer) getServer()).getHandle().getServer().propertyManager.setProperty("enable-command-block", COMMAND_BLOCK_ENABLE);
        
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(listener);
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling command blocks");
        ((CraftServer) getServer()).getHandle().getServer().propertyManager.setProperty("enable-command-block", false);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cbp")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("cbp.reload")) {
                        loadConfiguration();
                        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have permission for that command.");
                    }
                    return true;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "CommandBlockPermissions Commands:");
            sender.sendMessage(ChatColor.AQUA + "/cbp reload" + ChatColor.WHITE + " - Reloads the plugin.");
            return true;
        }
        return false;
    }
    
    public void loadConfiguration() {
        reloadConfig();
        COMMAND_BLOCK_ENABLE = getConfig().getBoolean("command-block-enable", true);
        ((CraftServer) getServer()).getHandle().getServer().propertyManager.setProperty("enable-command-block", COMMAND_BLOCK_ENABLE);
        LOG_OUTPUT = getConfig().getBoolean("log-output", false);
        COMMAND_BLOCK_GROUP = getConfig().getString("command-block-group", "commandblock");
    }

}
