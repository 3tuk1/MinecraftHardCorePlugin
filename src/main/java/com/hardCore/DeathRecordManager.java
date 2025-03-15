// DeathRecordManager.java
package com.hardCore;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DeathRecordManager {
    private final JavaPlugin plugin;
    private final File recordFile;
    private final Map<String, DeathRecord> deathRecords;

    public DeathRecordManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recordFile = new File(plugin.getDataFolder(), "death_records.yml");
        this.deathRecords = new HashMap<>();
        loadRecords();
    }

    public void recordDeath(String playerName) {
        DeathRecord record = deathRecords.computeIfAbsent(playerName, DeathRecord::new);
        record.incrementDeathCount();
        saveRecords();
    }

    private void loadRecords() {
        if (!recordFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(recordFile);
        for (String playerName : config.getKeys(false)) {
            int deaths = config.getInt(playerName + ".deaths");
            long lastDeath = config.getLong(playerName + ".lastDeath");
            deathRecords.put(playerName, new DeathRecord(playerName, deaths, lastDeath));
        }
    }

    private void saveRecords() {
        YamlConfiguration config = new YamlConfiguration();
        for (DeathRecord record : deathRecords.values()) {
            String path = record.getPlayerName();
            config.set(path + ".deaths", record.getDeathCount());
            config.set(path + ".lastDeath", record.getLastDeathTime());
        }

        try {
            config.save(recordFile);
        } catch (IOException e) {
            plugin.getLogger().warning("死亡記録の保存に失敗: " + e.getMessage());
        }
    }

    public Map<String, DeathRecord> getAllRecords() {
        return new HashMap<>(deathRecords);
    }


}
