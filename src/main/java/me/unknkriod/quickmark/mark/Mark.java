package me.unknkriod.quickmark.mark;

import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public class Mark {
    private final UUID id;
    private final MarkType type;
    private final BlockPos position;
    private final UUID playerId;
    private final long creationTime;

    public Mark(MarkType type, BlockPos position, UUID playerId, long creationTime) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.position = position;
        this.playerId = playerId;
        this.creationTime = creationTime;
    }

    public Mark(MarkType type, BlockPos position, UUID id, UUID playerId, long creationTime) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.playerId = playerId;
        this.creationTime = creationTime;
    }

    public Mark(MarkType type, BlockPos position, UUID playerId) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.position = position;
        this.playerId = playerId;
        this.creationTime = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    private boolean expired = false;

    public void markExpired() {
        this.expired = true;
    }

    public boolean isExpired() {
        return expired;
    }

    public MarkType getType() {
        return type;
    }

    public BlockPos getPosition() {
        return position;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getCreationTime() {
        return creationTime;
    }
}