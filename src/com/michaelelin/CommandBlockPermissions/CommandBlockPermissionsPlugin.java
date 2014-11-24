package com.michaelelin.CommandBlockPermissions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

import net.minecraft.server.v1_7_R4.CommandBlockListenerAbstract;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.server.v1_7_R4.TileEntityCommand;
import net.minecraft.server.v1_7_R4.TileEntityCommandListener;
import net.minecraft.util.io.netty.buffer.Unpooled;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.command.ServerCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class CommandBlockPermissionsPlugin extends JavaPlugin implements Listener {
    
    public boolean COMMAND_BLOCK_ENABLE;
    public boolean LOG_OUTPUT;
    public String COMMAND_BLOCK_GROUP;
    
    @Override
    public void onEnable() {
        
        getServer().getPluginManager().registerEvents(this, this);
        
        saveDefaultConfig();
        loadConfiguration();
        
        ((CraftServer) getServer()).getHandle().getServer().propertyManager.setProperty("enable-command-block", COMMAND_BLOCK_ENABLE);
        
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.CUSTOM_PAYLOAD) {
                    if (event.getPacket().getStrings().read(0).equals("MC|AdvCdm")) {
                        if (event.getPlayer().hasPermission("cbp.edit")) {
                            PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.wrappedBuffer(event.getPacket().getByteArrays().read(0)));
                            try {
                                if (packetdataserializer.readByte() == 0) {
                                    BlockState state = event.getPlayer().getWorld().getBlockAt(packetdataserializer.readInt(), packetdataserializer.readInt(), packetdataserializer.readInt()).getState();
                                    if (state.getType() == Material.COMMAND) {
                                        CommandBlock cb = (CommandBlock) state;
                                        String cmd = packetdataserializer.c(packetdataserializer.readableBytes());
                                        cb.setCommand(cmd);
                                        cb.update(false, false);
                                        event.getPlayer().sendMessage("Command set: " + cmd);
                                    }
                                }
                            } catch (Exception e) {
                                getLogger().log(Level.WARNING, "Couldn't set command block", e);
                            } finally {
                                packetdataserializer.release();
                            }
                        }
                        event.setCancelled(true);
                    }
                }
            }
        });
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
    
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.COMMAND && !event.getPlayer().hasPermission("cbp.place")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to place that.");
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.COMMAND) {
            try {
                TileEntityCommandListener listener = (TileEntityCommandListener) ((TileEntityCommand) ((CraftWorld) block.getWorld()).getTileEntityAt(block.getX(), block.getY(), block.getZ())).getCommandBlock();
                Field sender = CommandBlockListenerAbstract.class.getDeclaredField("sender");
                sender.setAccessible(true);
                if (!(sender.get(listener) instanceof CommandBlockPermissionsSender)) {
                    CommandBlockPermissionsSender cbp = new CommandBlockPermissionsSender(this, listener);
                    sender.set(listener, cbp);
                    try { // *Very* dirty way of checking if more work needs to be done (if we're running Spigot)
                        ServerCommandSender.class.getDeclaredField("blockPermInst");
                        Field perm = ServerCommandSender.class.getDeclaredField("perm");
                        perm.setAccessible(true);
                        Field mf = Field.class.getDeclaredField("modifiers");
                        mf.setAccessible(true);
                        mf.setInt(perm, perm.getModifiers() & ~Modifier.FINAL);
                        perm.set(cbp, new PermissibleBase(cbp));
                    } catch (NoSuchFieldException e) {
                        // Not running Spigot, so we don't have to do anything.
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.setCancelled(true);
            }
        }
    }
    
    public void loadConfiguration() {
        reloadConfig();
        COMMAND_BLOCK_ENABLE = getConfig().getBoolean("command-block-enable", true);
        ((CraftServer) getServer()).getHandle().getServer().propertyManager.setProperty("enable-command-block", COMMAND_BLOCK_ENABLE);
        LOG_OUTPUT = getConfig().getBoolean("log-output", false);
        COMMAND_BLOCK_GROUP = getConfig().getString("command-block-group", "commandblock");
    }

}
