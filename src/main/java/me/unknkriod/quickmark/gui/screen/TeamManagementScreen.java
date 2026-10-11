package me.unknkriod.quickmark.gui.screen;

import me.unknkriod.quickmark.gui.screen.tab.InvitesTabRenderer;
import me.unknkriod.quickmark.gui.screen.tab.PlayersTabRenderer;
import me.unknkriod.quickmark.gui.screen.tab.TeamTabRenderer;
import me.unknkriod.quickmark.network.NetworkSender;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import static me.unknkriod.quickmark.gui.screen.TeamSidebarRenderer.*;
import static me.unknkriod.quickmark.gui.screen.TeamSidebarRenderer.Tab.TEAM;
import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.*;
import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;

import java.util.*;

public class TeamManagementScreen extends Screen {
    private TextFieldWidget searchField;
    private ButtonWidget leaveButton;
    private final TeamScrollController scrollController;
    private final int panelWidth, panelHeight, cardWidth, cardHeight,
            sidebarWidth, cardSpacing, scrollbarWidth;

    private Tab currentTab = TEAM;
    private final TeamSidebarRenderer sidebarRenderer;

    private String statusMessage = null;
    private long statusMessageTime = 0;
    private float statusMessageAlpha = 0f;

    public TeamManagementScreen() {
        super(Text.literal("Team Management"));
        this.sidebarRenderer = new TeamSidebarRenderer();
        this.scrollController = new TeamScrollController();

        TeamScreenLayout.calculateScale();

        panelWidth = scale(BASE_PANEL_WIDTH);
        panelHeight = scale(BASE_PANEL_HEIGHT);
        cardWidth = scale(BASE_CARD_WIDTH);
        cardHeight = scale(BASE_CARD_HEIGHT);
        sidebarWidth = scale(BASE_SIDEBAR_WIDTH);
        cardSpacing = scale(BASE_CARD_SPACING);
        scrollbarWidth = scale(SCROLLBAR_WIDTH);
    }

    @Override
    protected void init() {
        super.init();

        TeamScreenLayout.calculateScale();

        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        int availableSearchWidth = cardWidth - scale(SEARCH_FIELD_HORIZONTAL_PADDING);
        int maxSearchWidth = panelWidth - sidebarWidth - scale(SEARCH_FIELD_LEFT_PADDING) * 2;
        int actualSearchWidth = Math.min(availableSearchWidth, maxSearchWidth);

        searchField = new TextFieldWidget(
                textRenderer,
                panelLeft + sidebarWidth + scale(SEARCH_FIELD_LEFT_PADDING),
                panelTop + scale(SEARCH_FIELD_TOP_PADDING),
                actualSearchWidth,
                scale(SEARCH_FIELD_HEIGHT),
                Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("🔍 Search players...").styled(style ->
                style.withColor(TEXT_SECONDARY)));
        addDrawableChild(searchField);

        int leaveButtonX = panelLeft + sidebarWidth + scale(SEARCH_FIELD_LEFT_PADDING);
        if (leaveButtonX + scale(LEAVE_BUTTON_WIDTH) > panelLeft + panelWidth) {
            leaveButtonX = panelLeft + panelWidth - scale(LEAVE_BUTTON_WIDTH) - scale(10);
        }

        leaveButton = ButtonWidget.builder(
                        Text.literal("Leave Team"),
                        button -> confirmLeaveTeam()
                )
                .position(leaveButtonX,
                        panelTop + panelHeight - scale(LEAVE_BUTTON_BOTTOM_MARGIN))
                .size(scale(LEAVE_BUTTON_WIDTH), scale(LEAVE_BUTTON_HEIGHT))
                .build();
        addDrawableChild(leaveButton);

        updateVisibility();
    }

    private void updateVisibility() {
        if (searchField != null) {
            searchField.setVisible(currentTab == Tab.PLAYERS);
        }
        if (leaveButton != null) {
            List<TeamPlayer> members = TeamManager.getTeamMembers();
            leaveButton.visible = currentTab == TEAM && members.size() > 1;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        drawRoundedRect(context, panelLeft, panelTop, panelWidth, panelHeight,
                scale(PANEL_BORDER_RADIUS), BG_PRIMARY);

        sidebarRenderer.renderSidebar(context, panelLeft, panelTop, mouseX, mouseY, delta, currentTab, sidebarWidth, panelHeight);

        int contentLeft = panelLeft + sidebarWidth;
        int contentTop = panelTop;
        int contentWidth = panelWidth - sidebarWidth;

        renderHeader(context, contentLeft, contentTop, contentWidth);

        int hoverMouseY = mouseY + scrollController.getScrollOffset();

        context.enableScissor(contentLeft, contentTop + scale(CONTENT_SCROLL_AREA_TOP),
                contentLeft + contentWidth, contentTop + panelHeight - scale(CONTENT_SCROLL_AREA_BOTTOM));
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, -scrollController.getScrollOffset());

        int scrollContentTop = contentTop + scale(CONTENT_SCROLL_AREA_TOP);
        switch (currentTab) {
            case TEAM -> TeamTabRenderer.render(context, textRenderer, currentTab, contentLeft, scrollContentTop,
                    mouseX, mouseY + scrollController.getScrollOffset(), delta, panelWidth, panelHeight, sidebarWidth, cardWidth, cardHeight, cardSpacing, scrollController);
            case INVITES -> InvitesTabRenderer.render(context, textRenderer, contentLeft, scrollContentTop,
                    mouseX, mouseY + scrollController.getScrollOffset(), width, panelWidth, panelHeight, sidebarWidth, cardWidth, cardHeight, cardSpacing, scrollController);
            case PLAYERS -> PlayersTabRenderer.render(context, textRenderer, contentLeft, scrollContentTop,
                    mouseX, hoverMouseY, delta, panelWidth, sidebarWidth, scrollController, searchField, cardWidth);
        }

        context.getMatrices().popMatrix();
        context.disableScissor();

        scrollController.renderScrollbar(context, contentLeft + contentWidth - scale(6), contentTop + scale(CONTENT_SCROLL_AREA_TOP), panelHeight - scale(98), mouseX, mouseY, this);

        renderFooter(context, contentLeft, contentTop + panelHeight - scale(FOOTER_TOP_MARGIN), contentWidth);

        StatusMessageRenderer.render(context, textRenderer, width, height, panelHeight, statusMessage, statusMessageTime, statusMessageAlpha, delta, TeamScreenLayout.getCurrentScale());

        sidebarRenderer.updateAnimations(delta, currentTab);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int x, int y, int width) {
        String title = switch (currentTab) {
            case TEAM -> "Your Team";
            case INVITES -> "Invitations";
            case PLAYERS -> "Invite Players";
        };

        context.drawText(textRenderer, title,
                x + scale(SEARCH_FIELD_LEFT_PADDING),
                y + scale(CONTENT_HEADER_TOP_PADDING),
                TEXT_PRIMARY, true);

        String subtitle = switch (currentTab) {
            case TEAM -> TeamManager.getTeamMembers().size() + " members";
            case INVITES -> getInviteCount() + " pending";
            case PLAYERS -> "Select players to invite";
        };

        context.drawText(textRenderer, subtitle,
                x + scale(SEARCH_FIELD_LEFT_PADDING),
                y + scale(CONTENT_HEADER_BOTTOM_PADDING),
                TEXT_SECONDARY, false);
    }

    public static void renderEmptyState(DrawContext context, TextRenderer textRenderer, int x, int y, int panelWidth, int sidebarWidth, Tab currentTab, String title, String subtitle) {
        int centerX = x + (panelWidth - sidebarWidth) / 2;
        int centerY = y + scale(EMPTY_STATE_TOP_PADDING);

        String icon;
        switch (currentTab) {
            case INVITES -> icon = "📭";
            case PLAYERS -> icon = "👤";
            default -> icon = "";
        }

        int iconWidth = textRenderer.getWidth(icon) * EMPTY_STATE_ICON_SCALE;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(EMPTY_STATE_ICON_SCALE, EMPTY_STATE_ICON_SCALE);
        context.drawText(textRenderer, icon, -iconWidth / 4, 0, TEXT_SECONDARY, false);
        context.getMatrices().popMatrix();

        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title,
                centerX - titleWidth / 2,
                centerY + scale(EMPTY_STATE_TITLE_TOP_PADDING),
                TEXT_SECONDARY, true);

        int subWidth = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, subtitle,
                centerX - subWidth / 2,
                centerY + scale(EMPTY_STATE_SUBTITLE_TOP_PADDING),
                0x60_FFFFFF, false);
    }

    private void renderFooter(DrawContext context, int x, int y, int width) {
        context.fill(x + scale(FOOTER_BUTTON_HORIZONTAL_PADDING), y,
                x + width - scale(FOOTER_BUTTON_HORIZONTAL_PADDING),
                y + scale(FOOTER_BORDER_HEIGHT), BORDER_COLOR);

        drawButton(context, x + width - scale(CLOSE_BUTTON_RIGHT_PADDING),
                y + scale(CLOSE_BUTTON_TOP_PADDING),
                scale(CLOSE_BUTTON_WIDTH), scale(CLOSE_BUTTON_HEIGHT),
                "Close", BG_CARD_HOVER,
                width, panelWidth, textRenderer, scale(BUTTON_BORDER_RADIUS));
    }

    private int getInviteCount() {
        return TeamManager.getIncomingInvitations().size() +
                TeamManager.getOutgoingInvitations().size();
    }

    private void confirmLeaveTeam() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<TeamPlayer> members = TeamManager.getTeamMembers();
        boolean isLeader = TeamManager.isLeader(client.player.getUuid());

        if (isLeader && members.size() > 1) {
            client.player.sendMessage(
                    Text.literal("Promote someone else to leader before leaving"), false);
            return;
        }

        TeamManager.removePlayer(client.player.getUuid());
        NetworkSender.sendTeamUpdate();
        showStatusMessage("Left the team");
        client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        close();
    }

    private void handleContentClick(int contentX, int contentY, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int cardY = contentY + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);

        switch (currentTab) {
            case TEAM -> {
                List<TeamPlayer> members = TeamManager.getTeamMembers();
                for (TeamPlayer player : members) {
                    if (mouseX >= contentX + scale(CARD_CONTENT_LEFT_PADDING) &&
                            mouseX < contentX + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                            mouseY >= cardY && mouseY < cardY + cardHeight) {
                        handleTeamMemberClick(player,
                                mouseX - (contentX + scale(CARD_CONTENT_LEFT_PADDING)),
                                mouseY - cardY);
                        return;
                    }
                    cardY += cardHeight + cardSpacing;
                }
            }
            case INVITES -> {
                Map<UUID, String> incoming = TeamManager.getIncomingInvitations();
                cardY += scale(CONTENT_SECTION_SPACING);

                for (Map.Entry<UUID, String> entry : incoming.entrySet()) {
                    if (mouseX >= contentX + scale(CARD_CONTENT_LEFT_PADDING) &&
                            mouseX < contentX + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                            mouseY >= cardY && mouseY < cardY + cardHeight) {
                        handleIncomingInviteClick(entry.getKey(),
                                mouseX - (contentX + scale(CARD_CONTENT_LEFT_PADDING)));
                        return;
                    }
                    cardY += cardHeight + cardSpacing;
                }

                Map<UUID, Map.Entry<String, Long>> outgoing = TeamManager.getOutgoingInvitationsWithTime();
                if (!outgoing.isEmpty()) {
                    cardY += scale(CONTENT_SECTION_LABEL_SPACING);
                    for (Map.Entry<UUID, Map.Entry<String, Long>> entry : outgoing.entrySet()) {
                        if (mouseX >= contentX + scale(CARD_CONTENT_LEFT_PADDING) &&
                                mouseX < contentX + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                                mouseY >= cardY && mouseY < cardY + cardHeight) {
                            handleOutgoingInviteClick(entry.getKey(),
                                    mouseX - (contentX + scale(CARD_CONTENT_LEFT_PADDING)));
                            return;
                        }
                        cardY += cardHeight + cardSpacing;
                    }
                }
            }
            case PLAYERS -> {
                if (client.getNetworkHandler() == null) return;
                String searchText = searchField.getText().toLowerCase();

                List<PlayerListEntry> players = client.getNetworkHandler().getPlayerList().stream()
                        .filter(entry -> {
                            String name = entry.getProfile().getName();
                            if (name.equals(client.player.getName().getString())) return false;
                            if (TeamManager.isPlayerInTeam(name)) return false;
                            if (!searchText.isEmpty() && !name.toLowerCase().contains(searchText)) return false;
                            return true;
                        })
                        .sorted(Comparator.comparing(e -> e.getProfile().getName()))
                        .toList();

                for (PlayerListEntry entry : players) {
                    if (mouseX >= contentX + scale(CARD_CONTENT_LEFT_PADDING) &&
                            mouseX < contentX + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                            mouseY >= cardY && mouseY < cardY + scale(PLAYER_LIST_CARD_HEIGHT)) {
                        invitePlayer(entry.getProfile().getId(), entry.getProfile().getName());
                        return;
                    }
                    cardY += scale(PLAYER_LIST_ITEM_SPACING);
                }
            }
        }
    }

    private void handleTeamMemberClick(TeamPlayer player, int relX, int relY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean isSelf = player.getPlayerId().equals(client.player.getUuid());
        boolean isLeader = TeamManager.isLeader(client.player.getUuid());

        if (!isLeader || isSelf) return;

        boolean isPlayerLeader = TeamManager.isLeader(player.getPlayerId());

        int buttonSize = scale(ACTION_BUTTON_SIZE);
        int actionRightPadding = scale(ACTION_BUTTONS_RIGHT_PADDING);
        int actionSpacing = scale(ACTION_BUTTON_SPACING);
        int actionTop = scale(ACTION_BUTTONS_TOP_PADDING);

        if (!isPlayerLeader && relX >= cardWidth - actionRightPadding &&
                relX < cardWidth - actionRightPadding + buttonSize &&
                relY >= actionTop && relY < actionTop + buttonSize) {

            String result = TeamManager.promotePlayer(player.getPlayerId());
            if (!result.isEmpty()) {
                String cleanMessage = result.replace("§a", "").replace("§c", "");
                showStatusMessage(cleanMessage);
            }

            if (result.startsWith("§a")) { // Success
                NetworkSender.sendTeamUpdate();
                client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            return;
        }

        if (relX >= cardWidth - actionRightPadding + actionSpacing &&
                relX < cardWidth - actionRightPadding + actionSpacing + buttonSize &&
                relY >= actionTop && relY < actionTop + buttonSize) {
            TeamManager.removePlayer(player.getPlayerId());
            NetworkSender.sendTeamUpdate();
            showStatusMessage("Kicked " + player.getPlayerName() + " from the team");
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
    }

    private void handleIncomingInviteClick(UUID senderId, int relX) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int btnSize = scale(INVITE_ACTION_BUTTON_SIZE);
        int rightPad = scale(INVITE_ACTION_BUTTONS_RIGHT_PADDING);
        int spacing  = scale(INVITE_ACTION_BUTTON_SPACING);
        int actionRight = cardWidth - rightPad;

        // Accept
        int acceptLeft = actionRight - btnSize;
        if (relX >= acceptLeft && relX < acceptLeft + btnSize) {
            TeamManager.acceptInvitation(senderId);
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            showStatusMessage("Invitation accepted");
            return;
        }

        // Decline
        int declineLeft = actionRight - btnSize + spacing;
        if (relX >= declineLeft && relX < declineLeft + btnSize) {
            TeamManager.declineIncomingInvitation(senderId);
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
            showStatusMessage("Invitation declined");
        }
    }

    private void handleOutgoingInviteClick(UUID targetId, int relX) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (relX >= cardWidth - scale(INVITE_CARD_BUTTON_SPACING) &&
                relX < cardWidth - scale(7)) {
            TeamManager.cancelOutgoingInvitation(targetId);
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
        }
    }

    private void invitePlayer(UUID playerId, String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (TeamManager.hasOutgoingInvitation(playerId)) {
            client.player.sendMessage(Text.literal("Already sent invitation"), false);
            return;
        }

        TeamManager.sendInvitation(playerId);

        showStatusMessage("Sent invitation to " + playerName);
        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        client.player.sendMessage(Text.literal("Sent invitation to " + playerName), false);
    }

    private void switchTab(Tab newTab) {
        currentTab = newTab;
        scrollController.resetScroll();
        updateVisibility();
    }

    private void showStatusMessage(String message) {
        this.statusMessage = message;
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        Tab clickedTab = sidebarRenderer.getTabAt((int) mouseX, (int) mouseY, panelLeft, panelTop, sidebarWidth);
        if (clickedTab != null) {
            switchTab(clickedTab);
            return true;
        }

        int contentLeft = panelLeft + sidebarWidth;
        int scrollbarX = contentLeft + (panelWidth - sidebarWidth) - scale(SCROLLBAR_WIDTH);
        int scrollbarY = panelTop + scale(CONTENT_SCROLL_AREA_TOP);
        int scrollbarHeight = panelHeight - scale(98);
        if (scrollController.getMaxScroll() > 0 &&
                mouseX >= scrollbarX && mouseX < scrollbarX + scale(SCROLLBAR_WIDTH) &&
                mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
            scrollController.startDragging((int) mouseY, scrollController.getScrollOffset());
            return true;
        }

        int contentTop = panelTop + scale(CONTENT_SCROLL_AREA_TOP);
        int scrolledMouseY = (int) mouseY + scrollController.getScrollOffset();
        handleContentClick(contentLeft, contentTop, (int) mouseX, scrolledMouseY);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollController.isDragging() && scrollController.getMaxScroll() > 0) {
            int centerY = height / 2;
            int panelTop = centerY - panelHeight / 2;
            int scrollbarHeight = panelHeight - scale(98);

            int deltaMove = (int) mouseY - scrollController.getScrollbarDragStartY();
            float scrollPercent = (float) deltaMove / scrollbarHeight;
            return scrollController.handleDrag((int) mouseY, scrollbarHeight, scrollController.getMaxScroll());
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollController.stopDragging();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollController.handleScroll(verticalAmount, scale(SCROLL_SPEED))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}