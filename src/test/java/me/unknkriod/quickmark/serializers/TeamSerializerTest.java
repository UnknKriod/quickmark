package me.unknkriod.quickmark.serializers;

import me.unknkriod.quickmark.team.TeamPlayer;
import me.unknkriod.quickmark.utils.Base85Encoder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamSerializerTest {

    private UUID testUuid() {
        return UUID.randomUUID();
    }

    private List<TeamPlayer> createTestPlayers(int count) {
        List<TeamPlayer> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new TeamPlayer(UUID.randomUUID(), "Player" + i));
        }
        return players;
    }

    // ---------- INVITE ----------

    @Test
    void testSerializeDeserializeInvite() {
        UUID sender = testUuid();
        String encoded = TeamSerializer.serializeInvitation(sender);
        UUID decoded = TeamSerializer.deserializeInvitation(encoded);

        assertNotNull(decoded);
        assertEquals(sender, decoded);
    }

    @Test
    void testInvalidInviteReturnsNull() {
        byte[] badData = Base85Encoder.uuidToBytes(UUID.randomUUID());
        badData[0] = 'A'; // Префикс не 'I'
        String encoded = Base85Encoder.encode(badData);

        assertNull(TeamSerializer.deserializeInvitation(encoded));
    }

    // ---------- RESPONSE ----------

    @Test
    void testSerializeDeserializeResponse() {
        UUID sender = testUuid();
        String encoded = TeamSerializer.serializeInvitationResponse(sender, true);
        TeamSerializer.InvitationResponse decoded = TeamSerializer.deserializeInvitationResponse(encoded);

        assertNotNull(decoded);
        assertEquals(sender, decoded.senderId);
        assertTrue(decoded.accepted);
    }

    @Test
    void testInvalidResponseReturnsNull() {
        byte[] badData = Base85Encoder.uuidToBytes(UUID.randomUUID());
        badData[0] = 'A'; // Префикс не 'R'
        String encoded = Base85Encoder.encode(badData);

        assertNull(TeamSerializer.deserializeInvitationResponse(encoded));
    }

    // ---------- TEAM UPDATE ----------

    @Test
    void testSerializeDeserializeTeamUpdate() {
        UUID leader = testUuid();
        List<TeamPlayer> players = createTestPlayers(5);

        String encoded = TeamSerializer.serializeTeamUpdate(players, leader);
        TeamSerializer.TeamData decoded = TeamSerializer.deserializeTeamUpdate(encoded);

        assertNotNull(decoded);
        assertEquals(leader, decoded.leaderId);
        assertEquals(players.size(), decoded.members.size());
        assertEquals(players.get(0).getPlayerName(), decoded.members.get(0).getPlayerName());
    }

    @Test
    void testInvalidTeamUpdateReturnsNull() {
        byte[] badData = Base85Encoder.uuidToBytes(UUID.randomUUID());
        badData[0] = 'A'; // Префикс не 'T'
        String encoded = Base85Encoder.encode(badData);

        assertNull(TeamSerializer.deserializeTeamUpdate(encoded));
    }
}
