package com.smp.bounty.commands;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.data.BountyDataManager;
import com.smp.bounty.data.PlayerBounty;
import com.smp.bounty.gui.LeaderboardGUI;
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

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountyPlugin plugin;

    public BountyCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        BountyDataManager data = plugin.getDataManager();
        String prefix = plugin.getConfigManager().getPrefix();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtil.color(prefix + "&cConsole must specify a player: /bounty <player>"));
                return true;
            }
            PlayerBounty pb = data.getOrCreate(player);
            sender.sendMessage(ColorUtil.color(prefix + "&7Your bounty: &e" + pb.getBounty()));
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("gui")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.color(prefix + "&cOnly players can open the GUI leaderboard."));
                    return true;
                }
                if (!plugin.getConfigManager().isLeaderboardGuiEnabled()) {
                    sender.sendMessage(ColorUtil.color(prefix + "&cThe GUI leaderboard is disabled."));
                    return true;
                }
                LeaderboardGUI.open(plugin, player);
                return true;
            }

            List<PlayerBounty> top = data.getTop(10);
            sender.sendMessage(ColorUtil.color("&6&l--- Top Bounties ---"));
            if (top.isEmpty()) {
                sender.sendMessage(ColorUtil.color("&7No bounty data yet."));
                return true;
            }
            int rank = 1;
            for (PlayerBounty pb : top) {
                sender.sendMessage(ColorUtil.color("&e#" + rank + " &f" + pb.getName() + " &7- &6" + pb.getBounty()));
                rank++;
            }
            return true;
        }

        // /bounty <player>
        UUID targetUuid = data.resolveUuid(args[0]);
        if (targetUuid == null) {
            sender.sendMessage(ColorUtil.color(prefix + "&cThat player has no bounty data."));
            return true;
        }
        PlayerBounty pb = data.get(targetUuid);
        long bounty = pb != null ? pb.getBounty() : 0L;
        String displayName = pb != null ? pb.getName() : args[0];
        sender.sendMessage(ColorUtil.color(prefix + "&7" + displayName + "'s bounty: &e" + bounty));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("top");
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            String input = args[0].toLowerCase();
            return options.stream().filter(o -> o.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return List.of("gui").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
