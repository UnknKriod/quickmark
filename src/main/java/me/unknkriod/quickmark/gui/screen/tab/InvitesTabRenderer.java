package me.unknkriod.quickmark.gui.screen.tab;

import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.gui.screen.TeamScrollController;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

import static me.unknkriod.quickmark.gui.screen.TeamManagementScreen.renderEmptyState;
import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;
import static me.unknkriod.quickmark.gui.screen.TeamSidebarRenderer.*;
import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.*;
import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.drawCentredGlyph;

public class InvitesTabRenderer {

    /** Shared with the click handler, which measures the same button. */
    public static final String CANCEL_LABEL = "Cancel";

    /** Badges drawn on the bottom-right corner of each card's head. */
    private static final String INCOMING_BADGE = "👥";
    private static final String OUTGOING_BADGE = "📨";

    public static void render(
            DrawContext context,
            TextRenderer textRenderer,
            int x,
            int y,
            int mouseX,
            int mouseY,
            int panelWidth,
            int panelHeight,
            int sidebarWidth,
            int cardWidth,
            int cardHeight,
            int cardSpacing,
            TeamScrollController scrollController
    ) {
        Map<UUID, String> incoming = TeamManager.getIncomingInvitations();
        Map<UUID, Map.Entry<String, Long>> outgoing = TeamManager.getOutgoingInvitationsWithTime();

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            renderEmptyState(context, textRenderer, x, y, panelWidth, sidebarWidth, Tab.INVITES, "No pending invitations", "Invites will appear here");
            return;
        }

        int actualCardWidth = cardContentWidth(panelWidth, sidebarWidth, cardWidth);
        int panelRight = panelRight(x, sidebarWidth, panelWidth);
        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);

        // Total height of everything that will be drawn, section headers included, so the last
        // card can actually be scrolled into view.
        int contentHeight = scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        if (!incoming.isEmpty()) {
            contentHeight += scale(CONTENT_SECTION_SPACING)
                    + incoming.size() * (cardHeight + cardSpacing)
                    + scale(CONTENT_SECTION_LABEL_SPACING);
        }
        if (!outgoing.isEmpty()) {
            contentHeight += scale(CONTENT_SECTION_SPACING)
                    + outgoing.size() * (cardHeight + cardSpacing);
        }
        scrollController.setMaxScroll(Math.max(0, contentHeight - contentScrollHeight(panelHeight)));

        if (!incoming.isEmpty()) {
            context.drawText(textRenderer, "📥 Incoming",
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY, TEXT_SECONDARY, true);
            cardY += scale(CONTENT_SECTION_SPACING);

            for (Map.Entry<UUID, String> entry : incoming.entrySet()) {
                boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                        mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                        mouseY >= cardY && mouseY < cardY + cardHeight;

                renderIncomingInviteCard(context, textRenderer, entry.getKey(), entry.getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY, mouseX, mouseY,
                        actualCardWidth, cardHeight, panelRight, isHovered);
                cardY += cardHeight + cardSpacing;
            }
            cardY += scale(CONTENT_SECTION_LABEL_SPACING);
        }

        if (!outgoing.isEmpty()) {
            context.drawText(textRenderer, "📤 Outgoing",
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY, TEXT_SECONDARY, true);
            cardY += scale(CONTENT_SECTION_SPACING);

            for (Map.Entry<UUID, Map.Entry<String, Long>> entry : outgoing.entrySet()) {
                boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                        mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                        mouseY >= cardY && mouseY < cardY + cardHeight;

                renderOutgoingInviteCard(context, textRenderer, entry.getKey(),
                        entry.getValue().getKey(),
                        entry.getValue().getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY,
                        actualCardWidth, cardHeight, panelRight, isHovered);
                cardY += cardHeight + cardSpacing;
            }
        }
    }

    /**
     * Draws the player's head in the card's left column, sized to sit level with the text, plus the
     * card's icon centred on the right.
     * <p>
     * Both invite cards use this, so they stay identical.
     */
    private static void renderHeadAndIcon(DrawContext context, TextRenderer textRenderer, UUID playerId,
                                          String icon, int iconColor,
                                          int x, int y, int actualCardWidth, int cardHeight) {
        int headSize = scale(INVITE_CARD_HEAD_SIZE);
        int headX = x + scale(INVITE_CARD_HEAD_LEFT_PADDING);
        TeamHudRenderer.renderHead(context, playerId, headX, y + (cardHeight - headSize) / 2, headSize);

        // Icon on the right, centred in the card rather than on a text line.
        int iconX = x + inviteCardIconX(actualCardWidth, textRenderer.fontHeight);
        drawCentredGlyph(context, textRenderer, icon, iconX + textRenderer.fontHeight / 2,
                y + inviteCardCentredGlyphY(cardHeight, textRenderer.fontHeight), iconColor);
    }

    private static void renderIncomingInviteCard(DrawContext context, TextRenderer textRenderer, UUID senderId,
                                                 String senderName, int x, int y, int mouseX, int mouseY,
                                                 int actualCardWidth, int cardHeight, int panelRight,
                                                 boolean isHovered) {
        int cardColor = isHovered ? BG_CARD_HOVER : BG_CARD;
        drawRoundedRect(context, x, y, actualCardWidth, cardHeight,
                scale(CARD_BORDER_RADIUS), cardColor);

        // Centre the two lines in the card instead of pinning them to its top.
        int lineGap = scale(INVITE_CARD_LINE_GAP);
        int textTop = y + centeredTextBlockTop(cardHeight, textRenderer.fontHeight, lineGap);
        int subtitleTop = textTop + textRenderer.fontHeight + lineGap;

        renderHeadAndIcon(context, textRenderer, senderId, INCOMING_BADGE, ACCENT_PRIMARY,
                x, y, actualCardWidth, cardHeight);

        context.drawText(textRenderer, senderName + " invited you",
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                textTop, TEXT_PRIMARY, true);
        context.drawText(textRenderer, "Join their team",
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                subtitleTop, TEXT_SECONDARY, false);

        if (isHovered) {
            int btnSize = scale(INVITE_ACTION_BUTTON_SIZE);
            // Positions come from TeamScreenLayout, the same helpers the click handler uses.
            int btnTop = y + inviteActionButtonTop(cardHeight, textRenderer.fontHeight);

            int acceptX = x + acceptInviteButtonX(actualCardWidth);
            int declineX = x + declineInviteButtonX(actualCardWidth);

            drawIconButton(context, acceptX, btnTop, btnSize, "✓", ACCENT_SUCCESS, panelRight, textRenderer, scale(BUTTON_BORDER_RADIUS));
            if (mouseInRect(mouseX, mouseY, acceptX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Accept invitation"), mouseX, mouseY);
            }

            drawIconButton(context, declineX, btnTop, btnSize, "✕", ACCENT_DANGER, panelRight, textRenderer, scale(BUTTON_BORDER_RADIUS));
            if (mouseInRect(mouseX, mouseY, declineX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Decline invitation"), mouseX, mouseY);
            }
        }
    }

    private static void renderOutgoingInviteCard(DrawContext context, TextRenderer textRenderer, UUID targetId,
                                                 String targetName, long remainingTime, int x, int y,
                                                 int actualCardWidth, int cardHeight,
                                                 int panelRight, boolean isHovered) {
        int cardColor = isHovered ? BG_CARD_HOVER : BG_CARD;
        drawRoundedRect(context, x, y, actualCardWidth, cardHeight,
                scale(CARD_BORDER_RADIUS), cardColor);

        // Centre the two lines in the card instead of pinning them to its top.
        int lineGap = scale(INVITE_CARD_LINE_GAP);
        int textTop = y + centeredTextBlockTop(cardHeight, textRenderer.fontHeight, lineGap);
        int subtitleTop = textTop + textRenderer.fontHeight + lineGap;

        // The invited player's own head on the left, envelope on the right.
        renderHeadAndIcon(context, textRenderer, targetId, OUTGOING_BADGE, 0xFF_F59E0B,
                x, y, actualCardWidth, cardHeight);

        context.drawText(textRenderer, "Invited " + targetName,
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                textTop, TEXT_PRIMARY, true);

        long seconds = remainingTime / 1000;
        String timeText = String.format("Expires in %ds", seconds);
        context.drawText(textRenderer, timeText,
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                subtitleTop, TEXT_SECONDARY, false);

        // Width comes from the label so a small window cannot trim "Cancel"; the button is anchored
        // by its right edge and grows leftwards, staying inside the card.
        int cancelWidth = buttonWidth(INVITE_CARD_BUTTON_WIDTH, textRenderer.getWidth(CANCEL_LABEL));
        drawButton(context,
                x + cancelInviteButtonX(actualCardWidth, cancelWidth),
                y + (cardHeight - scale(INVITE_CARD_BUTTON_HEIGHT)) / 2,
                cancelWidth,
                scale(INVITE_CARD_BUTTON_HEIGHT),
                CANCEL_LABEL,
                ACCENT_DANGER,
                panelRight, textRenderer, scale(BUTTON_BORDER_RADIUS));
    }
}