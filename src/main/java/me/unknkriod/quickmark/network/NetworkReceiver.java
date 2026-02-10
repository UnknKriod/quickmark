package me.unknkriod.quickmark.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.SoundManager;
import me.unknkriod.quickmark.gui.overlay.InfoOverlayRenderer;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.serializers.MarkSerializer;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.serializers.TeamSerializer;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.utils.Base85Encoder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receives and processes network messages from plugin channel or filtered chat.
 * Handles marks, team updates, invitations, and auth.
 */
public class NetworkReceiver {
    private static final Pattern MARK_PATTERN =
            Pattern.compile("quickmark://([0-9A-Za-z.\\-+=^!/*?&<>()\\[\\]{}@%$#:]+)");

    private static final Pattern AUTH_PATTERN =
            Pattern.compile("quickmark-auth://(REQ|ACK):([0-9A-Za-z!#$%&()*+\\-;<=>?@^_`{|}~]+)");

    private static final long RESPONSE_TIMEOUT = 60000; // 1 minute

    private static class ProcessedResponse {
        UUID uuid;
        long timestamp;

        private ProcessedResponse(UUID senderUuid, long timestamp) {
            this.uuid = senderUuid;
            this.timestamp = timestamp;
        }
    }

    private static final Set<ProcessedResponse> processedResponses = new HashSet<>();
    private static final Set<String> processedAuthTokens = new HashSet<>();
    private static final Set<UUID> authorizedPlayers = new HashSet<>();
    private static final Map<UUID, String> pendingAuthTokens = new HashMap<>();

    private static long lastCleanupResponsesTime = 0;
    private static final int CLEANUP_RESPONSES_INTERVAL = 20000; // 20 seconds

    /**
     * Processes plugin channel messages.
     */
    public static void handlePluginMessage(PacketByteBuf buf) {
        byte[] raw = new byte[buf.readableBytes()];
        buf.readBytes(raw);

        String received = new String(raw, StandardCharsets.UTF_8);

        if ("PONG".equals(received)) {
            NetworkSender.setServerHasPlugin(true);
            return;
        }

        byte[] data;
        try {
            data = Base85Encoder.decode(received);
        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to decode Base85: " + e.getMessage());
            return;
        }

        if (data.length == 0) return;
        char type = (char) (data[0] & 0xFF);

        switch (type) {
            case 'M' -> handleMarkData(received);
            case 'X' -> {
                UUID markId = MarkSerializer.deserializeRemoveCommand(received);
                if (markId != null) MarkManager.removeMark(markId);
            }
            case 'T' -> handleTeamUpdateData(received);
            case 'J' -> {
                UUID joinedId = TeamSerializer.deserializeTeamJoinInfo(received);
                if (joinedId != null) handleTeamJoinInfo(joinedId);
            }
            case 'I' -> {
                if (data.length >= 33) {
                    UUID inviterId = Base85Encoder.bytesToUuid(data, 17); // 1 + 16 (target) + 16 (sender)
                    handleInvitationData(inviterId);
                }
            }
            case 'R' -> {
                if (data.length >= 34) {
                    UUID responderId = Base85Encoder.bytesToUuid(data, 17);
                    handleInvitationResponseData(responderId, received);
                }
            }
            default -> Quickmark.LOGGER.warn("Unknown plugin message type: " + type);
        }
    }

    public static void handleChatMessage(String rawMessage, GameProfile sender) {
        if (sender == null) {
            return;
        }

        Matcher matcher = MARK_PATTERN.matcher(rawMessage);
        String senderName = sender.getName();
        UUID senderId = sender.getId();

        // Check for secure ping
        Matcher authMatcher = AUTH_PATTERN.matcher(rawMessage);
        if (authMatcher.find()) {
            handleSecurePing(authMatcher, senderName, senderId);
            return;
        }

        if (matcher.find()) {
            String encoded = matcher.group(1);
            handleEncodedMessage(encoded, sender, senderName, senderId);
        }
    }

    private static void handleSecurePing(Matcher authMatcher, String senderName, UUID senderId) {
        String type = authMatcher.group(1);
        String token = authMatcher.group(2);

        if (processedAuthTokens.contains(token)) {
            return;
        }
        processedAuthTokens.add(token);

        // Добавляем проверку, чтобы не авторизовывать повторно
        if (!authorizedPlayers.contains(senderId)) {
            authorizedPlayers.add(senderId);
        }

        if ("REQ".equals(type)) {
            NetworkSender.sendSecurePingAck(senderId);
            Quickmark.LOGGER.info("Received REQ from " + senderName + ", sent ACK.");
        } else {
            Quickmark.LOGGER.info("Received ACK from " + senderName + ".");
        }
    }

    private static void handleEncodedMessage(String encoded, GameProfile sender, String senderName, UUID senderId) {
        // Handle team invitations
        UUID inviteSender = TeamSerializer.deserializeInvitation(encoded);
        if (inviteSender != null) {
            handleInvitationData(inviteSender);
            return;
        }

        // Handle invitation responses
        handleInvitationResponseData(senderId, encoded);

        // Handle mark removal
        UUID removeId = MarkSerializer.deserializeRemoveCommand(encoded);
        if (removeId != null) {
            MarkManager.removeMark(removeId);
            return;
        }

        // Handle team join info
        UUID joinedId = TeamSerializer.deserializeTeamJoinInfo(encoded);
        if (joinedId != null && !MinecraftClient.getInstance().player.getUuid().equals(joinedId)) {
            handleTeamJoinInfo(joinedId);
            return;
        }

        // Handle team update
        handleTeamUpdateData(encoded);

        // Handle mark
        handleMarkData(encoded);
    }

    private static void handleMarkData(String encoded) {
        Mark mark = MarkSerializer.deserializeMark(encoded);
        if (mark == null) return;

        if (mark.getType() == MarkType.DANGER &&
                System.currentTimeMillis() - mark.getCreationTime() > 10000) {
            mark.markExpired();
        }
        MarkManager.addMark(mark);
    }

    private static void handleInvitationData(UUID inviterId) {
        if (isSelf(inviterId)) return;

        String inviterName = TeamManager.getPlayerName(inviterId);

        if (inviterName != null) {
            TeamManager.addIncomingInvitation(inviterId, inviterName);
            SoundManager.playInviteSound();
        }
    }

    private static void handleInvitationResponseData(UUID responderId, String encoded) {
        TeamSerializer.InvitationResponse response = TeamSerializer.deserializeInvitationResponse(encoded);
        if (response != null && !processedResponses.contains(response.senderId)) {
            processedResponses.add(new ProcessedResponse(response.senderId, System.currentTimeMillis()));
            String responderName = TeamManager.getPlayerName(responderId);
            if (responderName != null) {
                if (response.accepted) {
                    Quickmark.log(responderName + " accepted your invitation");
                } else {
                    Quickmark.log(responderName + " declined your invitation");
                }
            }
            TeamManager.removeOutgoingInvitation(responderId);
        }
    }

    private static void handleTeamUpdateData(String encoded) {
        TeamSerializer.TeamData teamData = TeamSerializer.deserializeTeamUpdate(encoded);
        if (teamData != null) {
            TeamManager.setTeamMembers(teamData.members, teamData.leaderId);
        }
    }

    private static void handleTeamJoinInfo(UUID joinedId) {
        String playerName = TeamManager.getPlayerName(joinedId);
        if (playerName == null) playerName = "Player";

        InfoOverlayRenderer.INSTANCE.show(
                Text.translatableWithFallback("quickmark.info.invited.title", "Joining the team"),
                Text.translatableWithFallback("quickmark.info.invited.message", playerName + " joined the team", playerName),
                joinedId,
                playerName
        );
    }

    public static boolean isQuickMarkMessage(String message) {
        return MARK_PATTERN.matcher(message).find();
    }

    public static boolean isQuickMarkAuthMessage(String message) {
        return AUTH_PATTERN.matcher(message).find();
    }

    private static boolean isSelf(UUID inviteSender) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        return client.player.getUuid().equals(inviteSender);
    }

    public static boolean isPlayerAuthorized(UUID playerId) {
        return authorizedPlayers.contains(playerId);
    }

    public static void authorizePlayer(UUID playerId) {
        authorizedPlayers.add(playerId);
    }

    public static void clearAuthorizedPlayers() {
        authorizedPlayers.clear();
        processedAuthTokens.clear();
        pendingAuthTokens.clear();
    }

    public static void cleanupExpiredResponses() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupResponsesTime > CLEANUP_RESPONSES_INTERVAL) {
            processedResponses.removeIf(entry ->
                    currentTime - entry.timestamp > RESPONSE_TIMEOUT
            );
            lastCleanupResponsesTime = currentTime;
        }
    }
}