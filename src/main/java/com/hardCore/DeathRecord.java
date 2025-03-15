// DeathRecord.java
package com.hardCore;

public class DeathRecord {
    private String playerName;
    private int deathCount;
    private long lastDeathTime;

    public DeathRecord(String playerName) {
        this.playerName = playerName;
        this.deathCount = 0;
        this.lastDeathTime = 0;
    }

    public DeathRecord(String playerName, int deathCount, long lastDeathTime) {
        this.playerName = playerName;
        this.deathCount = deathCount;
        this.lastDeathTime = lastDeathTime;
    }

    public String getPlayerName() { return playerName; }
    public int getDeathCount() { return deathCount; }
    public long getLastDeathTime() { return lastDeathTime; }
    public void incrementDeathCount() {
        this.deathCount++;
        this.lastDeathTime = System.currentTimeMillis();
    }
}