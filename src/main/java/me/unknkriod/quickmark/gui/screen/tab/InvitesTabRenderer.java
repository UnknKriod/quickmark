package me.unknkriod.quickmark.gui.screen.tab;

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

public class InvitesTabRenderer {

    public static void render(
            DrawContext context,
            TextRenderer textRenderer,
            int x,
            int y,
            int mouseX,
            int mouseY,
            int width,
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

        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        scrollController.setMaxScroll(Math.max(0, (incoming.size() + outgoing.size()) * (cardHeight + cardSpacing) -
                (panelHeight - scale(98))));

        if (!incoming.isEmpty()) {
            context.drawText(textRenderer, "📥 Incoming",
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY, TEXT_SECONDARY, true);
            cardY += scale(CONTENT_SECTION_SPACING);

            for (Map.Entry<UUID, String> entry : incoming.entrySet()) {
                boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                        mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                        mouseY >= cardY && mouseY < cardY + cardHeight;

                renderIncomingInviteCard(context, textRenderer, entry.getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY, mouseX, mouseY, width, cardWidth, cardHeight, panelWidth, isHovered);
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
                        mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                        mouseY >= cardY && mouseY < cardY + cardHeight;

                renderOutgoingInviteCard(context, textRenderer, entry.getValue().getKey(),
                        entry.getValue().getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY, width, cardWidth, cardHeight, panelWidth, isHovered);
                cardY += cardHeight + cardSpacing;
            }
        }
    }

    private static void renderIncomingInviteCard(DrawContext context, TextRenderer textRenderer, String senderName,
                                                 int x, int y, int mouseX, int mouseY,
                                                 int width, int cardWidth, int cardHeight, int panelWidth,
                                                 boolean isHovered) {
        int cardColor = isHovered ? BG_CARD_HOVER : BG_CARD;
        drawRoundedRect(context, x, y, cardWidth, cardHeight,
                scale(CARD_BORDER_RADIUS), cardColor);

        String icon = "👥";
        context.drawText(textRenderer, icon,
                x + scale(INVITE_CARD_ICON_LEFT_PADDING),
                y + scale(INVITE_CARD_ICON_TOP_PADDING),
                ACCENT_PRIMARY, false);

        context.drawText(textRenderer, senderName + " invited you",
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                y + scale(INVITE_CARD_TEXT_TOP_PADDING), TEXT_PRIMARY, true);
        context.drawText(textRenderer, "Join their team",
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                y + scale(INVITE_CARD_SUBTEXT_TOP_PADDING), TEXT_SECONDARY, false);

        if (isHovered) {
            int btnSize = scale(INVITE_ACTION_BUTTON_SIZE);
            int btnTop  = y + scale(INVITE_ACTION_BUTTON_TOP_PADDING);
            int btnRight = x + cardWidth - scale(INVITE_ACTION_BUTTONS_RIGHT_PADDING);

            int acceptX = btnRight - btnSize;
            int declineX = btnRight - btnSize - scale(INVITE_ACTION_BUTTON_SPACING);

            drawIconButton(context, acceptX, btnTop, btnSize, "✓", ACCENT_SUCCESS, width, panelWidth, textRenderer, scale(BUTTON_BORDER_RADIUS));
            if (mouseInRect(mouseX, mouseY, acceptX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Accept invitation"), mouseX, mouseY);
            }

            drawIconButton(context, declineX, btnTop, btnSize, "✕", ACCENT_DANGER, width, panelWidth, textRenderer, scale(BUTTON_BORDER_RADIUS));
            if (mouseInRect(mouseX, mouseY, declineX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Decline invitation"), mouseX, mouseY);
            }
        }
    }

    private static void renderOutgoingInviteCard(DrawContext context, TextRenderer textRenderer, String targetName,
                                                 long remainingTime, int x, int y, int width, int cardWidth, int cardHeight, int panelWidth, boolean isHovered) {
        int cardColor = isHovered ? BG_CARD_HOVER : BG_CARD;
        drawRoundedRect(context, x, y, cardWidth, cardHeight,
                scale(CARD_BORDER_RADIUS), cardColor);

        String icon = "📨";
        context.drawText(textRenderer, icon,
                x + scale(INVITE_CARD_ICON_LEFT_PADDING),
                y + scale(INVITE_CARD_ICON_TOP_PADDING),
                0xFF_F59E0B, false);

        context.drawText(textRenderer, "Invited " + targetName,
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                y + scale(INVITE_CARD_TEXT_TOP_PADDING), TEXT_PRIMARY, true);

        long seconds = remainingTime / 1000;
        String timeText = String.format("Expires in %ds", seconds);
        context.drawText(textRenderer, timeText,
                x + scale(INVITE_CARD_TEXT_LEFT_PADDING),
                y + scale(INVITE_CARD_SUBTEXT_TOP_PADDING), TEXT_SECONDARY, false);

        drawButton(context, x + cardWidth - scale(INVITE_CARD_BUTTON_SPACING),
                y + scale(INVITE_CARD_BUTTON_TOP_PADDING),
                scale(INVITE_CARD_BUTTON_WIDTH),
                scale(INVITE_CARD_BUTTON_HEIGHT),
                "Cancel",
                ACCENT_DANGER,
                width, panelWidth, textRenderer, scale(BUTTON_BORDER_RADIUS));
    }
}