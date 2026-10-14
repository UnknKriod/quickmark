package me.unknkriod.quickmark.gui.overlay;

import me.unknkriod.quickmark.team.TeamManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The invite overlay auto-declines when it closes, which tells the sender the invitation was
 * refused. It therefore has to stay open for the whole time the sender considers the invitation
 * pending; a shorter window made the outgoing card on the Invites tab vanish with seconds still on
 * its countdown.
 */
class InviteOverlayLifetimeTest {

    @Test
    void acceptWindowMatchesTheInvitationLifetime() {
        InviteOverlayRenderer overlay = new InviteOverlayRenderer();

        assertEquals(TeamManager.INVITATION_LIFETIME_MS, overlay.displayTime,
                "the overlay must not close before the invitation expires");
    }
}
