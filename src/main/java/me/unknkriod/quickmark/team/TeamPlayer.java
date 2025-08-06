package me.unknkriod.quickmark.team;

import java.util.UUID;

public class TeamPlayer {
    private final UUID playerId;
    private final String playerName;
    private float health;
    private float absorption;

    public TeamPlayer(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getHealth() {
        return health;
    }

    public float getAbsorption() {
        return absorption;
    }

    public void setAbsorption(float absorption) {
        this.absorption = absorption;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}