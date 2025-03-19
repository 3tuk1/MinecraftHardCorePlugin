package com.hardCore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import java.util.Objects;
import java.util.Random;

public class WorldResetPlugin extends JavaPlugin implements Listener {
    private DeathRecordManager deathRecordManager;
    @Override
    public void onEnable() {
        ReadSettings readSettings = new ReadSettings(this);
        readSettings.saveDefaultConfig();
        readSettings.reloadSettings();
        this.deathRecordManager = new DeathRecordManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("deaths").setExecutor(new Commands(deathRecordManager));
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(), this); // ポータルリスナーを登録
        getLogger().info("WorldResetPlugin が有効化されました！");
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().hasPermission("worldresetplugin.reset")) {
            return;
        }

        Player player = event.getEntity();
        String deathMessage = event.getDeathMessage();
        deathRecordManager.recordDeath(player.getName());
        //if (PlayerPortalListener.isReset() || Bukkit.getOnlinePlayers().stream().allMatch(Entity::isDead)) {
        if(PlayerPortalListener.isReset()){
            getLogger().info(event.getEntity().getName() + " が死亡。ワールドをリセットします...");
            resetWorldAsync("world",deathMessage);
        }else if(Bukkit.getOnlinePlayers().stream().allMatch(players -> players.isDead() || players.getGameMode() == GameMode.SPECTATOR)){
            getLogger().info("全プレイヤーがスペクテイターモードか死亡しています。ワールドをリセットします...");
            resetWorldAsync("world", deathMessage);
        }
    }

    private void resetWorldAsync(String worldName,String deathMessage) {
        // プレイヤーを全員キック
        kickAllPlayers(deathMessage);

        // メインスレッドで実行
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                if(ReadSettings.isWorldBackup()){
                    Arrays.asList(worldName, worldName + "_nether", worldName + "_the_end")
                        .forEach(name -> {
                            World world = Bukkit.getWorld(name);
                            if (world != null) {
                                // ワールドのセーブ
                                world.save();
                            }
                        });
                }

                // 全てのタスクをキャンセル
                Bukkit.getScheduler().cancelTasks(this);

                // ワールドデータをバックアップ
                if(ReadSettings.isWorldBackup()){
                    World world = Bukkit.getWorld(worldName);
                    long seed = Objects.requireNonNull(world).getSeed();
                    worldBackup(worldName,seed);
                    worldBackup(worldName + "_nether",  seed);
                    worldBackup(worldName + "_the_end", seed);
                }

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

    private void worldBackup(String worldName, long seed) {

        File worldContainer = Bukkit.getWorldContainer();
        // もし　同じディレクトリ名があったら、それを削除して新しいバックアップを作成
        File backupDir = new File(worldContainer, "world_backups/" + seed);
        File backupDirworld = new File(worldContainer, "world_backups/" + seed + "/" + worldName);
        if (backupDirworld.exists()) {
            try {
                FileUtils.deleteDirectory(backupDirworld);
                getLogger().info("既存のバックアップディレクトリを削除しました: " + backupDirworld.getName());
            } catch (IOException e) {
                getLogger().warning("既存のバックアップディレクトリの削除に失敗: " + backupDirworld.getName());
            }
        }
        if (backupDir.mkdir()) {
            getLogger().info("バックアップディレクトリを作成しました: " + backupDir.getName());
        }

        File worldBackup = new File(backupDir, worldName);
        try {
            // session.lock ファイルを除外してディレクトリをコピー
            FileUtils.copyDirectory(new File(worldContainer, worldName), worldBackup,
                    new NotFileFilter(new NameFileFilter("session.lock")));
            getLogger().info("ワールドのバックアップを作成しました: " + worldBackup.getName());
        } catch (IOException e) {
            getLogger().warning("ワールドのバックアップ作成中にエラー: " + e.getMessage());
        }
    }

    private void kickAllPlayers(String deathMessage) {
        for (Player player : Bukkit.getOnlinePlayers()) {

            player.kickPlayer("§c" + deathMessage + "\n§eワールドをリセットします。\n§e数分後に再接続してください。");
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
