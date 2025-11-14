package me.unknkriod.quickmark.team;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.gui.overlay.InviteOverlayRenderer;
import me.unknkriod.quickmark.gui.overlay.SuccessOverlayRenderer;
import me.unknkriod.quickmark.network.NetworkSender;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ColorHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {
    private static final List<TeamPlayer> teamMembers = new LinkedList<>();
    private static UUID leaderId;
    private static final Map<UUID, String> pendingInvitations = new ConcurrentHashMap<>();
    private static final Set<UUID> knownPlayers = new HashSet<>();

    public static void initialize() {
        // Ждём, пока появится игрок
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;

            if (getPlayerById(client.player.getUuid()) == null) {
                addPlayerQuietly(client.player.getUuid(), client.player.getName().getLiteralString());

                // Если команда состоит только из нас, назначаем себя лидером
                if (teamMembers.size() == 1 && leaderId == null) {
                    setLeaderQuietly(client.player.getUuid());
                }
            }

            // Текущие игроки
            Set<UUID> current = new HashSet<>();
            client.getNetworkHandler().getPlayerList().forEach(entry -> current.add(entry.getProfile().getId()));

            // Вышедшие
            for (UUID uuid : new HashSet<>(knownPlayers)) {
                if (!current.contains(uuid)) {
                    removePlayer(uuid);
                    knownPlayers.remove(uuid);
                }
            }

            // Новые
            for (UUID uuid : current) {
                if (!knownPlayers.contains(uuid)) {
                    knownPlayers.add(uuid);
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            knownPlayers.clear();
        });
    }

    public static void clearTeam() {
        teamMembers.clear();
        leaderId = null;
    }

    // Добавляет игрока без отправки обновления
    private static void addPlayerQuietly(UUID playerId, String playerName) {
        if (getPlayerById(playerId) != null) return;

        TeamPlayer newPlayer = new TeamPlayer(playerId, playerName);
        teamMembers.add(newPlayer);
    }

    public static void removePlayer(UUID playerId) {
        teamMembers.removeIf(player -> player.getPlayerId().equals(playerId));

        // Если удалили лидера - выбираем нового
        if (playerId.equals(leaderId)) {
            if (!teamMembers.isEmpty()) {
                setLeaderQuietly(teamMembers.getFirst().getPlayerId());
            } else {
                leaderId = null;
            }
        }
    }

    // Устанавливает лидера без отправки обновления
    private static void setLeaderQuietly(UUID newLeaderId) {
        leaderId = newLeaderId;
        ensureLeaderFirst();
        Quickmark.log("New team leader: " + getPlayerName(newLeaderId));
    }

    public static boolean isLeader(UUID playerId) {
        return playerId.equals(leaderId);
    }

    public static List<TeamPlayer> getTeamMembers() {
        return new ArrayList<>(teamMembers);
    }

    public static void setTeamMembers(List<TeamPlayer> members, UUID leaderId) {
        // Удаляем дубликаты
        Set<UUID> existingIds = new HashSet<>();
        List<TeamPlayer> uniqueMembers = new ArrayList<>();

        for (TeamPlayer member : members) {
            if (!existingIds.contains(member.getPlayerId())) {
                uniqueMembers.add(member);
                existingIds.add(member.getPlayerId());
            }
        }

        teamMembers.clear();
        teamMembers.addAll(uniqueMembers);
        TeamManager.leaderId = leaderId;
        ensureLeaderFirst();
    }

    public static boolean isPlayerInTeam(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;

        String searchName = playerName.toLowerCase();

        for (TeamPlayer member : teamMembers) {
            String memberName = member.getPlayerName();
            if (memberName != null && memberName.toLowerCase().equals(searchName)) {
                return true;
            }
        }
        return false;
    }

    public static TeamPlayer getPlayerById(UUID playerId) {
        for (TeamPlayer player : teamMembers) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public static String getPlayerName(UUID playerId) {
        TeamPlayer player = getPlayerById(playerId);
        if (player != null) return player.getPlayerName();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(playerId);
            if (entry != null) return entry.getProfile().getName();
        }

        return null;
    }

    public static void updateTeamHealth() {
        // Получаем список UUID всех игроков в команде
        Set<UUID> teamPlayerIds = new HashSet<>();
        for (TeamPlayer player : teamMembers) {
            teamPlayerIds.add(player.getPlayerId());
        }

        // Обновляем только игроков в команде
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            for (PlayerEntity playerEntity : client.world.getPlayers()) {
                UUID playerId = playerEntity.getUuid();
                if (teamPlayerIds.contains(playerId)) {
                    TeamPlayer teamPlayer = getPlayerById(playerId);
                    if (teamPlayer != null) {
                        teamPlayer.setHealth((int) playerEntity.getHealth());
                        teamPlayer.setAbsorption(playerEntity.getAbsorptionAmount());
                    }
                }
            }
        }
    }

    public static void sendInvitation(UUID targetPlayerId) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        UUID selfId = player.getUuid();

        // Защита от приглашения самого себя
        if (selfId.equals(targetPlayerId)) {
            Quickmark.LOGGER.warn("Нельзя пригласить самого себя");
            return;
        }

        // Проверка на лидерство
        if (!isLeader(selfId)) {
            Quickmark.LOGGER.warn("Только лидер может приглашать игроков");
            return;
        }

        pendingInvitations.put(targetPlayerId, player.getName().getLiteralString());
        NetworkSender.sendInvitation(targetPlayerId);
        Quickmark.log("Invited: " + player.getName().getLiteralString());
    }

    public static void addPendingInvitation(UUID senderId, String senderName) {
        // Проверяем, нет ли уже такого приглашения
        if (!pendingInvitations.containsKey(senderId)) {
            pendingInvitations.put(senderId, senderName);
            Quickmark.log("Added pending invitation: " + senderName);
            InviteOverlayRenderer.INSTANCE.show(senderId, senderName);
        }
    }

    public static void acceptInvitation(UUID senderId) {
        String senderName = pendingInvitations.remove(senderId);
        if (senderName != null) {
            NetworkSender.sendInvitationResponse(senderId, true);
            SuccessOverlayRenderer.INSTANCE.show(senderId, senderName);

            // Очищаем текущую команду
            clearTeam();

            // Добавляем отправителя как лидера (без отправки обновления)
            addPlayerQuietly(senderId, senderName);
            setLeaderQuietly(senderId);

            // Добавляем себя в команду (без отправки обновления)
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                addPlayerQuietly(player.getUuid(), player.getName().getLiteralString());
                NetworkSender.sendTeamJoinInfo(player.getUuid());
            }

            // Теперь отправляем одно обновление со всей командой
            NetworkSender.sendTeamUpdate();
        }
    }

    public static void clearPendingInvitations() {
        pendingInvitations.clear();
    }

    private static void ensureLeaderFirst() {
        if (leaderId == null) return;

        TeamPlayer leader = getPlayerById(leaderId);
        if (leader != null && !teamMembers.isEmpty() && !teamMembers.getFirst().getPlayerId().equals(leaderId)) {
            // Перемещаем лидера на первую позицию
            teamMembers.remove(leader);
            teamMembers.addFirst(leader);
        }
    }

    public static void declineInvitation(UUID senderId) {
        // Удаляем только одно приглашение
        if (pendingInvitations.containsKey(senderId)) {
            pendingInvitations.remove(senderId);
            NetworkSender.sendInvitationResponse(senderId, false);
        }
    }

    public static int getPlayerPosition(UUID playerId) {
        for (int i = 0; i < teamMembers.size(); i++) {
            if (teamMembers.get(i).getPlayerId().equals(playerId)) {
                return i;
            }
        }
        return -1;
    }

    public static int getColorForPosition(int position) {
        return switch (position) {
            case 0 -> ColorHelper.getArgb(255, 0, 120, 255);     // Синий (лидер)
            case 1 -> ColorHelper.getArgb(255, 255, 102, 255);   // Розовый
            case 2 -> ColorHelper.getArgb(255, 0, 255, 102);     // Зеленый
            case 3 -> ColorHelper.getArgb(255, 255, 255, 90);   // Желтый
            default -> ColorHelper.getArgb(255, 255, 165, 0);  // Оранжевый
        };
    }

    public static UUID getLeaderId() {
        return leaderId;
    }
}