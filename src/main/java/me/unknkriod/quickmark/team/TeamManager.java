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
import net.minecraft.text.Text;
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
    private static final long OUTGOING_INVITATION_TIMEOUT = 10000; // 10 seconds

    private static class CacheEntry {
        String name;
        long timestamp;

        private CacheEntry(String name, long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }

    private static final Map<UUID, CacheEntry> playerNameCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30000; // 30 секунд

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
            playerNameCache.clear();
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

    public static void setLeaderQuietly(UUID newLeaderId) {
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
        String cachedName = getCachedName(playerId);
        if (cachedName != null) {
            return cachedName;
        }

        String name = findPlayerName(playerId);
        if (name != null) {
            cachePlayerName(playerId, name);
        }

        return name;
    }

    private static String getCachedName(UUID playerId) {
        CacheEntry entry = playerNameCache.get(playerId);

        if (entry == null) {
            return null;
        }

        boolean isCacheValid = System.currentTimeMillis() - entry.timestamp < CACHE_DURATION;
        return isCacheValid ? entry.name : null;
    }

    private static void cachePlayerName(UUID playerId, String name) {
        if (name == null) {
            return;
        }

        playerNameCache.put(playerId, new CacheEntry(name, System.currentTimeMillis()));
    }

    private static String findPlayerName(UUID playerId) {
        TeamPlayer player = getPlayerById(playerId);
        if (player != null) {
            return player.getPlayerName();
        }

        String onlineName = findOnlinePlayerName(playerId);
        if (onlineName != null) {
            return onlineName;
        }

        String invitationName = findInvitationName(playerId);
        if (invitationName != null) {
            return invitationName;
        }

        return null;
    }

    private static String findOnlinePlayerName(UUID playerId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return null;
        }

        return client.getNetworkHandler().getPlayerList().stream()
                .filter(entry -> playerId.equals(entry.getProfile().getId()))
                .map(entry -> entry.getProfile().getName())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String findInvitationName(UUID playerId) {
        String name = pendingOutgoingInvitations.get(playerId);
        return name != null ? name : pendingIncomingInvitations.get(playerId);
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
                        teamPlayer.setMaxHealth((int) playerEntity.getMaxHealth());
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

        // Check if player is online
        String targetName = getPlayerName(targetPlayerId);
        if (targetName == null) {
            player.sendMessage(Text.literal("§cPlayer not found or offline"), false);
            return;
        }

        // Check if inviting self
        if (selfId.equals(targetPlayerId)) {
            player.sendMessage(Text.literal("§cYou cannot invite yourself"), false);
            return;
        }

        // Check if sender is leader
        if (!isLeader(selfId)) {
            player.sendMessage(Text.literal("§cOnly the team leader can invite players"), false);
            return;
        }

        // Check if player is already in team
        if (getPlayerById(targetPlayerId) != null) {
            player.sendMessage(Text.literal("§cPlayer is already in your team"), false);
            return;
        }

        if (hasOutgoingInvitation(targetPlayerId)) {
            player.sendMessage(Text.literal("You have already sent an invitation to this player."), false);
            return;
        }

        if (hasIncomingInvitation(targetPlayerId)) {
            player.sendMessage(Text.literal("This player has already sent you an invitation. Please respond to it first."), false);
            return;
        }

        pendingOutgoingInvitations.put(targetPlayerId, targetName);
        outgoingInvitationTimestamps.put(targetPlayerId, System.currentTimeMillis());
        NetworkSender.sendInvitation(targetPlayerId);
        Quickmark.log("Invited: " + targetName);
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
        if (!pendingOutgoingInvitations.containsKey(playerId)) {
            return false;
        }

        long remainingTime = getRemainingTimeForOutgoingInvitation(playerId);
        if (remainingTime <= 0) {
            pendingOutgoingInvitations.remove(playerId);
            outgoingInvitationTimestamps.remove(playerId);
            return false;
        }

        return true;
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
     * Returns a map of outgoing invitations with the remaining time in milliseconds
     * @return Map<Player's UUID, Pair<player's name, remaining time>>
     */
    public static Map<UUID, Map.Entry<String, Long>> getOutgoingInvitationsWithTime() {
        Map<UUID, Map.Entry<String, Long>> result = new HashMap<>();

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, String> entry : pendingOutgoingInvitations.entrySet()) {
            UUID playerId = entry.getKey();
            String playerName = entry.getValue();

            Long sendTime = outgoingInvitationTimestamps.get(playerId);
            if (sendTime == null) continue;

            long elapsed = currentTime - sendTime;
            long remaining = OUTGOING_INVITATION_TIMEOUT - elapsed;

            if (remaining > 0) {
                result.put(playerId, new AbstractMap.SimpleEntry<>(playerName, remaining));
            } else {
                pendingOutgoingInvitations.remove(playerId);
                outgoingInvitationTimestamps.remove(playerId);
            }
        }

        return result;
    }

    /**
     * Passes leadership to the specified player (only if the current caller is the leader)
     * @param newLeaderId The UUID of the player we want to promote to the leader
     * @return String message to display (null if no message, empty string for success without message)
     */
    public static String promotePlayer(UUID newLeaderId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "Error: Client player is null";

        UUID currentLeader = leaderId;
        UUID selfId = client.player.getUuid();

        if (!selfId.equals(currentLeader)) {
            return "§cOnly the current leader can promote another player";
        }

        TeamPlayer target = getPlayerById(newLeaderId);
        if (target == null) {
            return "§cCannot promote: player not in team";
        }

        if (newLeaderId.equals(selfId)) {
            return "§cYou are already the leader";
        }

        setLeaderQuietly(newLeaderId);
        ensureLeaderFirst();
        NetworkSender.sendTeamUpdate();

        return "§aLeadership transferred to " + target.getPlayerName();
    }

    /**
     * Проверяет, истекло ли исходящее приглашение
     */
    public static boolean isOutgoingInvitationExpired(UUID playerId) {
        return getRemainingTimeForOutgoingInvitation(playerId) <= 0;
    }
}