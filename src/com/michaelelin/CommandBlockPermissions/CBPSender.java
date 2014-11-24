package com.michaelelin.CommandBlockPermissions;

import net.minecraft.server.v1_7_R4.TileEntityCommandListener;

import org.bukkit.craftbukkit.v1_7_R4.command.CraftBlockCommandSender;
import org.bukkit.permissions.Permission;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;

// To be safe, this never actually has any permissions applied to it - it only references
// the bPermissions API to determine permissions.

public class CBPSender extends CraftBlockCommandSender {

    private CBPPlugin plugin;
    
    public CBPSender(CBPPlugin plugin, TileEntityCommandListener listener) {
        super(listener);
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(String message) {
        if (plugin.LOG_OUTPUT) {
            plugin.getLogger().info(String.format("[CommandBlock@%d,%d,%d,\"%s\"] << %s", getBlock().getX(), getBlock().getY(), getBlock().getZ(), ((TileEntityCommandListener) getTileEntity()).getCommand(), message));
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        if (plugin.LOG_OUTPUT) {
            for (String message : messages) {
                sendMessage(message);
            }
        }
    }
    
    @Override
    public boolean isOp() {
        return false;
    }
    
    @Override
    public boolean hasPermission(Permission perm) {
        return ApiLayer.hasPermission(getBlock().getWorld().getName(), CalculableType.GROUP, plugin.COMMAND_BLOCK_GROUP, perm.getName());
    }
    
    @Override
    public boolean hasPermission(String name) {
        return ApiLayer.hasPermission(getBlock().getWorld().getName(), CalculableType.GROUP, plugin.COMMAND_BLOCK_GROUP, name);
    }
}
