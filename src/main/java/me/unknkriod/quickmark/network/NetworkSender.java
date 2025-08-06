package me.unknkriod.quickmark.network;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.serializers.MarkSerializer;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import me.unknkriod.quickmark.serializers.TeamSerializer;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;

public class NetworkSender {
    public static void sendPing(UUID targetPlayer) {
        String name = TeamManager.getPlayerName(targetPlayer);

        String message = "If you see this message, it means that you do not have the QuickMark mod installed";

        String command = "/tell " + name + " quickmark://" + message;

        sendCommandSilently(command);

        sendSecurePing(targetPlayer);
    }

    private static void sendSecurePing(UUID targetPlayer) {
        // Если игрок уже авторизован — не отправляем повторно
        if (NetworkReceiver.isPlayerAuthorized(targetPlayer)) return;

        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String secureMessage = "quickmark-auth://REQ:" + token;

        String command = "/tell " + name + " " + secureMessage;
        sendCommandSilently(command);
    }

    public static void sendSecurePingAck(UUID targetPlayer) {
        // Если игрок уже авторизован — не отправляем повторно
        if (NetworkReceiver.isPlayerAuthorized(targetPlayer)) return;

        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String secureMessage = "quickmark-auth://ACK:" + token;

        String command = "/tell " + name + " " + secureMessage;
        sendCommandSilently(command);
    }

    public static void sendMarkToTeam(Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = MarkSerializer.serializeMark(mark);

        // Отправляем всем участникам команды
        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            // Не отправляем себе
            if (member.getPlayerId().equals(mark.getPlayerId())) continue;
            if (member.getPlayerId().equals(client.player.getUuid())) continue;

            String command = "/tell " + member.getPlayerName() + " quickmark://" + encoded;
            sendCommandSilently(command);
        }
    }

    public static void sendRemoveCommand(UUID markId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = MarkSerializer.serializeRemoveCommand(markId);

        // Отправляем всем участникам команды
        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            if (member.getPlayerId().equals(client.player.getUuid())) continue;

            String command = "/tell " + member.getPlayerName() + " quickmark://" + encoded;
            sendCommandSilently(command);
        }
    }

    public static void sendInvitation(UUID targetPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String encoded = TeamSerializer.serializeInvitation(client.player.getUuid());
        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            String command = "/tell " + targetName + " quickmark://" + encoded;
            sendCommandSilently(command);
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: " + targetPlayerId);
        }
    }

    public static void sendInvitationResponse(UUID targetPlayerId, boolean accepted) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) return;

        String encoded = TeamSerializer.serializeInvitationResponse(client.player.getUuid(), accepted);
        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            String command = "/tell " + targetName + " quickmark://" + encoded;
            sendCommandSilently(command);
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: " + targetPlayerId);
        }
    }

    public static void sendTeamUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        UUID currentLeader = TeamManager.getLeaderId();

        // Если лидер не назначен, не отправляем обновление
        if (currentLeader == null) {
            Quickmark.LOGGER.warn("Cannot send team update: no leader assigned");
            return;
        }

        // Сериализуем весь состав команды
        String encoded = TeamSerializer.serializeTeamUpdate(
                TeamManager.getTeamMembers(),
                currentLeader
        );

        for (TeamPlayer member : TeamManager.getTeamMembers()) {

            String command = "/tell " + member.getPlayerName() + " quickmark://" + encoded;
            sendCommandSilently(command);
        }
    }

    public static void sendTeamJoinInfo(UUID joinedPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) return;

        String encoded = TeamSerializer.serializeTeamJoinInfo(joinedPlayerId);

        // Рассылаем всем участникам, кроме себя
        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            if (member.getPlayerId().equals(joinedPlayerId)) continue;

            String command = "/tell " + member.getPlayerName() + " quickmark://" + encoded;

            sendCommandSilently(command);
        }
    }

    private static void sendCommandSilently(String command) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            // Убираем первый слэш для команды sendCommand
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            client.player.networkHandler.sendChatCommand(command);
        }
    }
}