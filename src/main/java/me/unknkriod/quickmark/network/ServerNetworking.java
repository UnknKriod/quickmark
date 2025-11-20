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

    public static void initialize() {
        NetworkingInit.registerPayloads();

        ServerPlayNetworking.registerGlobalReceiver(QuickmarkPayload.ID, (payload, context) -> {
            PacketByteBuf buf = payload.data();
            byte[] raw = new byte[buf.readableBytes()];
            buf.readBytes(raw);

            String payloadStr = new String(raw, StandardCharsets.UTF_8);

            ServerPlayerEntity player = context.player();

            if ("PING".equals(payloadStr)) {
                playerLastPing.put(player.getUuid(), System.currentTimeMillis());
                PacketByteBuf responseBuf = new PacketByteBuf(Unpooled.buffer());
                responseBuf.writeBytes("PONG".getBytes(StandardCharsets.UTF_8));
                ServerPlayNetworking.send(player, new QuickmarkPayload(responseBuf));
                return;
            }

            byte[] binary;
            try {
                binary = Base85Encoder.decode(payloadStr);
            } catch (Exception e) {
                LOGGER.warn("Invalid Base85 from {}: {}", player.getName().getString(), e.getMessage());
                return;
            }

            if (binary.length == 0) return;
            char type = (char) (binary[0] & 0xFF);

            switch (type) {
                case 'I', 'R' -> handleTargetedMessage(binary, raw, player.getServer());
                case 'M', 'X', 'T', 'J' -> broadcast(player, raw, player.getServer());
                default -> LOGGER.warn("Unknown type: {} (char: '{}') from {}", (int)type, type, player.getName().getString());
            }
        });
    }

    public static void registerTickHandler() {
        // Periodic cleanup of old pings
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server.getTicks() % 120 == 0) { // Every 6 seconds
                long currentTime = System.currentTimeMillis();
                playerLastPing.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > 30000); // 30 seconds
            }
        });
    }

    private static void handleTargetedMessage(byte[] data, byte[] raw, MinecraftServer server) {
        if (data.length < 33) return;

        UUID targetId = Base85Encoder.bytesToUuid(data, 1);
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);

        if (target == null || !hasQuickMark(target)) return;

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
        return playerLastPing.containsKey(player.getUuid());
    }
}