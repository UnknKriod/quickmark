package me.unknkriod.quickmark.mark;

import me.unknkriod.quickmark.SoundManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import me.unknkriod.quickmark.network.NetworkSender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages creation, removal, and expiration of marks per player.
 */
public class MarkManager {
    private static final Map<UUID, List<Mark>> playerMarks = new HashMap<>();
    private static final int MAX_DISTANCE = 256;
    private static final long DANGER_TTL = 10000; // 10 seconds

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            removeExpiredMarks();
        });
    }

    /**
     * Removes expired danger marks.
     */
    private static void removeExpiredMarks() {
        long currentTime = System.currentTimeMillis();
        for (List<Mark> marks : playerMarks.values()) {
            marks.removeIf(mark ->
                    mark.getType() == MarkType.DANGER &&
                            (currentTime - mark.getCreationTime()) > DANGER_TTL
            );
        }
    }

    public static void clearAllMarks() {
        playerMarks.clear();
    }

    /**
     * Creates a new mark at the raycasted block position if valid.
     */
    public static void createMark(MarkType type, UUID playerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d look = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(MAX_DISTANCE));
        BlockHitResult hitResult = client.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hitResult.getBlockPos();

        removeMarkOfType(playerId, type);

        Mark mark = new Mark(type, pos, playerId);
        playerMarks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(mark);

        if (type == MarkType.NORMAL) {
            SoundManager.playNormalPing(pos);
        } else {
            SoundManager.playDangerPing(pos);
        }

        NetworkSender.sendMarkToTeam(mark);
    }

    public static void removeMarkOfType(UUID playerId, MarkType type) {
        playerMarks.computeIfPresent(playerId, (id, marks) ->
                marks.stream().filter(m -> m.getType() != type).collect(Collectors.toList())
        );
    }

    /**
     * Adds a received mark, checking for expiration.
     */
    public static void addMark(Mark mark) {
        if (mark.getType() == MarkType.DANGER &&
                System.currentTimeMillis() - mark.getCreationTime() > 10000) {
            return;
        }

        removeMarkOfType(mark.getPlayerId(), mark.getType());
        playerMarks.computeIfAbsent(mark.getPlayerId(), k -> new ArrayList<>()).add(mark);

        if (!mark.isExpired()) {
            if (mark.getType() == MarkType.NORMAL) {
                SoundManager.playNormalPing(mark.getPosition());
            } else {
                SoundManager.playDangerPing(mark.getPosition());
            }
        }
    }

    public static List<Mark> getAllMarks() {
        return playerMarks.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Removes a mark by ID and spawns cloud particles at its position.
     */
    public static void removeMark(UUID markId) {
        for (List<Mark> marks : playerMarks.values()) {
            Iterator<Mark> iterator = marks.iterator();
            while (iterator.hasNext()) {
                Mark mark = iterator.next();
                if (mark.getId().equals(markId)) {
                    spawnRemovalParticles(mark.getPosition());
                    iterator.remove();
                    return;
                }
            }
        }
    }

    private static void spawnRemovalParticles(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 20; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;

            world.addParticleClient(ParticleTypes.CLOUD,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.05, 0);
        }
    }

    public static void removeOwnMark(MarkType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        UUID playerId = client.player.getUuid();
        playerMarks.computeIfPresent(playerId, (id, marks) ->
                marks.stream().filter(m -> m.getType() != type).collect(Collectors.toList())
        );
    }

    public static Optional<Mark> getMarkAtPosition(BlockPos pos) {
        return getAllMarks().stream()
                .filter(mark -> mark.getPosition().equals(pos))
                .findFirst();
    }
}