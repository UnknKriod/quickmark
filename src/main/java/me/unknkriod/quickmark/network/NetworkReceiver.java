package me.unknkriod.quickmark.network;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkReceiver {
    private static final Pattern MARK_PATTERN =
            Pattern.compile("quickmark://([0-9A-Za-z.\\-+=^!/*?&<>()\\[\\]{}@%$#:]+)");

    private static final Pattern AUTH_PATTERN =
            Pattern.compile("quickmark-auth://(REQ|ACK):([0-9A-Za-z!#$%&()*+\\-;<=>?@^_`{|}~]+)");

    private static final Set<UUID> processedResponses = new HashSet<>();
    private static final Set<String> processedAuthTokens = new HashSet<>();
    private static final Set<UUID> authorizedPlayers = new HashSet<>();

    public static void handleChatMessage(String rawMessage, GameProfile sender) {
        if (sender == null) {
            return;
        }

        Matcher matcher = MARK_PATTERN.matcher(rawMessage);
        String senderName = sender.getName();
        UUID senderId = sender.getId();

        // Проверка secure-пинга
        Matcher authMatcher = AUTH_PATTERN.matcher(rawMessage);
        if (authMatcher.find()) {
            String type = authMatcher.group(1); // "REQ" или "ACK"
            String token = authMatcher.group(2); // сам токен

            // Проверяем, не обрабатывали ли этот токен
            if (processedAuthTokens.contains(token)) {
                return;
            }
            processedAuthTokens.add(token);

            authorizedPlayers.add(senderId);

            if ("REQ".equals(type)) {
                NetworkSender.sendSecurePingAck(senderId);
                Quickmark.LOGGER.info("Received REQ from " + senderName + ", sent ACK.");
            } else {
                Quickmark.LOGGER.info("Received ACK from " + senderName + ".");
            }
            return;
        }

        if (matcher.find()) {
            String encoded = matcher.group(1);

            // Обработка приглашений в команду
            UUID inviteSender = TeamSerializer.deserializeInvitation(encoded);
            if (inviteSender != null) {
                // Проверяем, что приглашение пришло не от нас самих
                if (isSelf(inviteSender)) {
                    return;
                }

                TeamManager.addPendingInvitation(inviteSender, senderName);
                SoundManager.playInviteSound();
                return;
            }

            // Обработка ответов на приглашения
            TeamSerializer.InvitationResponse response = TeamSerializer.deserializeInvitationResponse(encoded);
            if (response != null) {
                // Проверяем, не обрабатывали ли мы уже этот ответ
                if (!processedResponses.contains(response.senderId)) {
                    processedResponses.add(response.senderId);

                    if (response.accepted) {
                        Quickmark.LOGGER.info(senderName + " accepted your invitation");
                    } else {
                        Quickmark.LOGGER.info(senderName + " declined your invitation");
                    }
                }
                return;
            }

            // Попытка обработать как удаление
            UUID removeId = MarkSerializer.deserializeRemoveCommand(encoded);
            if (removeId != null) {
                MarkManager.removeMark(removeId);
                return;
            }

            UUID joinedId = TeamSerializer.deserializeTeamJoinInfo(encoded);
            if (joinedId != null && !MinecraftClient.getInstance().player.getUuid().equals(joinedId)) {
                String playerName = TeamManager.getPlayerName(joinedId);
                if (playerName == null) playerName = "Player";

                InfoOverlayRenderer.INSTANCE.show(
                        Text.translatableWithFallback("quickmark.info.invited.title", "Joining the team"),
                        Text.translatableWithFallback("quickmark.info.invited.message", playerName + " joined the team", playerName),
                        joinedId,
                        playerName
                );
                return;
            }

            // Попытка обработать обновление состава команды
            TeamSerializer.TeamData teamData = TeamSerializer.deserializeTeamUpdate(encoded);
            if (teamData != null) {
                // Устанавливаем состав команды И лидера
                TeamManager.setTeamMembers(teamData.members, teamData.leaderId);
                return;
            }

            // Обработка обычной метки
            Mark mark = MarkSerializer.deserializeMark(encoded);
            if (mark != null) {
                // Проверка TTL перед добавлением
                if (mark.getType() == MarkType.DANGER &&
                        System.currentTimeMillis() - mark.getCreationTime() > 10000) {
                    // Помечаем как просроченную вместо добавления
                    mark.markExpired();
                }
                MarkManager.addMark(mark);
            }
        }
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

        // Проверяем совпадение UUID текущего игрока и отправителя приглашения
        return client.player.getUuid().equals(inviteSender);
    }

    public static boolean isPlayerAuthorized(UUID playerId) {
        return authorizedPlayers.contains(playerId);
    }

    public static void clearAuthorizedPlayers() {
        authorizedPlayers.clear();
    }
}