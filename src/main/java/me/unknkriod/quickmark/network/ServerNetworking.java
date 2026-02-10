package me.unknkriod.quickmark.network;

import io.netty.buffer.Unpooled;
import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.network.QuickmarkPayload;
import me.unknkriod.quickmark.utils.Base85Encoder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side networking handler for Quickmark.
 * Receives C2S packets, detects mod presence via pings, and broadcasts/relays messages to authorized players.
 */
public class ServerNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger(Quickmark.MOD_ID);
    private static final Map<UUID, Long> playerLastPing = new HashMap<>();
    private static final Map<UUID, String> playerNames = new HashMap<>();

    public static void initialize() {
        NetworkingInit.registerPayloads();

        ServerPlayNetworking.registerGlobalReceiver(QuickmarkPayload.ID, (payload, context) -> {
            PacketByteBuf buf = payload.data();
            byte[] raw = new byte[buf.readableBytes()];
            buf.readBytes(raw);

            String payloadStr = new String(raw, StandardCharsets.UTF_8);

            ServerPlayerEntity player = context.player();
            UUID playerId = player.getUuid();

            playerNames.put(playerId, player.getName().getString());

            if ("PING".equals(payloadStr)) {
                playerLastPing.put(playerId, System.currentTimeMillis());

                PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer());
                responseBuf.writeBytes("PONG".getBytes(StandardCharsets.UTF_8));
                ServerPlayNetworking.send(player, new QuickmarkPayload(responseBuf));
                return;
            }

            byte[] binary;
            try {
                binary = Base85Encoder.decode(payloadStr);
            } catch (Exception e) {
                LOGGER.warn("[Quickmark] Invalid Base85 from {}: {}", player.getName().getString(), e.getMessage());
                return;
            }

            if (binary.length == 0) return;
            char type = (char) (binary[0] & 0xFF);

            LOGGER.debug("[Quickmark] Received type '{}' from {}", type, player.getName().getString());

            switch (type) {
                case 'I', 'R' -> handleTargetedMessage(binary, raw, player, player.getServer());
                case 'M', 'X', 'T', 'J' -> broadcast(player, raw, player.getServer());
                default -> LOGGER.warn("[Quickmark] Unknown type: {} (char: '{}') from {}",
                        (int)type, type, player.getName().getString());
            }
        });
    }

    public static void registerTickHandler() {
        // Periodic cleanup of old pings
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server.getTicks() % 120 == 0) { // Every 6 seconds
                long currentTime = System.currentTimeMillis();
                int before = playerLastPing.size();
                playerLastPing.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > 60000); // 60 seconds timeout
                int after = playerLastPing.size();
                if (before != after) {
                    LOGGER.debug("[Quickmark] Cleaned up {} inactive players", before - after);
                }
            }
        });
    }

    private static void handleTargetedMessage(byte[] data, byte[] raw, ServerPlayerEntity sender, MinecraftServer server) {
        if (data.length < 33) {
            LOGGER.warn("[Quickmark] Invalid targeted message length: {} from {}",
                    data.length, sender.getName().getString());
            return;
        }

        UUID targetId = Base85Encoder.bytesToUuid(data, 1);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);

        if (target == null) {
            LOGGER.warn("[Quickmark] Target player {} not found or offline", targetId);
            return;
        }

        boolean hasMod = hasQuickMark(target);
        if (!hasMod) {
            LOGGER.warn("[Quickmark] Target player {} doesn't have Quickmark mod installed",
                    target.getName().getString());
            return;
        }

        char type = (char) (data[0] & 0xFF);
        LOGGER.info("[Quickmark] Forwarding {} message from {} to {}",
                type == 'I' ? "invitation" : "response",
                sender.getName().getString(),
                target.getName().getString());

        PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
        buf.writeBytes(raw);
        ServerPlayNetworking.send(target, new QuickmarkPayload(buf));
    }

    /**
     * Broadcasts to all players with mod except sender.
     */
    private static void broadcast(ServerPlayerEntity sender, byte[] raw, MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!p.getUuid().equals(sender.getUuid()) && hasQuickMark(p)) {
                PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
                buf.writeBytes(raw);
                ServerPlayNetworking.send(p, new QuickmarkPayload(buf));
            }
        }
    }

    /**
     * Checks if player has sent a ping recently (has mod).
     */
    private static boolean hasQuickMark(ServerPlayerEntity player) {
        boolean hasMod = playerLastPing.containsKey(player.getUuid());
        if (hasMod) {
            Long lastPing = playerLastPing.get(player.getUuid());
            if (lastPing != null && System.currentTimeMillis() - lastPing > 60000) {
                playerLastPing.remove(player.getUuid());
                LOGGER.debug("[Quickmark] Player {} ping expired", player.getName().getString());
                return false;
            }
        }
        return hasMod;
    }

    public static String getPlayerName(UUID playerId) {
        return playerNames.get(playerId);
    }

    public static void cleanupPlayer(UUID playerId) {
        playerNames.remove(playerId);
        playerLastPing.remove(playerId);
    }
}