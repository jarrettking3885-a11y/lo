package com.smp.bounty.gui;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.data.PlayerBounty;
import com.smp.bounty.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Simple read-only leaderboard GUI. Opened via "/bounty top gui".
 */
public final class LeaderboardGUI {

    public static final String TITLE = ColorUtil.color("&6&lBounty Leaderboard");

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};

    private LeaderboardGUI() {}

    public static void open(BountyPlugin plugin, Player viewer) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        List<PlayerBounty> top = plugin.getDataManager().getTop(10);

        for (int i = 0; i < top.size() && i < SLOTS.length; i++) {
            inventory.setItem(SLOTS[i], buildHead(top.get(i), i + 1));
        }

        viewer.openInventory(inventory);
    }

    private static ItemStack buildHead(PlayerBounty pb, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            // Bukkit.getOfflinePlayer(UUID) is a safe, non-blocking lookup (no network call).
            OfflinePlayer owner = Bukkit.getOfflinePlayer(pb.getUuid());
            meta.setOwningPlayer(owner);
            meta.setDisplayName(ColorUtil.color("&e#" + rank + " &f" + pb.getName()));
            meta.setLore(List.of(
                    ColorUtil.color("&7Bounty: &6" + pb.getBounty()),
                    ColorUtil.color("&7Kill streak: &c" + pb.getKillStreak())
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
