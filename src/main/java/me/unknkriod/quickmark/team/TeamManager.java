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

/**
 * Manages team members, invitations, and leadership.
 * Handles adding/removing players, invitations, and health updates.
 */
public class TeamManager {
    private static final List<TeamPlayer> teamMembers = new LinkedList<>();
    private static UUID leaderId;

    private static final Map<UUID, String> pendingOutgoingInvitations = new ConcurrentHashMap<>();
    private static final Map<UUID, String> pendingIncomingInvitations = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> outgoingInvitationTimestamps = new ConcurrentHashMap<>();
    private static final long OUTGOING_INVITATION_TIMEOUT = 5000; // 5 seconds

    private static final Set<UUID> knownPlayers = new HashSet<>();

    /**
     * Initializes team management, including tick handlers for player tracking and health updates.
     */
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;

            if (getPlayerById(client.player.getUuid()) == null) {
                addPlayerQuietly(client.player.getUuid(), client.player.getName().getLiteralString());

                // Set self as leader if alone in team
                if (teamMembers.size() == 1 && leaderId == null) {
                    setLeaderQuietly(client.player.getUuid());
                }
            }

            // Track current online players
            Set<UUID> current = new HashSet<>();
            client.getNetworkHandler().getPlayerList().forEach(entry -> current.add(entry.getProfile().getId()));

            // Remove offline players
            for (UUID uuid : new HashSet<>(knownPlayers)) {
                if (!current.contains(uuid)) {
                    removePlayer(uuid);
                    knownPlayers.remove(uuid);
                }
            }

            // Add new online players to known set
            for (UUID uuid : current) {
                if (!knownPlayers.contains(uuid)) {
                    knownPlayers.add(uuid);
                }
            }

            checkOutgoingInvitationsTimeout();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            knownPlayers.clear();
            clearAllInvitations();
        });
    }

    /**
     * Checks and deletes expired outgoing invitations
     */
    private static void checkOutgoingInvitationsTimeout() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = outgoingInvitationTimestamps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerId = entry.getKey();
            long sendTime = entry.getValue();

            if (currentTime - sendTime > OUTGOING_INVITATION_TIMEOUT) {
                String playerName = pendingOutgoingInvitations.get(playerId);
                if (playerName != null) {
                    Quickmark.log("Outgoing invitation to " + playerName + " has timed out");
                }

                // Удаляем из обеих карт
                iterator.remove();
                pendingOutgoingInvitations.remove(playerId);
            }
        }
    }

    public static void clearTeam() {
        teamMembers.clear();
        leaderId = null;
    }

    public static void clearAllInvitations() {
        pendingOutgoingInvitations.clear();
        pendingIncomingInvitations.clear();
        outgoingInvitationTimestamps.clear();
    }

    private static void addPlayerQuietly(UUID playerId, String playerName) {
        if (getPlayerById(playerId) != null) return;

        TeamPlayer newPlayer = new TeamPlayer(playerId, playerName);
        teamMembers.add(newPlayer);
    }

    /**
     * Removes a player from the team and reassigns leader if necessary.
     */
    public static void removePlayer(UUID playerId) {
        teamMembers.removeIf(player -> player.getPlayerId().equals(playerId));

        // If you have deleted the leader, select a new one.
        if (playerId.equals(leaderId)) {
            if (!teamMembers.isEmpty()) {
                setLeaderQuietly(teamMembers.getFirst().getPlayerId());
            } else {
                leaderId = null;
            }
        }
    }

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

    /**
     * Sets the team members and leader, ensuring no duplicates.
     */
    public static void setTeamMembers(List<TeamPlayer> members, UUID leaderId) {
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

    /**
     * Retrieves player name from team or network handler if available.
     */
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

    /**
     * Updates health and absorption for all team members in the world.
     */
    public static void updateTeamHealth() {
        Set<UUID> teamPlayerIds = new HashSet<>();
        for (TeamPlayer player : teamMembers) {
            teamPlayerIds.add(player.getPlayerId());
        }

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

    /**
     * Sends an invitation to a player if the sender is the leader.
     */
    public static void sendInvitation(UUID targetPlayerId) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        UUID selfId = player.getUuid();

        if (selfId.equals(targetPlayerId)) {
            Quickmark.LOGGER.warn("You can't invite yourself.");
            return;
        }

        if (!isLeader(selfId)) {
            Quickmark.LOGGER.warn("Only the leader can invite players.");
            return;
        }

        if (pendingOutgoingInvitations.containsKey(targetPlayerId)) {
            Quickmark.LOGGER.warn("You have already sent an invitation to this player.");
            return;
        }

        if (pendingIncomingInvitations.containsKey(targetPlayerId)) {
            Quickmark.LOGGER.warn("This player has already sent you an invitation. Please respond to it first.");
            return;
        }

        String targetName = getPlayerName(targetPlayerId);
        if (targetName != null) {
            pendingOutgoingInvitations.put(targetPlayerId, targetName);
            outgoingInvitationTimestamps.put(targetPlayerId, System.currentTimeMillis());
            NetworkSender.sendInvitation(targetPlayerId);
            Quickmark.log("Invited: " + targetName);
        }
    }

    public static void addIncomingInvitation(UUID senderId, String senderName) {
        if (pendingIncomingInvitations.containsKey(senderId)) {
            Quickmark.log("Already have pending invitation from: " + senderName);
            return;
        }

        pendingIncomingInvitations.put(senderId, senderName);
        Quickmark.log("Received invitation from: " + senderName);
        InviteOverlayRenderer.INSTANCE.show(senderId, senderName);
    }

    /**
     * Accepts an invitation, joins the team, and updates via network.
     */
    public static void acceptInvitation(UUID senderId) {
        String senderName = pendingIncomingInvitations.remove(senderId);
        if (senderName != null) {
            NetworkSender.sendInvitationResponse(senderId, true);
            SuccessOverlayRenderer.INSTANCE.show(senderId, senderName);

            clearTeam();

            addPlayerQuietly(senderId, senderName);
            setLeaderQuietly(senderId);

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                addPlayerQuietly(player.getUuid(), player.getName().getLiteralString());
                NetworkSender.sendTeamJoinInfo(player.getUuid());
            }

            NetworkSender.sendTeamUpdate();

            // Clearing all outgoing invitations when accepting incoming
            pendingOutgoingInvitations.clear();
            outgoingInvitationTimestamps.clear();
        }
    }

    /**
     * Deletes the outgoing invitation (when we receive a reply)
     */
    public static void removeOutgoingInvitation(UUID targetPlayerId) {
        String targetName = pendingOutgoingInvitations.remove(targetPlayerId);
        outgoingInvitationTimestamps.remove(targetPlayerId);
        if (targetName != null) {
            Quickmark.log("Removed outgoing invitation to: " + targetName);
        }
    }

    /**
     * Ensures the leader is always first in the team list for rendering/order purposes.
     */
    private static void ensureLeaderFirst() {
        if (leaderId == null) return;

        TeamPlayer leader = getPlayerById(leaderId);
        if (leader != null && !teamMembers.isEmpty() && !teamMembers.getFirst().getPlayerId().equals(leaderId)) {
            teamMembers.remove(leader);
            teamMembers.addFirst(leader);
        }
    }

    public static void declineIncomingInvitation(UUID senderId) {
        String senderName = pendingIncomingInvitations.remove(senderId);
        if (senderName != null) {
            NetworkSender.sendInvitationResponse(senderId, false);
            Quickmark.log("Declined invitation from: " + senderName);
        }
    }

    public static void cancelOutgoingInvitation(UUID targetPlayerId) {
        String targetName = pendingOutgoingInvitations.remove(targetPlayerId);
        outgoingInvitationTimestamps.remove(targetPlayerId);
        if (targetName != null) {
            Quickmark.log("Cancelled invitation to: " + targetName);
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

    /**
     * Returns a color based on the player's position in the team (e.g., leader is blue).
     */
    public static int getColorForPosition(int position) {
        return switch (position) {
            case 0 -> ColorHelper.getArgb(255, 0, 120, 255);     // Blue (leader)
            case 1 -> ColorHelper.getArgb(255, 255, 102, 255);   // Pink
            case 2 -> ColorHelper.getArgb(255, 0, 255, 102);     // Green
            case 3 -> ColorHelper.getArgb(255, 255, 255, 90);   // Yellow
            default -> ColorHelper.getArgb(255, 255, 165, 0);  // Orange
        };
    }

    public static UUID getLeaderId() {
        return leaderId;
    }

    public static boolean hasOutgoingInvitation(UUID playerId) {
        return pendingOutgoingInvitations.containsKey(playerId);
    }

    public static boolean hasIncomingInvitation(UUID playerId) {
        return pendingIncomingInvitations.containsKey(playerId);
    }

    public static Map<UUID, String> getOutgoingInvitations() {
        return new HashMap<>(pendingOutgoingInvitations);
    }

    public static Map<UUID, String> getIncomingInvitations() {
        return new HashMap<>(pendingIncomingInvitations);
    }

    /**
     * Возвращает оставшееся время для исходящего приглашения в миллисекундах
     */
    public static long getRemainingTimeForOutgoingInvitation(UUID playerId) {
        Long sendTime = outgoingInvitationTimestamps.get(playerId);
        if (sendTime == null) return 0;

        long elapsed = System.currentTimeMillis() - sendTime;
        long remaining = OUTGOING_INVITATION_TIMEOUT - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Проверяет, истекло ли исходящее приглашение
     */
    public static boolean isOutgoingInvitationExpired(UUID playerId) {
        return getRemainingTimeForOutgoingInvitation(playerId) <= 0;
    }
}