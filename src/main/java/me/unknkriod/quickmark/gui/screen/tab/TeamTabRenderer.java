package me.unknkriod.quickmark.gui.screen.tab;

import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.gui.screen.TeamManagementScreen;
import me.unknkriod.quickmark.gui.screen.TeamScrollController;
import me.unknkriod.quickmark.gui.screen.TeamSidebarRenderer;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;
import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.*;

public class TeamTabRenderer {
    private static final Map<UUID, Float> hoverAnimations = new HashMap<>();

    public static void render(
            DrawContext context,
            TextRenderer textRenderer,
            TeamSidebarRenderer.Tab currentTab,
            int x,
            int y,
            int mouseX,
            int mouseY,
            float delta,
            int panelWidth,
            int panelHeight,
            int sidebarWidth,
            int cardWidth,
            int cardHeight,
            int cardSpacing,
            TeamScrollController scrollController
    ) {
        List<TeamPlayer> members = TeamManager.getTeamMembers();
        MinecraftClient client = MinecraftClient.getInstance();

        if (members.isEmpty()) {
            TeamManagementScreen.renderEmptyState(context, textRenderer, x, y, panelWidth, sidebarWidth, currentTab, "No team members", "Start by inviting players");
            return;
        }

        int actualCardWidth = cardContentWidth(panelWidth, sidebarWidth, cardWidth);
        int panelRight = panelRight(x, sidebarWidth, panelWidth);

        // Keep the hover-animation map bounded by the players actually on the team instead of
        // growing it by one entry for every UUID ever seen.
        Set<UUID> memberIds = new HashSet<>();
        for (TeamPlayer member : members) {
            memberIds.add(member.getPlayerId());
        }
        hoverAnimations.keySet().retainAll(memberIds);

        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        scrollController.setMaxScroll(Math.max(0, members.size() * (cardHeight + cardSpacing) -
                contentScrollHeight(panelHeight)));

        for (int i = 0; i < members.size(); i++) {
            TeamPlayer player = members.get(i);
            boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                    mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                    mouseY >= cardY && mouseY < cardY + cardHeight;

            renderTeamMemberCard(context, textRenderer, player, i,
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY,
                    actualCardWidth, cardHeight, panelRight, isHovered, mouseX, mouseY, delta);
            cardY += cardHeight + cardSpacing;
        }
    }

    private static void renderTeamMemberCard(DrawContext context, TextRenderer textRenderer, TeamPlayer player, int position,
                                             int x, int y, int actualWidth, int cardHeight, int panelRight, boolean isHovered,
                                             int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        UUID playerId = player.getPlayerId();

        float hoverAnim = hoverAnimations.getOrDefault(playerId, 0f);
        if (isHovered) {
            hoverAnim = Math.min(1f, hoverAnim + delta * HOVER_ANIMATION_SPEED);
        } else {
            hoverAnim = Math.max(0f, hoverAnim - delta * HOVER_ANIMATION_SPEED);
        }
        hoverAnimations.put(playerId, hoverAnim);

        int cardColor = lerpColor(BG_CARD, BG_CARD_HOVER, hoverAnim);
        drawRoundedRect(context, x, y, actualWidth, cardHeight,
                scale(CARD_BORDER_RADIUS), cardColor);

        int posColor = TeamManager.getColorForPosition(position);
        context.fill(x, y + scale(CARD_POSITION_INDICATOR_TOP_PADDING),
                x + scale(CARD_POSITION_INDICATOR_WIDTH),
                y + cardHeight - scale(CARD_POSITION_INDICATOR_TOP_PADDING), posColor);

        renderPlayerHead(context, player,
                x + scale(PLAYER_HEAD_LEFT_PADDING),
                y + scale(PLAYER_HEAD_TOP_PADDING),
                scale(PLAYER_HEAD_SIZE));

        String name = player.getPlayerName();
        boolean isLeader = TeamManager.isLeader(playerId);
        boolean isSelf = client.player != null && playerId.equals(client.player.getUuid());

        String displayName = name;
        if (isLeader) displayName = "👑 " + displayName;
        if (isSelf) displayName += " (You)";

        context.drawText(textRenderer, displayName,
                x + scale(PLAYER_INFO_LEFT_PADDING),
                y + scale(PLAYER_INFO_TOP_PADDING), TEXT_PRIMARY, true);

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorption();
        float healthPercent = Math.min(1.0f, (health + absorption) / maxHealth);

        int maxHealthBarWidth = actualWidth - scale(PLAYER_INFO_LEFT_PADDING) - scale(10);
        int barWidth = Math.min(scale(HEALTH_BAR_WIDTH), maxHealthBarWidth);
        int barHeight = scale(HEALTH_BAR_HEIGHT);
        int barX = x + scale(PLAYER_INFO_LEFT_PADDING);
        int barY = y + scale(HEALTH_BAR_TOP_PADDING);

        drawRoundedRect(context, barX, barY, barWidth, barHeight,
                scale(HEALTH_BAR_BORDER_RADIUS), 0x80_000000);

        int healthColor = getHealthColor((health + absorption) / maxHealth);
        drawRoundedRect(context, barX, barY, (int) (barWidth * healthPercent), barHeight,
                scale(HEALTH_BAR_BORDER_RADIUS), healthColor);

        if (absorption > 0) {
            String healthWithAbsorption = String.format("%.1f", health + absorption);
            String maxHealthStr = String.format("%.0f", maxHealth);
            String heart = " ❤";

            int healthWithAbsorptionWidth = textRenderer.getWidth(healthWithAbsorption);
            int slashWidth = textRenderer.getWidth(" / ");
            int maxHealthWidth = textRenderer.getWidth(maxHealthStr);
            int heartWidth = textRenderer.getWidth(heart);

            int currentX = barX;

            context.drawText(textRenderer, healthWithAbsorption, currentX,
                    barY + scale(HEALTH_TEXT_TOP_PADDING), ABSORPTION_COLOR, false);
            currentX += healthWithAbsorptionWidth;

            context.drawText(textRenderer, " / ", currentX,
                    barY + scale(HEALTH_TEXT_TOP_PADDING), TEXT_SECONDARY, false);
            currentX += slashWidth;

            context.drawText(textRenderer, maxHealthStr, currentX,
                    barY + scale(HEALTH_TEXT_TOP_PADDING), TEXT_SECONDARY, false);
            currentX += maxHealthWidth;

            context.drawText(textRenderer, heart, currentX,
                    barY + scale(HEALTH_TEXT_TOP_PADDING), TEXT_SECONDARY, false);
        } else {
            String healthText = String.format("%.1f / %.0f ❤", health, maxHealth);
            context.drawText(textRenderer, healthText, barX, barY + scale(HEALTH_TEXT_TOP_PADDING),
                    TEXT_SECONDARY, false);
        }

        if (client.player != null && TeamManager.isLeader(client.player.getUuid()) && !isSelf) {
            renderActionButtons(context, textRenderer, isLeader,
                    x, y, actualWidth, panelRight, isHovered, mouseX, mouseY);
        }
    }

    /**
     * Draws the promote/kick buttons for a card.
     * <p>
     * Positions come from {@link TeamScreenLayout}, the same helpers the click handler uses, so a
     * button can never be drawn somewhere other than where it is clickable.
     */
    private static void renderActionButtons(DrawContext context, TextRenderer textRenderer, boolean isPlayerLeader,
                                            int cardX, int cardY, int actualWidth, int panelRight,
                                            boolean parentHovered, int mouseX, int mouseY) {
        if (!parentHovered) return;

        int buttonSize = scale(ACTION_BUTTON_SIZE);
        int buttonTop = cardY + scale(ACTION_BUTTONS_TOP_PADDING);

        if (!isPlayerLeader) {
            int leadX = cardX + promoteButtonX(actualWidth);
            drawIconButton(context, leadX, buttonTop, buttonSize, "⬆", ACCENT_SUCCESS, panelRight,
                    textRenderer, scale(BUTTON_BORDER_RADIUS));
            if (mouseInRect(mouseX, mouseY, leadX, buttonTop, buttonSize, buttonSize)) {
                context.drawTooltip(textRenderer, Text.literal("Make leader"), mouseX, mouseY);
            }
        }

        int kickX = cardX + kickButtonX(actualWidth);
        drawIconButton(context, kickX, buttonTop, buttonSize, "✕", ACCENT_DANGER, panelRight,
                textRenderer, scale(BUTTON_BORDER_RADIUS));
        if (mouseInRect(mouseX, mouseY, kickX, buttonTop, buttonSize, buttonSize)) {
            context.drawTooltip(textRenderer, Text.literal("Kick from team"), mouseX, mouseY);
        }
    }

    private static void renderPlayerHead(DrawContext context, TeamPlayer player, int x, int y, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        TeamHudRenderer.renderHead(context, player.getPlayerId(), x, y, size);
    }
}