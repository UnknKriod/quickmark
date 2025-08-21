package me.unknkriod.quickmark.network;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.serializers.MarkSerializer;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import me.unknkriod.quickmark.serializers.TeamSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Map;
import java.util.UUID;

public class NetworkSender {
    public static void sendPing(UUID targetPlayer) {
        String name = TeamManager.getPlayerName(targetPlayer);
        if (name == null) return;

        String message = "quickmark://If you see this message, it means that you do not have the QuickMark mod installed";
        sendPrivateMessage(name, message);

        sendSecurePing(targetPlayer);
    }

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

        String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String secureMessage = "quickmark-auth://ACK:" + token;

        sendPrivateMessage(name, secureMessage);
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

            sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
        }
    }

    public static void sendRemoveCommand(UUID markId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String encoded = MarkSerializer.serializeRemoveCommand(markId);

        // Отправляем всем участникам команды
        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            if (member.getPlayerId().equals(client.player.getUuid())) continue;

            sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
        }
    }

    public static void sendInvitation(UUID targetPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String encoded = TeamSerializer.serializeInvitation(client.player.getUuid());
        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            sendPrivateMessage(targetName, "quickmark://" + encoded);
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: {}", targetPlayerId);
        }
    }

    public static void sendInvitationResponse(UUID targetPlayerId, boolean accepted) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) return;

        String encoded = TeamSerializer.serializeInvitationResponse(client.player.getUuid(), accepted);
        String targetName = TeamManager.getPlayerName(targetPlayerId);

        if (targetName != null) {
            sendPrivateMessage(targetName, "quickmark://" + encoded);
        } else {
            Quickmark.LOGGER.warn("Failed to get name for player ID: {}", targetPlayerId);
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

        String encoded = TeamSerializer.serializeTeamUpdate(
                TeamManager.getTeamMembers(),
                currentLeader
        );

        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
        }
    }

    public static void sendTeamJoinInfo(UUID joinedPlayerId) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) return;

        String encoded = TeamSerializer.serializeTeamJoinInfo(joinedPlayerId);

        // Рассылаем всем участникам, кроме себя
        for (TeamPlayer member : TeamManager.getTeamMembers()) {
            if (member.getPlayerId().equals(joinedPlayerId)) continue;
            sendPrivateMessage(member.getPlayerName(), "quickmark://" + encoded);
        }
    }

    /**
     * Определяет, какую команду whisper можно использовать.
     * Сначала проверяется /minecraft:tell, затем /minecraft:msg, затем /tell.
     */
    private static String getAvailableWhisperCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "tell";

        Map<CommandNode<ClientCommandSource>, String> commands = client.player.networkHandler.getCommandDispatcher().getSmartUsage(
                client.player.networkHandler.getCommandDispatcher().getRoot(),
                client.player.networkHandler.getCommandSource()
        );

        // проверяем наличие
        if (commands != null) {
            if (commands.keySet().stream().anyMatch(node -> node.getName().equals("minecraft:tell"))) {
                return "minecraft:tell";
            }
            if (commands.keySet().stream().anyMatch(node -> node.getName().equals("minecraft:msg"))) {
                return "minecraft:msg";
            }
        }

        // если не нашли — используем fallback
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
        return null; // если не нашли
    }
}