package com.hardCore;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class WorldResetPlugin extends JavaPlugin implements Listener {
    private DeathRecordManager deathRecordManager;
    @Override
    public void onEnable() {
        createStartBatIfNotExists();
        ReadSettings readSettings = new ReadSettings(this);
        readSettings.saveDefaultConfig();
        readSettings.reloadSettings();
        this.deathRecordManager = new DeathRecordManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("deaths").setExecutor(new Commands(deathRecordManager));
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(), this); // ポータルリスナーを登録
        getLogger().info("WorldResetPlugin が有効化されました！");
    }

    private void createStartBatIfNotExists() {
        // サーバーのルートディレクトリに start.bat を作成
        File startBat = new File(getServer().getWorldContainer().getParentFile(), "start.bat");
        if (!startBat.exists()) {
            try {
                String batContent = "@echo off\n" +
                        ":start\n" +
                        "java -Xms8G -Xmx8G -jar paper.jar nogui\n" +
                        "ping 127.0.0.1 -n 10 > nul\n" +
                        "goto start";

                FileUtils.writeStringToFile(startBat, batContent, "UTF-8");
                getLogger().info("start.batファイルをサーバーのルートディレクトリに作成しました。");
                getLogger().info("サーバーを起動するには start.bat を実行してください。");
                Runtime.getRuntime().halt(0); // 即時強制終了
            } catch (IOException e) {
                getLogger().warning("start.batファイルの作成に失敗: " + e.getMessage());
            }
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().hasPermission("worldresetplugin.reset")) {
            return;
        }

        Player player = event.getEntity();
        deathRecordManager.recordDeath(player.getName());
        if (PlayerPortalListener.isReset() || Bukkit.getOnlinePlayers().stream().allMatch(Entity::isDead)) {
            getLogger().info(event.getEntity().getName() + " が死亡。ワールドをリセットします...");
            event.getDrops().clear();
            resetWorldAsync("world");
        }
    }

    private void resetWorldAsync(String worldName) {
        // プレイヤーを全員キック
        kickAllPlayers();

        // メインスレッドで実行
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                // 全てのタスクをキャンセル
                Bukkit.getScheduler().cancelTasks(this);

                // データクリーンアップと削除を即時実行
                File worldContainer = Bukkit.getWorldContainer();

                // ワールドをアンロード（保存せず）
                unloadAllWorlds(worldName);

                // ワールドデータを削除
                deleteWorldFolder(new File(worldContainer, worldName));
                deleteWorldFolder(new File(worldContainer, worldName + "_nether"));
                deleteWorldFolder(new File(worldContainer, worldName + "_the_end"));

                // server.propertiesの更新
                saveCurrentLevelName(worldName);

                getLogger().info("ワールドを削除しました。サーバーを強制終了します...");
                Runtime.getRuntime().halt(0); // 即時強制終了

            } catch (Exception e) {
                getLogger().severe("リセット処理中にエラーが発生: " + e.getMessage());
                Runtime.getRuntime().halt(1);
            }
        });
    }

    private void kickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("§cワールドをリセットします。\n§e数分後に再接続してください。");
        }
    }

    private void unloadAllWorlds(String worldName) {
        Arrays.asList(worldName, worldName + "_nether", worldName + "_the_end")
                .forEach(name -> {
                    World world = Bukkit.getWorld(name);
                    if (world != null) {
                        // セーブせずにアンロード
                        Bukkit.unloadWorld(world, false);
                    }
                });
    }

    private void deleteWorldFolder(File worldFolder) throws IOException {
        if (worldFolder.exists()) {
            try {
                FileUtils.deleteDirectory(worldFolder);
                getLogger().info("ワールド " + worldFolder.getName() + " を削除しました。");
            } catch (IOException e) {
                getLogger().warning("ワールド " + worldFolder.getName() + " の削除中にエラー: " + e.getMessage());
                throw e;
            }
        }
    }

    private void saveCurrentLevelName(String worldName) {
        try {
            File serverProperties = new File("server.properties");
            if (serverProperties.exists()) {
                java.util.Properties props = new java.util.Properties();
                props.load(new java.io.FileInputStream(serverProperties));
                props.setProperty("level-name", worldName);

                // カスタムシード値の設定
                if (ReadSettings.isCustomSeed()) {
                    props.setProperty("level-seed", String.valueOf(ReadSettings.getSeed()));
                } else {
                    // ランダムなシード値を生成
                    long newSeed = new Random().nextLong();
                    props.setProperty("level-seed", String.valueOf(newSeed));
                    getLogger().info("新しいシード値を設定: " + newSeed);
                }

                props.store(new java.io.FileOutputStream(serverProperties), null);
            }
        } catch (IOException e) {
            getLogger().warning("server.propertiesの更新に失敗: " + e.getMessage());
        }
    }

}
