package com.hardCore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ReadSettings {
    private final JavaPlugin plugin;
    private static boolean noresetOnEndDeath = true;
    private static boolean customSeed = true;
    private static long seed = 123456789L;

    public ReadSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    public  void reloadSettings() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        noresetOnEndDeath = config.getBoolean("reset_on_end_reach", true);
        plugin.getLogger().info("設定を読み込みました: reset_on_end_reach = " + noresetOnEndDeath);
        customSeed = config.getBoolean("custom_seed", true);
        plugin.getLogger().info("設定を読み込みました: custom_seed = " + customSeed);
        seed = config.getLong("seed", 123456789L);
        plugin.getLogger().info("設定を読み込みました: seed = " + seed);
    }

    public static boolean isNoresetOnEndDeath() {
        return noresetOnEndDeath;
    }

    public static boolean isCustomSeed() {
        return customSeed;
    }

    public static long getSeed() {
        return seed;
    }
}