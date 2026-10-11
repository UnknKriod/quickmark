package me.unknkriod.quickmark.gui.screen.tab;

import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.gui.screen.TeamManagementScreen;
import me.unknkriod.quickmark.gui.screen.TeamScreenLayout;
import me.unknkriod.quickmark.gui.screen.TeamScrollController;
import me.unknkriod.quickmark.gui.screen.TeamSidebarRenderer;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.*;
import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;

public class PlayersTabRenderer {

    public static void render(DrawContext context, TextRenderer textRenderer, int x, int y, int mouseX, int hoverMouseY, float delta,
                              int panelWidth, int sidebarWidth, TeamScrollController scrollController,
                              TextFieldWidget searchField, int cardWidth) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        String searchText = searchField.getText().toLowerCase();

        List<PlayerListEntry> players = client.getNetworkHandler().getPlayerList().stream()
                .filter(entry -> {
                    String name = entry.getProfile().getName();
                    if (client.player != null && name.equals(client.player.getName().getString())) return false;
                    if (TeamManager.isPlayerInTeam(name)) return false;
                    if (!searchText.isEmpty() && !name.toLowerCase().contains(searchText)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(e -> e.getProfile().getName()))
                .toList();

        if (players.isEmpty()) {
            String msg = searchText.isEmpty() ? "No players available" : "No results found";
            TeamManagementScreen.renderEmptyState(context, textRenderer, x, y, panelWidth, sidebarWidth, TeamSidebarRenderer.Tab.PLAYERS, msg, "Try a different search");
            return;
        }

        int availableWidth = panelWidth - sidebarWidth - scale(CARD_CONTENT_LEFT_PADDING) * 2;
        int actualCardWidth = Math.min(cardWidth, availableWidth);
        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);

        scrollController.setMaxScroll(Math.max(0, players.size() * scale(PLAYER_LIST_ITEM_SPACING) -
                (panelWidth - scale(98))));

        for (PlayerListEntry entry : players) {
            boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                    mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                    hoverMouseY >= cardY && hoverMouseY < cardY + scale(PLAYER_LIST_CARD_HEIGHT);
            renderPlayerListCard(context, textRenderer, entry,
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY,
                    actualCardWidth, isHovered);
            cardY += scale(PLAYER_LIST_ITEM_SPACING);
        }
    }

    private static void renderPlayerListCard(DrawContext context, TextRenderer textRenderer, PlayerListEntry entry,
                                             int x, int y, int actualWidth, boolean isHovered) {
        int cardColor = isHovered ? BG_CARD_HOVER : BG_CARD;
        drawRoundedRect(context, x, y, actualWidth, scale(PLAYER_LIST_CARD_HEIGHT),
                scale(PLAYER_LIST_CARD_BORDER_RADIUS), cardColor);

        UUID playerId = entry.getProfile().getId();

        TeamHudRenderer.renderHead(context,
                playerId,
                x + scale(PLAYER_LIST_AVATAR_LEFT_PADDING),
                y + scale(PLAYER_LIST_AVATAR_TOP_PADDING),
                scale(PLAYER_LIST_AVATAR_SIZE)
        );

        String name = entry.getProfile().getName();
        int maxNameWidth = actualWidth - scale(PLAYER_LIST_NAME_LEFT_PADDING) - scale(PLAYER_LIST_INVITE_RESERVED_WIDTH) - scale(20);
        String displayName = textRenderer.trimToWidth(name, maxNameWidth);
        context.drawText(textRenderer, displayName,
                x + scale(PLAYER_LIST_NAME_LEFT_PADDING),
                y + scale(PLAYER_LIST_NAME_TOP_PADDING), TEXT_PRIMARY, false);

        if (isHovered) {
            String buttonText = "+ Invite";
            int textWidth = textRenderer.getWidth(buttonText);
            int neededWidth = textWidth + scale(PLAYER_LIST_INVITE_TEXT_PADDING);
            int buttonWidth = Math.max(scale(PLAYER_LIST_INVITE_MIN_WIDTH), neededWidth);
            int rightPad = scale(PLAYER_LIST_INVITE_BUTTON_RIGHT_PADDING);
            int buttonX = x + actualWidth - rightPad - buttonWidth;

            if (buttonX < x + scale(10)) {
                buttonX = x + scale(10);
                buttonWidth = actualWidth - rightPad - scale(10);
            }

            drawButton(context, buttonX,
                    y + scale(PLAYER_LIST_INVITE_BUTTON_TOP_PADDING),
                    buttonWidth,
                    scale(PLAYER_LIST_INVITE_BUTTON_HEIGHT),
                    buttonText, ACCENT_PRIMARY,
                    0, 0, textRenderer, scale(BUTTON_BORDER_RADIUS));
        }
    }
}