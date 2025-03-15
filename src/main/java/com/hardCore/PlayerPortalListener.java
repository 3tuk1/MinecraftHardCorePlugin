package com.hardCore;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PlayerPortalListener implements Listener {
    private static boolean RESET = true;
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            if (ReadSettings.isNoresetOnEndDeath()) {
                // エンド到達時にワールドをリセットしない
                RESET = false;
            }
        }
    }
    public static boolean isReset() {
        return RESET;
    }
}

