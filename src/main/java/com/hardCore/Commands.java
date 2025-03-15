package com.hardCore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Commands implements CommandExecutor {
    private final DeathRecordManager deathRecordManager;
    private final SimpleDateFormat dateFormat;

    public Commands(DeathRecordManager deathRecordManager) {
        this.deathRecordManager = deathRecordManager;
        this.dateFormat = new SimpleDateFormat("MM/dd HH:mm");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("deaths")) {
            return false;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                showDeaths(sender, ((Player) sender).getName());
            } else {
                showAllDeaths(sender);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            showAllDeaths(sender);
        } else {
            showDeaths(sender, args[0]);
        }
        return true;
    }

    private void showDeaths(CommandSender sender, String playerName) {
        DeathRecord record = deathRecordManager.getAllRecords().get(playerName);
        if (record == null) {
            sender.sendMessage("§e" + playerName + "§fの死亡記録はありません");
            return;
        }

        String lastDeath = formatLastDeath(record.getLastDeathTime());
        sender.sendMessage("§e" + playerName + "§fの死亡記録");
        sender.sendMessage("§7死亡回数: §c" + record.getDeathCount() + "§7回");
        sender.sendMessage("§7最終死亡: §c" + lastDeath);
    }

    private void showAllDeaths(CommandSender sender) {
        Map<String, DeathRecord> records = deathRecordManager.getAllRecords();
        if (records.isEmpty()) {
            sender.sendMessage("§c死亡記録がありません");
            return;
        }

        sender.sendMessage("§e===== 全プレイヤーの死亡記録 =====");
        records.values().stream()
                .sorted((a, b) -> Integer.compare(b.getDeathCount(), a.getDeathCount()))
                .forEach(record -> {
                    String lastDeath = formatLastDeath(record.getLastDeathTime());
                    sender.sendMessage(String.format("§e%s§f: §c%d§7回 (最終: %s)",
                            record.getPlayerName(),
                            record.getDeathCount(),
                            lastDeath));
                });
    }

    private String formatLastDeath(long time) {
        return time == 0 ? "なし" : dateFormat.format(new Date(time));
    }
}