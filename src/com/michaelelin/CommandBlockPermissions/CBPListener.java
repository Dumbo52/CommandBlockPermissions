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
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.command.ServerCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.permissions.PermissibleBase;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.*;

public class CBPListener extends PacketAdapter implements Listener {
    
    private CBPPlugin plugin;
    
    public CBPListener(CBPPlugin plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.TILE_ENTITY_DATA, PacketType.Play.Client.CUSTOM_PAYLOAD);
        this.plugin = plugin;
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
                if (!(sender.get(listener) instanceof CBPSender)) {
                    CBPSender cbp = new CBPSender(plugin, listener);
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
    
    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.TILE_ENTITY_DATA && !event.getPlayer().hasPermission("cbp.read")) {
            NbtCompound nbt = (NbtCompound) event.getPacket().getNbtModifier().read(0);
            if (nbt.getString("id").equals("Control")) {
                nbt.remove("Command");
                nbt.remove("LastOutput");
            }
        }
    }
    
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
                        plugin.getLogger().log(Level.WARNING, "Couldn't set command block", e);
                    } finally {
                        packetdataserializer.release();
                    }
                }
                event.setCancelled(true);
            }
        }
    }
    
}
