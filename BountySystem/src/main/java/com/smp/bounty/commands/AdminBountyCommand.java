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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminBountyCommand implements CommandExecutor, TabCompleter {

    private final BountyPlugin plugin;

    public AdminBountyCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bounty.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission to do that."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ColorUtil.color("&cUsage: /adminbounty <set|add> <player> <value>"));
            return true;
        }

        String action = args[0].toLowerCase();
        if (!action.equals("set") && !action.equals("add")) {
            sender.sendMessage(ColorUtil.color("&cUsage: /adminbounty <set|add> <player> <value>"));
            return true;
        }

        BountyDataManager data = plugin.getDataManager();

        // Resolve online first; if never seen by the bounty system either, create a fresh
        // record using the typed name only when the target is currently online (so the UUID
        // is guaranteed correct - we never trust a name-derived UUID for offline players).
        Player onlineTarget = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            PlayerBounty existing = data.findByName(args[1]);
            if (existing == null) {
                sender.sendMessage(ColorUtil.color("&cThat player has no bounty data and is not online."));
                return true;
            }
            targetUuid = existing.getUuid();
            targetName = existing.getName();
        }

        long value;
        try {
            value = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.color("&cValue must be a whole number."));
            return true;
        }

        PlayerBounty pb = data.getOrCreate(targetUuid, targetName);

        if (action.equals("set")) {
            pb.setBounty(value);
            sender.sendMessage(ColorUtil.color("&aSet " + targetName + "'s bounty to &e" + pb.getBounty()));
        } else {
            pb.addBounty(value);
            sender.sendMessage(ColorUtil.color("&aAdded " + value + " to " + targetName +
                    "'s bounty. New total: &e" + pb.getBounty()));
        }

        if (onlineTarget != null) {
            onlineTarget.sendMessage(ColorUtil.color("&7Your bounty has been updated by an admin. New value: &e" + pb.getBounty()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("set", "add").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
