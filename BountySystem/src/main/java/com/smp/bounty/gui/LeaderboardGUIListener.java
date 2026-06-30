package com.smp.bounty.gui;

import com.smp.bounty.BountyPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class LeaderboardGUIListener implements Listener {

    private final BountyPlugin plugin;

    public LeaderboardGUIListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(LeaderboardGUI.TITLE)) {
            event.setCancelled(true);
        }
    }
}
