package me.unknkriod.quickmark.serializers;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.team.TeamPlayer;
import me.unknkriod.quickmark.utils.Base85Encoder;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serializes and deserializes team-related data (invitations, responses, updates) using Base85 encoding.
 */
public class TeamSerializer {

    /**
     * Serializes an invitation from a sender.
     */
    public static String serializeInvitation(UUID senderId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('I');
        out.writeBytes(Base85Encoder.uuidToBytes(senderId));
        return Base85Encoder.encode(out.toByteArray());
    }

    public static UUID deserializeInvitation(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'I') return null;
            return Base85Encoder.bytesToUuid(data, 1);
        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to deserialize invitation: " + e.getMessage());
            return null;
        }
    }

    /**
     * Serializes a response to an invitation.
     */
    public static String serializeInvitationResponse(UUID senderId, boolean accepted) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('R');
        out.writeBytes(Base85Encoder.uuidToBytes(senderId));
        out.write(accepted ? 1 : 0);
        return Base85Encoder.encode(out.toByteArray());
    }

    public static InvitationResponse deserializeInvitationResponse(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'R') return null;
            UUID senderId = Base85Encoder.bytesToUuid(data, 1);
            boolean accepted = data[17] == 1;
            return new InvitationResponse(senderId, accepted);
        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to deserialize invitation response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Serializes a team update with members and leader.
     * Player names are truncated to 31 bytes if necessary.
     */
    public static String serializeTeamUpdate(List<TeamPlayer> members, @NotNull UUID leaderId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('T');
        out.writeBytes(Base85Encoder.uuidToBytes(leaderId));
        out.write((byte) members.size());

        for (TeamPlayer member : members) {
            out.writeBytes(Base85Encoder.uuidToBytes(member.getPlayerId()));
            byte[] nameBytes = Base85Encoder.stringToBytes(member.getPlayerName());
            if (nameBytes.length > 31) {
                byte[] truncated = new byte[31];
                System.arraycopy(nameBytes, 0, truncated, 0, 31);
                nameBytes = truncated;
            }
            out.write((byte) nameBytes.length);
            out.writeBytes(nameBytes);
        }

        return Base85Encoder.encode(out.toByteArray());
    }

    public static TeamData deserializeTeamUpdate(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'T') return null;

            int pos = 1;
            UUID leaderId = Base85Encoder.bytesToUuid(data, pos);
            pos += 16;

            int count = data[pos++] & 0xFF;
            List<TeamPlayer> members = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                UUID id = Base85Encoder.bytesToUuid(data, pos);
                pos += 16;

                int nameLen = data[pos++] & 0xFF;
                String name = Base85Encoder.bytesToString(data, pos, nameLen);
                pos += nameLen;

                members.add(new TeamPlayer(id, name));
            }

            return new TeamData(members, leaderId);
        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to deserialize team update: " + e.getMessage());
            return null;
        }
    }

    /**
     * Serializes info about a player joining the team.
     */
    public static String serializeTeamJoinInfo(UUID joinedId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('J'); // J = Join
        out.writeBytes(Base85Encoder.uuidToBytes(joinedId));
        return Base85Encoder.encode(out.toByteArray());
    }

    public static UUID deserializeTeamJoinInfo(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'J') return null;
            return Base85Encoder.bytesToUuid(data, 1);
        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to deserialize team join info: " + e.getMessage());
            return null;
        }
    }

    // -------------------- Data Classes --------------------

    public static class InvitationResponse {
        public final UUID senderId;
        public final boolean accepted;

        public InvitationResponse(UUID senderId, boolean accepted) {
            this.senderId = senderId;
            this.accepted = accepted;
        }
    }

    public static class TeamData {
        public final List<TeamPlayer> members;
        public final UUID leaderId;

        public TeamData(List<TeamPlayer> members, UUID leaderId) {
            this.members = members;
            this.leaderId = leaderId;
        }
    }
}