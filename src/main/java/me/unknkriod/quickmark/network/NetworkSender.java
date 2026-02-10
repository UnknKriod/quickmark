package me.unknkriod.quickmark.network;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.serializers.MarkSerializer;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import me.unknkriod.quickmark.serializers.TeamSerializer;
import me.unknkriod.quickmark.utils.Base85Encoder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Sends network messages to the server or team members via plugin channel (preferred) or chat whispers (fallback).
 * Handles plugin detection, secure pings for mod verification, and team/mark broadcasts.
 */
public class NetworkSender {
    private static final Identifier CHANNEL = Identifier.of("quickmark", "main");
    private static boolean serverHasPlugin = false;
    private static long lastPingTime = 0;
    private static final long PING_INTERVAL = 15000; // 15 seconds
    private static boolean isFirstPingLog = true;

    public static void reset() {
        serverHasPlugin = false;
        lastPingTime = 0;
        isFirstPingLog = true;
    }

    public static void setServerHasPlugin(boolean hasPlugin) {
        boolean changed = serverHasPlugin != hasPlugin;
        serverHasPlugin = hasPlugin;

        if (changed && isFirstPingLog) {
            isFirstPingLog = false;
            Quickmark.log("Server plugin status: " + (hasPlugin ? "DETECTED" : "NOT DETECTED"));
        }
    }

    public static boolean hasServerPlugin() {
        return serverHasPlugin;
    }

    public static void sendPeriodicPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !serverHasPlugin) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPingTime > PING_INTERVAL) {
            sendPing();
            lastPingTime = currentTime;
        }
    }

    /**
     * Sends a PING to detect if server has the Quickmark plugin.
     */
    public static void sendPing() {
        if (ClientPlayNetworking.canSend(CHANNEL)) {
            try {
                PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
                buf.writeBytes("PING".getBytes(StandardCharsets.UTF_8));
                ClientPlayNetworking.send(new QuickmarkPayload(buf));
            } catch (Exception e) {
                Quickmark.LOGGER.warn("Failed to send ping: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a ping to alert a target player (used before invitations).
     */
    public static void sendPing(UUID targetPlayer) {
        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        String message = "quickmark://If you see this message, it means that you do not have the QuickMark mod installed";
        sendPrivateMessage(name, message);

        sendSecurePing(targetPlayer);
    }

    /**
     * Sends a secure auth request to verify the target has the mod.
     */
    private static void sendSecurePing(UUID targetPlayer) {
        if (NetworkReceiver.isPlayerAuthorized(targetPlayer)) return;

        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String secureMessage = "quickmark-auth://REQ:" + token;

        sendPrivateMessage(name, secureMessage);
    }

    public static void sendSecurePingAck(UUID targetPlayer) {
        if (NetworkReceiver.isPlayerAuthorized(targetPlayer)) return;

        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        NetworkReceiver.authorizePlayer(targetPlayer);
        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String secureMessage = "quickmark-auth://ACK:" + token;

        sendPrivateMessage(name, secureMessage);
    }

    /**
     * Sends a mark to the team via plugin or whispers.
     */
    public static void sendMarkToTeam(Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = MarkSerializer.serializeMark(mark);

        if (serverHasPlugin) {
            PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
            buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
            ClientPlayNetworking.send(new QuickmarkPayload(buf));
        } else {
            // Fallback to chat
            for (TeamPlayer member : TeamManager.getTeamMembers()) {
                if (member.getPlayerId().equals(mark.getPlayerId())) continue;
                if (member.getPlayerId().equals(client.player.getUuid())) continue;

                sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
            }
        }
    }

    /**
     * Sends a mark removal command to the team.
     */
    public static void sendRemoveCommand(UUID markId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = MarkSerializer.serializeRemoveCommand(markId);

        if (serverHasPlugin) {
            PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
            buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
            ClientPlayNetworking.send(new QuickmarkPayload(buf));
        } else {
            // Fallback to chat
            for (TeamPlayer member : TeamManager.getTeamMembers()) {
                if (member.getPlayerId().equals(client.player.getUuid())) continue;
                sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
            }
        }
    }

    public static void sendInvitation(UUID targetPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!serverHasPlugin && !NetworkReceiver.isPlayerAuthorized(targetPlayerId)) {
            sendPing(targetPlayerId);
            return;
        }

        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            Quickmark.log("Sending invitation to " + targetName + " via " + (serverHasPlugin ? "plugin" : "chat"));

            if (serverHasPlugin) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                out.write('I');
                out.writeBytes(Base85Encoder.uuidToBytes(targetPlayerId));
                out.writeBytes(Base85Encoder.uuidToBytes(client.player.getUuid()));
                String encoded = Base85Encoder.encode(out.toByteArray());

                PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
                buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
                ClientPlayNetworking.send(new QuickmarkPayload(buf));
                Quickmark.LOGGER.debug("Sent invitation via plugin channel");
            } else {
                String encoded = TeamSerializer.serializeInvitation(client.player.getUuid());
                sendPrivateMessage(targetName, "quickmark://" + encoded);
                Quickmark.LOGGER.debug("Sent invitation via chat");
            }
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: {}", targetPlayerId);
        }
    }

    public static void sendInvitationResponse(UUID targetPlayerId, boolean accepted) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            Quickmark.log("Sending invitation response to " + targetName + " via " + (serverHasPlugin ? "plugin" : "chat"));

            if (serverHasPlugin) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                out.write('R');
                out.writeBytes(Base85Encoder.uuidToBytes(targetPlayerId));
                out.writeBytes(Base85Encoder.uuidToBytes(client.player.getUuid()));
                out.write(accepted ? 1 : 0);
                String encoded = Base85Encoder.encode(out.toByteArray());

                PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
                buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
                ClientPlayNetworking.send(new QuickmarkPayload(buf));
            } else {
                String encoded = TeamSerializer.serializeInvitationResponse(client.player.getUuid(), accepted);
                sendPrivateMessage(targetName, "quickmark://" + encoded);
            }
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: {}", targetPlayerId);
        }
    }

    /**
     * Broadcasts team update to team members.
     */
    public static void sendTeamUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        UUID currentLeader = TeamManager.getLeaderId();
        if (currentLeader == null) {
            Quickmark.LOGGER.warn("Cannot send team update: no leader assigned");
            return;
        }

        String encoded = TeamSerializer.serializeTeamUpdate(
                TeamManager.getTeamMembers(),
                currentLeader
        );

        if (serverHasPlugin) {
            PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
            buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
            ClientPlayNetworking.send(new QuickmarkPayload(buf));
        } else {
            for (TeamPlayer member : TeamManager.getTeamMembers()) {
                sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
            }
        }
    }

    public static void sendTeamJoinInfo(UUID joinedPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = TeamSerializer.serializeTeamJoinInfo(joinedPlayerId);

        if (serverHasPlugin) {
            PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.create());
            buf.writeBytes(encoded.getBytes(StandardCharsets.UTF_8));
            ClientPlayNetworking.send(new QuickmarkPayload(buf));
        } else {
            for (TeamPlayer member : TeamManager.getTeamMembers()) {
                if (member.getPlayerId().equals(joinedPlayerId)) continue;
                sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
            }
        }
    }

    /**
     * Detects available whisper command (/minecraft:tell, /minecraft:msg, /tell).
     */
    private static String getAvailableWhisperCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "tell";

        Map<CommandNode<ClientCommandSource>, String> commands = client.player.networkHandler.getCommandDispatcher().getSmartUsage(
                client.player.networkHandler.getCommandDispatcher().getRoot(),
                client.player.networkHandler.getCommandSource()
        );

        if (commands != null) {
            if (commands.keySet().stream().anyMatch(node -> node.getName().equals("minecraft:tell"))) {
                return "minecraft:tell";
            }
            if (commands.keySet().stream().anyMatch(node -> node.getName().equals("minecraft:msg"))) {
                return "minecraft:msg";
            }
        }

        return "tell";
    }

    private static void sendPrivateMessage(String targetName, String message) {
        String baseCommand = getAvailableWhisperCommand();
        String command = "/" + baseCommand + " " + targetName + " " + message;
        sendCommandSilently(command);
    }

    private static void sendCommandSilently(String command) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            client.player.networkHandler.sendChatCommand(command);
        }
    }

    public static GameProfile getGameProfileByName(String name) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return null;

        for (PlayerListEntry entry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().getName().equalsIgnoreCase(name)) {
                return entry.getProfile();
            }
        }
        return null;
    }
}