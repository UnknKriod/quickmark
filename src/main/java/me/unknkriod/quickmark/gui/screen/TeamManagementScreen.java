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

    // Button captions are shared between measuring the button and drawing it, so the two cannot
    // disagree about how much room the label needs.
    private static final String CLOSE_LABEL = "Close";
    private static final String LEAVE_LABEL = "Leave Team";

    private final TeamScrollController scrollController;
    // Not final: these are derived from the current GUI scale, which changes when the window is
    // resized or toggled to fullscreen while the screen is open. See updateLayout().
    private int panelWidth, panelHeight, cardWidth, cardHeight,
            sidebarWidth, cardSpacing;

    private Tab currentTab = TEAM;
    private final TeamSidebarRenderer sidebarRenderer;
    private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();

    public TeamManagementScreen() {
        super(Text.literal("Team Management"));
        this.sidebarRenderer = new TeamSidebarRenderer();
        this.scrollController = new TeamScrollController();

        updateLayout();
    }

    /**
     * Recomputes the GUI scale and every size derived from it.
     * <p>
     * {@link #init()} runs again whenever the window is resized or fullscreen is toggled while the
     * screen is open. The cached sizes used to be final and set only in the constructor, so after
     * such a change {@code scale(...)} calls made during rendering used the new scale while
     * {@code sidebarWidth} and the panel size kept the old one: tabs came out narrower than tall and
     * the logo lost its margin.
     */
    private void updateLayout() {
        TeamScreenLayout.calculateScale();

        panelWidth = scale(BASE_PANEL_WIDTH);
        panelHeight = scale(BASE_PANEL_HEIGHT);
        cardWidth = scale(BASE_CARD_WIDTH);
        cardHeight = scale(BASE_CARD_HEIGHT);
        sidebarWidth = scale(BASE_SIDEBAR_WIDTH);
        cardSpacing = scale(BASE_CARD_SPACING);
    }

    @Override
    protected void init() {
        super.init();

        updateLayout();

        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        // The field's width is the content width less the side padding on each side, so its left and
        // right margins are always equal and it can never end up flush against the panel's edge.
        int contentLeft = panelLeft + sidebarWidth;
        int contentWidth = panelWidth - sidebarWidth;

        searchField = new TextFieldWidget(
                textRenderer,
                contentLeft + searchFieldSidePadding(),
                panelTop + scale(SEARCH_FIELD_TOP_PADDING),
                searchFieldWidth(contentWidth),
                scale(SEARCH_FIELD_HEIGHT),
                Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("🔍 Search players...").styled(style ->
                style.withColor(TEXT_SECONDARY)));
        addDrawableChild(searchField);

        updateVisibility();
    }

    private void updateVisibility() {
        if (searchField != null) {
            searchField.setVisible(currentTab == Tab.PLAYERS);
        }
    }

    /** Whether the Leave Team button is offered on the current tab. */
    private boolean isLeaveButtonVisible() {
        // The leader of a multi-member team cannot leave without promoting someone else, so the
        // button is only offered when leaving is actually possible.
        return currentTab == TEAM && TeamManager.canLeaveTeam();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Team state can change while the screen is open (promotions, kicks, network updates),
        // so the conditional widgets are refreshed every frame instead of only on init.
        updateVisibility();

        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        drawRoundedRect(context, panelLeft, panelTop, panelWidth, panelHeight,
                scale(PANEL_BORDER_RADIUS), BG_PRIMARY);

        sidebarRenderer.renderSidebar(context, panelLeft, panelTop, mouseX, mouseY, currentTab, sidebarWidth, panelHeight);

        int contentLeft = panelLeft + sidebarWidth;
        int contentTop = panelTop;
        int contentWidth = panelWidth - sidebarWidth;
        int contentHeight = TeamScreenLayout.contentScrollHeight(panelHeight);
        int scrollAreaTop = contentTop + scale(CONTENT_SCROLL_AREA_TOP);

        renderHeader(context, contentLeft, contentTop, contentWidth);

        int hoverMouseY = mouseY + scrollController.getScrollOffset();

        context.enableScissor(contentLeft, scrollAreaTop,
                contentLeft + contentWidth, scrollAreaTop + contentHeight);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, -scrollController.getScrollOffset());

        switch (currentTab) {
            case TEAM -> TeamTabRenderer.render(context, textRenderer, currentTab, contentLeft, scrollAreaTop,
                    mouseX, hoverMouseY, delta, panelWidth, panelHeight, sidebarWidth, cardWidth, cardHeight, cardSpacing, scrollController);
            case INVITES -> InvitesTabRenderer.render(context, textRenderer, contentLeft, scrollAreaTop,
                    mouseX, hoverMouseY, panelWidth, panelHeight, sidebarWidth, cardWidth, cardHeight, cardSpacing, scrollController);
            case PLAYERS -> PlayersTabRenderer.render(context, textRenderer, contentLeft, scrollAreaTop,
                    mouseX, hoverMouseY, panelWidth, panelHeight, sidebarWidth, scrollController, searchField, cardWidth);
        }

        context.getMatrices().popMatrix();
        context.disableScissor();

        scrollController.renderScrollbar(context,
                TeamScreenLayout.scrollbarX(contentLeft, contentWidth), scrollAreaTop,
                contentHeight, mouseX, mouseY);

        renderFooter(context, panelLeft, panelTop);

        statusRenderer.render(context, textRenderer, width, height, panelHeight,
                TeamScreenLayout.getCurrentScale());

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int x, int y, int width) {
        // The Invite tab shows the search field where the title would be, so it draws no title.
        // Its subtitle stays on the same line as on the other tabs.
        String title = switch (currentTab) {
            case TEAM -> "Your Team";
            case INVITES -> "Invitations";
            case PLAYERS -> null;
        };

        if (title != null) {
            context.drawText(textRenderer, title,
                    x + searchFieldSidePadding(),
                    y + scale(CONTENT_HEADER_TOP_PADDING),
                    TEXT_PRIMARY, true);
        }

        String subtitle = switch (currentTab) {
            case TEAM -> TeamManager.getTeamMembers().size() + " members";
            case INVITES -> getInviteCount() + " pending";
            case PLAYERS -> "Select players to invite";
        };

        context.drawText(textRenderer, subtitle,
                x + searchFieldSidePadding(),
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

        int titleY = centerY + scale(EMPTY_STATE_TITLE_TOP_PADDING);
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title,
                centerX - titleWidth / 2,
                titleY,
                TEXT_SECONDARY, true);

        int subWidth = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, subtitle,
                centerX - subWidth / 2,
                emptyStateSubtitleTop(titleY, textRenderer.fontHeight),
                0x60_FFFFFF, false);
    }

    /** Screen-space rectangle of the Close button, shared by drawing and hit testing. */
    private record Bounds(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private Bounds closeButtonBounds(int panelLeft, int panelTop) {
        int contentLeft = panelLeft + sidebarWidth;
        int contentWidth = panelWidth - sidebarWidth;
        int x = contentLeft + contentWidth - scale(CLOSE_BUTTON_RIGHT_PADDING);
        int clipRight = TeamScreenLayout.panelRight(contentLeft, sidebarWidth, panelWidth);
        // Width comes from the label, so a small window cannot trim the caption.
        int width = Math.min(buttonWidth(CLOSE_BUTTON_WIDTH, textRenderer.getWidth(CLOSE_LABEL)), clipRight - x);
        int footerY = panelTop + panelHeight - scale(FOOTER_TOP_MARGIN);

        return new Bounds(x, footerY + scale(CLOSE_BUTTON_TOP_PADDING),
                width, scale(CLOSE_BUTTON_HEIGHT));
    }

    /**
     * Screen-space rectangle of the Leave Team button, shared by drawing and hit testing.
     * It sits on the same footer row as Close and is drawn flat with the same helper.
     */
    private Bounds leaveButtonBounds(int panelLeft, int panelTop) {
        int contentLeft = panelLeft + sidebarWidth;
        int x = contentLeft + searchFieldSidePadding();
        int clipRight = TeamScreenLayout.panelRight(contentLeft, sidebarWidth, panelWidth);
        // Width comes from the label, so a small window cannot trim "Leave Team".
        int width = Math.min(buttonWidth(LEAVE_BUTTON_WIDTH, textRenderer.getWidth(LEAVE_LABEL)), clipRight - x);

        int footerY = panelTop + panelHeight - scale(FOOTER_TOP_MARGIN);
        return new Bounds(x, footerY + scale(CLOSE_BUTTON_TOP_PADDING),
                width, scale(LEAVE_BUTTON_HEIGHT));
    }

    private void renderFooter(DrawContext context, int panelLeft, int panelTop) {
        int x = panelLeft + sidebarWidth;
        int width = panelWidth - sidebarWidth;
        int y = panelTop + panelHeight - scale(FOOTER_TOP_MARGIN);

        context.fill(x + scale(FOOTER_BUTTON_HORIZONTAL_PADDING), y,
                x + width - scale(FOOTER_BUTTON_HORIZONTAL_PADDING),
                y + scale(FOOTER_BORDER_HEIGHT), BORDER_COLOR);

        int clipRight = TeamScreenLayout.panelRight(x, sidebarWidth, panelWidth);

        Bounds close = closeButtonBounds(panelLeft, panelTop);
        drawButton(context, close.x(), close.y(), close.width(), close.height(),
                CLOSE_LABEL, BG_CARD_HOVER, clipRight, textRenderer, scale(BUTTON_BORDER_RADIUS));

        if (isLeaveButtonVisible()) {
            Bounds leave = leaveButtonBounds(panelLeft, panelTop);
            drawButton(context, leave.x(), leave.y(), leave.width(), leave.height(),
                    LEAVE_LABEL, BG_CARD_HOVER, clipRight, textRenderer, scale(BUTTON_BORDER_RADIUS));
        }
    }

    private int getInviteCount() {
        return TeamManager.getIncomingInvitations().size() +
                TeamManager.getOutgoingInvitations().size();
    }

    private void confirmLeaveTeam() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!TeamManager.canLeaveTeam()) {
            client.player.sendMessage(Text.translatableWithFallback(
                    "quickmark.gui.cannot_leave_as_leader",
                    "Promote someone else to leader before leaving"), false);
            return;
        }

        TeamManager.leaveTeam();
        showStatusMessage("Left the team");
        client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        close();
    }

    private void handleContentClick(int contentX, int contentY, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Cards are drawn clamped to the content viewport, so every hit box below has to use the
        // same width the renderer used. Passing cardWidth here is what used to put the promote and
        // kick buttons about 40 px away from where they are drawn.
        int actualCardWidth = TeamScreenLayout.cardContentWidth(panelWidth, sidebarWidth, cardWidth);
        int cardLeft = contentX + scale(CARD_CONTENT_LEFT_PADDING);
        boolean inCardColumn = mouseX >= cardLeft && mouseX < cardLeft + actualCardWidth;

        int cardY = contentY + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);

        switch (currentTab) {
            case TEAM -> {
                List<TeamPlayer> members = TeamManager.getTeamMembers();
                for (TeamPlayer player : members) {
                    if (inCardColumn && mouseY >= cardY && mouseY < cardY + cardHeight) {
                        handleTeamMemberClick(player, mouseX - cardLeft, mouseY - cardY, actualCardWidth);
                        return;
                    }
                    cardY += cardHeight + cardSpacing;
                }
            }
            case INVITES -> {
                Map<UUID, String> incoming = TeamManager.getIncomingInvitations();
                cardY += scale(CONTENT_SECTION_SPACING);

                for (Map.Entry<UUID, String> entry : incoming.entrySet()) {
                    if (inCardColumn && mouseY >= cardY && mouseY < cardY + cardHeight) {
                        handleIncomingInviteClick(entry.getKey(), mouseX - cardLeft, mouseY - cardY, actualCardWidth);
                        return;
                    }
                    cardY += cardHeight + cardSpacing;
                }

                Map<UUID, Map.Entry<String, Long>> outgoing = TeamManager.getOutgoingInvitationsWithTime();
                if (!outgoing.isEmpty()) {
                    cardY += scale(CONTENT_SECTION_LABEL_SPACING);
                    for (Map.Entry<UUID, Map.Entry<String, Long>> entry : outgoing.entrySet()) {
                        if (inCardColumn && mouseY >= cardY && mouseY < cardY + cardHeight) {
                            handleOutgoingInviteClick(entry.getKey(), mouseX - cardLeft, actualCardWidth);
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
                    if (inCardColumn && mouseY >= cardY && mouseY < cardY + scale(PLAYER_LIST_CARD_HEIGHT)) {
                        invitePlayer(entry.getProfile().getId(), entry.getProfile().getName());
                        return;
                    }
                    cardY += scale(PLAYER_LIST_ITEM_SPACING);
                }
            }
        }
    }

    private void handleTeamMemberClick(TeamPlayer player, int relX, int relY, int actualCardWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean isSelf = player.getPlayerId().equals(client.player.getUuid());
        boolean isLeader = TeamManager.isLeader(client.player.getUuid());

        if (!isLeader || isSelf) return;

        boolean isPlayerLeader = TeamManager.isLeader(player.getPlayerId());

        if (!isPlayerLeader && TeamScreenLayout.isOverPromoteButton(relX, relY, actualCardWidth)) {
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

        if (TeamScreenLayout.isOverKickButton(relX, relY, actualCardWidth)) {
            TeamManager.removePlayer(player.getPlayerId());
            NetworkSender.sendTeamUpdate();
            showStatusMessage("Kicked " + player.getPlayerName() + " from the team");
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
    }

    private void handleIncomingInviteClick(UUID senderId, int relX, int relY, int actualCardWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Same helpers the renderer draws with, so the hit boxes cannot drift from the buttons.
        int btnSize = scale(INVITE_ACTION_BUTTON_SIZE);
        int btnTop = TeamScreenLayout.inviteActionButtonTop(cardHeight, textRenderer.fontHeight);

        if (TeamScreenLayout.isOverButton(relX, relY,
                TeamScreenLayout.acceptInviteButtonX(actualCardWidth), btnTop, btnSize)) {
            TeamManager.acceptInvitation(senderId);
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            showStatusMessage("Invitation accepted");
            return;
        }

        if (TeamScreenLayout.isOverButton(relX, relY,
                TeamScreenLayout.declineInviteButtonX(actualCardWidth), btnTop, btnSize)) {
            TeamManager.declineIncomingInvitation(senderId);
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
            showStatusMessage("Invitation declined");
        }
    }

    private void handleOutgoingInviteClick(UUID targetId, int relX, int actualCardWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Same width and anchor the renderer draws with, so the hit box cannot drift.
        int width = buttonWidth(INVITE_CARD_BUTTON_WIDTH,
                textRenderer.getWidth(InvitesTabRenderer.CANCEL_LABEL));
        int left = cancelInviteButtonX(actualCardWidth, width);

        if (relX >= left && relX < left + width) {
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
        statusRenderer.show(message);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only the left button drives this screen. Without this check a right or middle click fell
        // through to the card hit boxes and could kick or promote a member.
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        if (sidebarRenderer.getTabAt((int) mouseX, (int) mouseY, panelLeft, panelTop, sidebarWidth) instanceof Tab clickedTab) {
            switchTab(clickedTab);
            return true;
        }

        if (isOverScrollbar(mouseX, mouseY, panelLeft, panelTop)) {
            scrollController.startDragging((int) mouseY, scrollController.getScrollOffset());
            return true;
        }

        // Widgets (search field, Leave Team) win over the hand-rolled card hit boxes below, so
        // clicking a button can never also trigger the card drawn underneath it.
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (closeButtonBounds(panelLeft, panelTop).contains(mouseX, mouseY)) {
            close();
            return true;
        }

        if (isLeaveButtonVisible() && leaveButtonBounds(panelLeft, panelTop).contains(mouseX, mouseY)) {
            confirmLeaveTeam();
            return true;
        }

        // Cards only exist inside the scrollable viewport, so anything else (header, footer,
        // outside the panel) must not be treated as a click on a card.
        int contentLeft = panelLeft + sidebarWidth;
        int contentWidth = panelWidth - sidebarWidth;
        int scrollAreaTop = panelTop + scale(CONTENT_SCROLL_AREA_TOP);
        int scrollAreaBottom = scrollAreaTop + TeamScreenLayout.contentScrollHeight(panelHeight);

        if (mouseX < contentLeft || mouseX >= contentLeft + contentWidth
                || mouseY < scrollAreaTop || mouseY >= scrollAreaBottom) {
            return false;
        }

        int scrolledMouseY = (int) mouseY + scrollController.getScrollOffset();
        handleContentClick(contentLeft, scrollAreaTop, (int) mouseX, scrolledMouseY);
        return true;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY, int panelLeft, int panelTop) {
        if (scrollController.getMaxScroll() <= 0) return false;

        int contentLeft = panelLeft + sidebarWidth;
        int contentWidth = panelWidth - sidebarWidth;
        int scrollbarX = TeamScreenLayout.scrollbarX(contentLeft, contentWidth);
        int scrollAreaTop = panelTop + scale(CONTENT_SCROLL_AREA_TOP);
        int scrollAreaHeight = TeamScreenLayout.contentScrollHeight(panelHeight);

        return mouseX >= scrollbarX && mouseX < scrollbarX + scale(SCROLLBAR_WIDTH)
                && mouseY >= scrollAreaTop && mouseY < scrollAreaTop + scrollAreaHeight;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollController.isDragging() && scrollController.getMaxScroll() > 0) {
            return scrollController.handleDrag((int) mouseY,
                    TeamScreenLayout.contentScrollHeight(panelHeight),
                    scrollController.getMaxScroll());
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