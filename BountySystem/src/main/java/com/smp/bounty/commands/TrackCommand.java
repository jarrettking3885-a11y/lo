package com.smp.bounty.commands;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.data.BountyDataManager;
import com.smp.bounty.data.PlayerBounty;
import com.smp.bounty.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrackCommand implements CommandExecutor, TabCompleter {

    private final BountyPlugin plugin;

    public TrackCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player tracker)) {
            sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
            return true;
        }

        if (!tracker.hasPermission("bounty.track")) {
            tracker.sendMessage(ColorUtil.color("&cYou do not have permission to do that."));
            return true;
        }

        if (args.length != 1) {
            tracker.sendMessage(ColorUtil.color("&cUsage: /track <player>"));
            return true;
        }

        if (args[0].equalsIgnoreCase(tracker.getName())) {
            tracker.sendMessage(ColorUtil.color("&cYou cannot track yourself."));
            return true;
        }

        BountyDataManager data = plugin.getDataManager();

        Player onlineTarget = Bukkit.getPlayer(args[0]);
        UUID targetUuid;
        String targetName;

        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            PlayerBounty existing = data.findByName(args[0]);
            if (existing == null || !existing.hasLastLocation()) {
                tracker.sendMessage(ColorUtil.color("&cNo location data is available for that player yet."));
                return true;
            }
            targetUuid = existing.getUuid();
            targetName = existing.getName();
        }

        plugin.getTrackingManager().startTracking(tracker, targetUuid, targetName);
        tracker.sendMessage(ColorUtil.color("&aYou are now tracking &f" + targetName + "&a. Check your compass!"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
