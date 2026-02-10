package me.unknkriod.quickmark.gui.screen;

import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.network.NetworkSender;
import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class TeamManagementScreen extends Screen {
    private static final int BASE_PANEL_WIDTH = 325;
    private static final int BASE_PANEL_HEIGHT = 390;
    private static final int BASE_CARD_WIDTH = 285;
    private static final int BASE_CARD_HEIGHT = 54;
    private static final int BASE_SIDEBAR_WIDTH = 60;
    private static final int BASE_CARD_SPACING = 6;

    private static final int PANEL_BORDER_RADIUS = 9;
    private static final int CARD_BORDER_RADIUS = 6;
    private static final int BUTTON_BORDER_RADIUS = 3;
    private static final int PLAYER_CARD_BORDER_RADIUS = 4;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_BORDER_RADIUS = 2;
    private static final int HEALTH_BAR_BORDER_RADIUS = 2;
    private static final int TAB_BORDER_RADIUS = 6;
    private static final int EMPTY_STATE_ICON_SCALE = 2;

    private static final int SEARCH_FIELD_LEFT_PADDING = 7;
    private static final int SEARCH_FIELD_TOP_PADDING = 17;
    private static final int SEARCH_FIELD_HEIGHT = 18;
    private static final int SEARCH_FIELD_HORIZONTAL_PADDING = 30;
    private static final int LEAVE_BUTTON_BOTTOM_MARGIN = 38;
    private static final int LEAVE_BUTTON_HEIGHT = 24;
    private static final int LEAVE_BUTTON_WIDTH = 90;

    private static final int SIDEBAR_TAB_START_TOP = 60;
    private static final int SIDEBAR_TAB_HEIGHT = 45;
    private static final int SIDEBAR_TAB_SPACING = 51;
    private static final int SIDEBAR_TAB_HORIZONTAL_PADDING = 7;
    private static final int SIDEBAR_TAB_INNER_PADDING = 15;
    private static final int SIDEBAR_ACTIVE_TAB_INDICATOR_WIDTH = 3;
    private static final int SIDEBAR_ACTIVE_TAB_INDICATOR_TOP_PADDING = 7;
    private static final int SIDEBAR_ACTIVE_TAB_INDICATOR_HEIGHT = 28;

    private static final int CONTENT_HEADER_TOP_PADDING = 21;
    private static final int CONTENT_HEADER_BOTTOM_PADDING = 35;
    private static final int CONTENT_SCROLL_AREA_TOP = 45;
    private static final int CONTENT_SCROLL_AREA_BOTTOM = 52;
    private static final int CONTENT_SCROLL_AREA_INNER_TOP_PADDING = 7;
    private static final int CONTENT_SECTION_SPACING = 15;
    private static final int CONTENT_SECTION_LABEL_SPACING = 7;

    private static final int CARD_CONTENT_LEFT_PADDING = 10;
    private static final int CARD_POSITION_INDICATOR_WIDTH = 3;
    private static final int CARD_POSITION_INDICATOR_TOP_PADDING = 6;
    private static final int PLAYER_HEAD_SIZE = 36;
    private static final int PLAYER_HEAD_LEFT_PADDING = 9;
    private static final int PLAYER_HEAD_TOP_PADDING = 9;
    private static final int PLAYER_INFO_LEFT_PADDING = 51;
    private static final int PLAYER_INFO_TOP_PADDING = 10;
    private static final int HEALTH_BAR_TOP_PADDING = 24;
    private static final int HEALTH_BAR_WIDTH = 150;
    private static final int HEALTH_BAR_HEIGHT = 4;
    private static final int HEALTH_TEXT_TOP_PADDING = 9;
    private static final int ABSORPTION_TEXT_LEFT_OFFSET = 75;

    private static final int ACTION_BUTTONS_RIGHT_PADDING = 60;
    private static final int ACTION_BUTTONS_TOP_PADDING = 17;
    private static final int ACTION_BUTTON_WIDTH = 41;
    private static final int ACTION_BUTTON_SIZE = 18;
    private static final int ACTION_BUTTON_SPACING = 25;

    private static final int INVITE_CARD_ICON_LEFT_PADDING = 12;
    private static final int INVITE_CARD_ICON_TOP_PADDING = 9;
    private static final int INVITE_CARD_TEXT_LEFT_PADDING = 30;
    private static final int INVITE_CARD_TEXT_TOP_PADDING = 10;
    private static final int INVITE_CARD_SUBTEXT_TOP_PADDING = 22;

    private static final int INVITE_CARD_BUTTON_RIGHT_PADDING = 135;
    private static final int INVITE_CARD_BUTTON_TOP_PADDING = 18;
    private static final int INVITE_CARD_BUTTON_WIDTH = 60;
    private static final int INVITE_CARD_BUTTON_HEIGHT = 21;
    private static final int INVITE_CARD_BUTTON_SPACING = 68;

    private static final int INVITE_ACTION_BUTTON_SIZE = 18;
    private static final int INVITE_ACTION_BUTTON_SPACING = 25;
    private static final int INVITE_ACTION_BUTTONS_RIGHT_PADDING = 45;
    private static final int INVITE_ACTION_BUTTON_TOP_PADDING = 17;

    private static final int PLAYER_LIST_CARD_HEIGHT = 30;
    private static final int PLAYER_LIST_CARD_BORDER_RADIUS = 4;
    private static final int PLAYER_LIST_AVATAR_LEFT_PADDING = 6;
    private static final int PLAYER_LIST_AVATAR_TOP_PADDING = 6;
    private static final int PLAYER_LIST_AVATAR_SIZE = 18;
    private static final int PLAYER_LIST_NAME_LEFT_PADDING = 30;
    private static final int PLAYER_LIST_NAME_TOP_PADDING = 9;

    private static final int PLAYER_LIST_INVITE_BUTTON_RIGHT_PADDING = 20;
    private static final int PLAYER_LIST_INVITE_BUTTON_TOP_PADDING = 6;
    private static final int PLAYER_LIST_INVITE_BUTTON_HEIGHT = 18;
    private static final int PLAYER_LIST_INVITE_MIN_WIDTH = 70;
    private static final int PLAYER_LIST_INVITE_TEXT_PADDING = 24;
    private static final int PLAYER_LIST_INVITE_RESERVED_WIDTH = 90; // для обрезки имени (чтобы оставалось место под кнопку)
    private static final int PLAYER_LIST_ITEM_SPACING = 36;

    private static final int EMPTY_STATE_TOP_PADDING = 75;
    private static final int EMPTY_STATE_TITLE_TOP_PADDING = 30;
    private static final int EMPTY_STATE_SUBTITLE_TOP_PADDING = 42;

    private static final int FOOTER_TOP_MARGIN = 45;
    private static final int FOOTER_BORDER_HEIGHT = 1;
    private static final int FOOTER_BUTTON_HORIZONTAL_PADDING = 15;

    private static final int CLOSE_BUTTON_WIDTH = 67;
    private static final int CLOSE_BUTTON_HEIGHT = 24;
    private static final int CLOSE_BUTTON_RIGHT_PADDING = 82;
    private static final int CLOSE_BUTTON_TOP_PADDING = 9;

    // Colors
    private static final int BG_PRIMARY = 0xE0_1E_1E_26;
    private static final int BG_SECONDARY = 0xF0_2B_2D_36;
    private static final int BG_CARD = 0xF0_35_37_43;
    private static final int BG_CARD_HOVER = 0xF0_3F_41_4E;
    private static final int ACCENT_PRIMARY = 0xFF_5865_F2;
    private static final int ACCENT_SUCCESS = 0xFF_3BA55C;
    private static final int ACCENT_DANGER = 0xFF_ED4245;
    private static final int TEXT_PRIMARY = 0xFF_FFFFFF;
    private static final int TEXT_SECONDARY = 0xB3_B9BBBE;
    private static final int BORDER_COLOR = 0x40_FFFFFF;
    private static final int ABSORPTION_COLOR = 0xFF_F59E0B;

    // Other
    private static final float WINDOW_SCALE_REFERENCE = 1080.0f;
    private static final float MAX_SCALE = 1.75f;
    private static final float MIN_SCALE = 0.75f;
    private static final float SCROLL_SPEED = 15.0f;
    private static final float HOVER_ANIMATION_SPEED = 4.0f;
    private static final float TAB_ANIMATION_SPEED = 8.0f;
    private static final float HEALTH_HIGH_THRESHOLD = 0.6f;
    private static final float HEALTH_MEDIUM_THRESHOLD = 0.3f;

    private final Map<UUID, Float> hoverAnimations = new HashMap<>();
    private final Map<Tab, Float> tabAnimations = new HashMap<>();
    private Tab currentTab = Tab.TEAM;
    private TextFieldWidget searchField;
    private ButtonWidget leaveButton;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragStartY = 0;
    private int scrollbarDragStartOffset = 0;
    private float scale = 1.0f;
    private int panelWidth, panelHeight, cardWidth, cardHeight,
            sidebarWidth, cardSpacing, scrollbarWidth;

    private String statusMessage = null;
    private long statusMessageTime = 0;
    private float statusMessageAlpha = 0f;
    private static final float STATUS_FADE_SPEED = 3f;
    private static final long STATUS_MESSAGE_DURATION = 3000;

    public TeamManagementScreen() {
        super(Text.literal("Team Management"));
        for (Tab tab : Tab.values()) {
            tabAnimations.put(tab, tab == currentTab ? 1.0f : 0.0f);
        }
        calculateScale();
    }

    private void calculateScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            scale = 1.0f;
            return;
        }

        int windowHeight = client.getWindow().getScaledHeight();
        int windowWidth = client.getWindow().getScaledWidth();

        float scaleFactor = MathHelper.clamp(windowHeight / WINDOW_SCALE_REFERENCE, MIN_SCALE, MAX_SCALE);

        if (BASE_PANEL_HEIGHT * scaleFactor > windowHeight - 20) {
            scaleFactor = (windowHeight - 20) / (float) BASE_PANEL_HEIGHT;
        }

        this.scale = scaleFactor;

        panelWidth = scale(BASE_PANEL_WIDTH);
        panelHeight = scale(BASE_PANEL_HEIGHT);

        if (panelWidth > windowWidth - scale(40)) {
            panelWidth = windowWidth - scale(40);
        }

        sidebarWidth = scale(BASE_SIDEBAR_WIDTH);

        int maxCardWidth = panelWidth - sidebarWidth - scale(CARD_CONTENT_LEFT_PADDING) * 2;
        cardWidth = Math.min(scale(BASE_CARD_WIDTH), maxCardWidth);

        cardHeight = scale(BASE_CARD_HEIGHT);
        cardSpacing = scale(BASE_CARD_SPACING);
        scrollbarWidth = scale(SCROLLBAR_WIDTH);
    }

    private int scale(int value) {
        return (int) (value * scale);
    }

    private float scale(float value) {
        return value * scale;
    }

    @Override
    protected void init() {
        super.init();

        calculateScale();

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
            leaveButton.visible = currentTab == Tab.TEAM && members.size() > 1;
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

        renderSidebar(context, panelLeft, panelTop, mouseX, mouseY, delta);

        int contentLeft = panelLeft + sidebarWidth;
        int contentTop = panelTop;
        int contentWidth = panelWidth - sidebarWidth;

        renderHeader(context, contentLeft, contentTop, contentWidth);

        int hoverMouseY = mouseY + scrollOffset;

        context.enableScissor(contentLeft, contentTop + scale(CONTENT_SCROLL_AREA_TOP),
                contentLeft + contentWidth, contentTop + panelHeight - scale(CONTENT_SCROLL_AREA_BOTTOM));
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, -scrollOffset);

        int scrollContentTop = contentTop + scale(CONTENT_SCROLL_AREA_TOP);
        switch (currentTab) {
            case TEAM -> renderTeamTab(context, contentLeft, scrollContentTop,
                    mouseX, mouseY + scrollOffset, delta);
            case INVITES -> renderInvitesTab(context, contentLeft, scrollContentTop,
                    mouseX, mouseY + scrollOffset, delta);
            case PLAYERS -> renderPlayersTab(context, contentLeft, scrollContentTop,
                    mouseX, hoverMouseY, delta);
        }

        context.getMatrices().popMatrix();
        context.disableScissor();

        if (maxScroll > 0) {
            renderScrollbar(context, contentLeft + contentWidth - scale(6),
                    contentTop + scale(CONTENT_SCROLL_AREA_TOP),
                    panelHeight - scale(98), mouseX, mouseY);
        }

        renderFooter(context, contentLeft, contentTop + panelHeight - scale(FOOTER_TOP_MARGIN), contentWidth);

        renderStatusMessage(context, mouseX, mouseY, delta);

        updateAnimations(delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        drawRoundedRect(context, x, y, sidebarWidth, panelHeight,
                scale(PANEL_BORDER_RADIUS), BG_SECONDARY);

        int tabY = y + scale(SIDEBAR_TAB_START_TOP);
        for (Tab tab : Tab.values()) {
            boolean isHovered = mouseX >= x && mouseX < x + sidebarWidth &&
                    mouseY >= tabY && mouseY < tabY + scale(SIDEBAR_TAB_HEIGHT);
            boolean isActive = tab == currentTab;

            float animation = tabAnimations.getOrDefault(tab, 0f);

            int tabColor = isActive ? ACCENT_PRIMARY :
                    (isHovered ? BG_CARD_HOVER : BG_CARD);
            int alpha = (int) (255 * (isActive ? 1.0f : (isHovered ? 0.8f : 0.5f)));
            tabColor = (tabColor & 0x00FFFFFF) | (alpha << 24);

            drawRoundedRect(context, x + scale(SIDEBAR_TAB_HORIZONTAL_PADDING), tabY,
                    sidebarWidth - scale(SIDEBAR_TAB_INNER_PADDING),
                    scale(42), scale(TAB_BORDER_RADIUS), tabColor);

            if (isActive) {
                context.fill(x + scale(SIDEBAR_ACTIVE_TAB_INDICATOR_WIDTH),
                        tabY + scale(SIDEBAR_ACTIVE_TAB_INDICATOR_TOP_PADDING),
                        x + scale(4),
                        tabY + scale(SIDEBAR_ACTIVE_TAB_INDICATOR_HEIGHT),
                        ACCENT_PRIMARY);
            }

            String icon = tab.icon;
            int iconWidth = textRenderer.getWidth(icon);
            context.drawText(textRenderer, icon,
                    x + (sidebarWidth - iconWidth) / 2, tabY + scale(9),
                    isActive ? TEXT_PRIMARY : TEXT_SECONDARY, false);

            String label = tab.label;
            int labelWidth = textRenderer.getWidth(label);
            context.drawText(textRenderer, label,
                    x + (sidebarWidth - labelWidth) / 2, tabY + scale(24),
                    isActive ? TEXT_PRIMARY : TEXT_SECONDARY, true);

            tabY += scale(SIDEBAR_TAB_SPACING);
        }
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

    private void renderTeamTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        List<TeamPlayer> members = TeamManager.getTeamMembers();
        MinecraftClient client = MinecraftClient.getInstance();

        if (members.isEmpty()) {
            renderEmptyState(context, x, y, "No team members", "Start by inviting players");
            return;
        }

        int availableWidth = panelWidth - sidebarWidth - scale(CARD_CONTENT_LEFT_PADDING) * 2;
        int actualCardWidth = Math.min(cardWidth, availableWidth);

        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        maxScroll = Math.max(0, members.size() * (cardHeight + cardSpacing) -
                (panelHeight - scale(98)));

        for (int i = 0; i < members.size(); i++) {
            TeamPlayer player = members.get(i);
            boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                    mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                    mouseY >= cardY && mouseY < cardY + cardHeight;

            renderTeamMemberCard(context, player, i,
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY,
                    actualCardWidth, isHovered, mouseX, mouseY, delta);
            cardY += cardHeight + cardSpacing;
        }
    }

    private void renderTeamMemberCard(DrawContext context, TeamPlayer player, int position,
                                      int x, int y, int actualWidth, boolean isHovered,
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
            renderActionButtons(context, playerId, isLeader,
                    x + actualWidth - scale(ACTION_BUTTONS_RIGHT_PADDING),
                    y + scale(ACTION_BUTTONS_TOP_PADDING), actualWidth, isHovered, mouseX, mouseY);
        }
    }

    private void renderActionButtons(DrawContext context, UUID playerId, boolean isPlayerLeader,
                                     int x, int y, int cardWidth, boolean parentHovered,
                                     int mouseX, int mouseY) {
        if (!parentHovered) return;

        int buttonSize = scale(ACTION_BUTTON_SIZE);
        int leadX = x;
        int kickX = x + scale(ACTION_BUTTON_SPACING);

        if (!isPlayerLeader) {
            drawIconButton(context, leadX, y, buttonSize, "⬆", ACCENT_SUCCESS);
            if (mouseInRect(mouseX, mouseY, leadX, y, buttonSize, buttonSize)) {
                context.drawTooltip(this.textRenderer, Text.literal("Make leader"), mouseX, mouseY);
            }
        }

        drawIconButton(context, kickX, y, buttonSize, "✕", ACCENT_DANGER);
        if (mouseInRect(mouseX, mouseY, kickX, y, buttonSize, buttonSize)) {
            context.drawTooltip(this.textRenderer, Text.literal("Kick from team"), mouseX, mouseY);
        }
    }

    private void drawIconButton(DrawContext context, int x, int y, int size, String icon, int color) {
        int panelRightEdge = this.width / 2 + this.panelWidth / 2;
        int actualSize = Math.min(size, panelRightEdge - x);

        if (actualSize > 0) {
            drawRoundedRect(context, x, y, actualSize, actualSize, scale(BUTTON_BORDER_RADIUS), color);
            int iconWidth = textRenderer.getWidth(icon);
            int iconX = x + (actualSize - iconWidth) / 2;
            int iconY = y + (actualSize - textRenderer.fontHeight) / 2 + 1;
            context.drawText(textRenderer, icon, iconX, iconY, TEXT_PRIMARY, false);
        }
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height,
                            String text, int color) {
        int panelRightEdge = this.width / 2 + this.panelWidth / 2;

        if (x + width > panelRightEdge) {
            width = panelRightEdge - x;
        }

        if (width > scale(20)) {
            drawRoundedRect(context, x, y, width, height, scale(BUTTON_BORDER_RADIUS), color);
            String trimmedText = textRenderer.trimToWidth(text, width - scale(10));
            int textWidth = textRenderer.getWidth(trimmedText);
            context.drawText(textRenderer, trimmedText,
                    x + (width - textWidth) / 2, y + (height - 8) / 2, TEXT_PRIMARY, false);
        }
    }

    private void renderInvitesTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        Map<UUID, String> incoming = TeamManager.getIncomingInvitations();
        Map<UUID, Map.Entry<String, Long>> outgoing = TeamManager.getOutgoingInvitationsWithTime();

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            renderEmptyState(context, x, y, "No pending invitations", "Invites will appear here");
            return;
        }

        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        maxScroll = Math.max(0, (incoming.size() + outgoing.size()) * (cardHeight + cardSpacing) -
                (panelHeight - scale(98)));

        if (!incoming.isEmpty()) {
            context.drawText(textRenderer, "📥 Incoming",
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY, TEXT_SECONDARY, true);
            cardY += scale(CONTENT_SECTION_SPACING);

            for (Map.Entry<UUID, String> entry : incoming.entrySet()) {
                boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                        mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + cardWidth &&
                        mouseY >= cardY && mouseY < cardY + cardHeight;

                renderIncomingInviteCard(context, entry.getKey(), entry.getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY, mouseX, mouseY, isHovered);
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

                renderOutgoingInviteCard(context, entry.getKey(), entry.getValue().getKey(),
                        entry.getValue().getValue(),
                        x + scale(CARD_CONTENT_LEFT_PADDING), cardY, isHovered);
                cardY += cardHeight + cardSpacing;
            }
        }
    }

    private void renderIncomingInviteCard(DrawContext context, UUID senderId, String senderName,
                                          int x, int y, int mouseX, int mouseY, boolean isHovered) {
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
            int declineX = btnRight - btnSize + scale(INVITE_ACTION_BUTTON_SPACING);

            drawIconButton(context, acceptX, btnTop, btnSize, "✓", ACCENT_SUCCESS);
            if (mouseInRect(mouseX, mouseY, acceptX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Accept invitation"), mouseX, mouseY);
            }

            drawIconButton(context, declineX, btnTop, btnSize, "✕", ACCENT_DANGER);
            if (mouseInRect(mouseX, mouseY, declineX, btnTop, btnSize, btnSize)) {
                context.drawTooltip(textRenderer, Text.literal("Decline invitation"), mouseX, mouseY);
            }
        }
    }

    private boolean mouseInRect(int mouseX, int mouseY, int rx, int ry, int rw, int rh) {
        return mouseX >= rx && mouseX < rx + rw &&
                mouseY >= ry && mouseY < ry + rh;
    }

    private void renderOutgoingInviteCard(DrawContext context, UUID targetId, String targetName,
                                          long remainingTime, int x, int y, boolean isHovered) {
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
                scale(INVITE_CARD_BUTTON_WIDTH), scale(INVITE_CARD_BUTTON_HEIGHT),
                "Cancel", ACCENT_DANGER);
    }

    private void renderPlayersTab(DrawContext context, int x, int y, int mouseX, int hoverMouseY, float delta) {
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
            renderEmptyState(context, x, y, msg, "Try a different search");
            return;
        }

        int availableWidth = panelWidth - sidebarWidth - scale(CARD_CONTENT_LEFT_PADDING) * 2;
        int actualCardWidth = Math.min(cardWidth, availableWidth);
        int cardY = y + scale(CONTENT_SCROLL_AREA_INNER_TOP_PADDING);
        maxScroll = Math.max(0, players.size() * scale(PLAYER_LIST_ITEM_SPACING) -
                (panelHeight - scale(98)));

        for (PlayerListEntry entry : players) {
            boolean isHovered = mouseX >= x + scale(CARD_CONTENT_LEFT_PADDING) &&
                    mouseX < x + scale(CARD_CONTENT_LEFT_PADDING) + actualCardWidth &&
                    hoverMouseY >= cardY && hoverMouseY < cardY + scale(PLAYER_LIST_CARD_HEIGHT);
            renderPlayerListCard(context, entry,
                    x + scale(CARD_CONTENT_LEFT_PADDING), cardY,
                    actualCardWidth, isHovered);
            cardY += scale(PLAYER_LIST_ITEM_SPACING);
        }
    }

    private void renderPlayerListCard(DrawContext context, PlayerListEntry entry,
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

            // Защита от выхода за границы
            if (buttonX < x + scale(10)) {
                buttonX = x + scale(10);
                buttonWidth = actualWidth - rightPad - scale(10);
            }

            drawButton(context, buttonX,
                    y + scale(PLAYER_LIST_INVITE_BUTTON_TOP_PADDING),
                    buttonWidth,
                    scale(PLAYER_LIST_INVITE_BUTTON_HEIGHT),
                    buttonText, ACCENT_PRIMARY);
        }
    }

    private void renderEmptyState(DrawContext context, int x, int y, String title, String subtitle) {
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
                "Close", BG_CARD_HOVER);
    }

    private void renderScrollbar(DrawContext context, int x, int y, int height, int mouseX, int mouseY) {
        drawRoundedRect(context, x, y, scale(SCROLLBAR_WIDTH), height,
                scale(SCROLLBAR_BORDER_RADIUS), 0x40_FFFFFF);

        float scrollPercent = (float) scrollOffset / maxScroll;
        int thumbHeight = Math.max(scale(15), (int) (height * 0.3f));
        int thumbY = y + (int) ((height - thumbHeight) * scrollPercent);

        boolean isHovered = mouseX >= x && mouseX < x + scale(SCROLLBAR_WIDTH) &&
                mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        int thumbColor = isHovered || isDraggingScrollbar ? 0x80_FFFFFF : 0x60_FFFFFF;

        drawRoundedRect(context, x, thumbY, scale(SCROLLBAR_WIDTH), thumbHeight,
                scale(SCROLLBAR_BORDER_RADIUS), thumbColor);
    }

    private void renderPlayerHead(DrawContext context, TeamPlayer player, int x, int y, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        TeamHudRenderer.renderHead(context, player.getPlayerId(), x, y, size);
    }

    private void renderStatusMessage(DrawContext context, int mouseX, int mouseY, float delta) {
        if (statusMessage != null) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < STATUS_MESSAGE_DURATION) {
                // Плавное появление/исчезание
                if (elapsed < 500) {
                    statusMessageAlpha = Math.min(1f, statusMessageAlpha + delta * STATUS_FADE_SPEED);
                } else if (elapsed > STATUS_MESSAGE_DURATION - 500) {
                    statusMessageAlpha = Math.max(0f, statusMessageAlpha - delta * STATUS_FADE_SPEED);
                } else {
                    statusMessageAlpha = 1f;
                }

                int centerX = width / 2;
                int panelTop = height / 2 - panelHeight / 2;
                int messageY = panelTop - scale(30);

                int messageWidth = textRenderer.getWidth(statusMessage) + scale(20);
                int messageX = centerX - messageWidth / 2;

                // Альфа-канал для плавности
                int bgAlpha = (int)(0xCC * statusMessageAlpha);
                int textAlpha = (int)(0xFF * statusMessageAlpha);

                drawRoundedRect(context, messageX, messageY - scale(5),
                        messageWidth, scale(25),
                        scale(4), (bgAlpha << 24) | 0x1E1E26);

                context.drawText(textRenderer, statusMessage,
                        centerX - textRenderer.getWidth(statusMessage) / 2,
                        messageY, (textAlpha << 24) | 0xFFFFFF, false);
            } else {
                statusMessage = null;
                statusMessageAlpha = 0f;
            }
        }
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height,
                                 int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        context.fill(x + radius, y, x + radius + scale(1), y + radius, color);
        context.fill(x + width - radius - scale(1), y, x + width - radius, y + radius, color);
        context.fill(x + radius, y + height - radius, x + radius + scale(1), y + height, color);
        context.fill(x + width - radius - scale(1), y + height - radius, x + width - radius, y + height, color);
    }

    private int getHealthColor(float percent) {
        if (percent > HEALTH_HIGH_THRESHOLD) return ACCENT_SUCCESS;
        if (percent > HEALTH_MEDIUM_THRESHOLD) return 0xFF_F59E0B;
        return ACCENT_DANGER;
    }

    private int lerpColor(int color1, int color2, float t) {
        t = MathHelper.clamp(t, 0, 1);
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateAnimations(float delta) {
        for (Tab tab : Tab.values()) {
            float target = tab == currentTab ? 1.0f : 0.0f;
            float current = tabAnimations.getOrDefault(tab, 0f);
            float newValue = current + (target - current) * delta * TAB_ANIMATION_SPEED;
            tabAnimations.put(tab, newValue);
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        int tabY = panelTop + scale(SIDEBAR_TAB_START_TOP);
        for (Tab tab : Tab.values()) {
            if (mouseX >= panelLeft && mouseX < panelLeft + sidebarWidth &&
                    mouseY >= tabY && mouseY < tabY + scale(SIDEBAR_TAB_HEIGHT)) {
                switchTab(tab);
                return true;
            }
            tabY += scale(SIDEBAR_TAB_SPACING);
        }

        int contentLeft = panelLeft + sidebarWidth;
        int scrollbarX = contentLeft + (panelWidth - sidebarWidth) - scale(6);
        int scrollbarY = panelTop + scale(CONTENT_SCROLL_AREA_TOP);
        int scrollbarHeight = panelHeight - scale(98);

        if (maxScroll > 0 && mouseX >= scrollbarX && mouseX < scrollbarX + scale(SCROLLBAR_WIDTH) &&
                mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
            isDraggingScrollbar = true;
            scrollbarDragStartY = (int) mouseY;
            scrollbarDragStartOffset = scrollOffset;
            return true;
        }

        if (mouseX >= contentLeft + (panelWidth - sidebarWidth) - scale(CLOSE_BUTTON_RIGHT_PADDING) &&
                mouseX < contentLeft + (panelWidth - sidebarWidth) - scale(FOOTER_BUTTON_HORIZONTAL_PADDING) &&
                mouseY >= panelTop + panelHeight - scale(34) &&
                mouseY < panelTop + panelHeight - scale(10)) {
            close();
            return true;
        }

        int adjustedMouseY = (int) mouseY + scrollOffset;
        handleContentClick(contentLeft, panelTop + scale(CONTENT_SCROLL_AREA_TOP),
                (int) mouseX, adjustedMouseY);

        return super.mouseClicked(mouseX, mouseY, button);
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
        scrollOffset = 0;
        updateVisibility();
    }

    private void showStatusMessage(String message) {
        this.statusMessage = message;
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int centerY = height / 2;
            int panelTop = centerY - panelHeight / 2;
            int scrollbarHeight = panelHeight - scale(98);

            int deltaMove = (int) mouseY - scrollbarDragStartY;
            float scrollPercent = (float) deltaMove / scrollbarHeight;
            scrollOffset = MathHelper.clamp(scrollbarDragStartOffset + (int) (maxScroll * scrollPercent), 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * scale(SCROLL_SPEED)), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tab {
        TEAM("👥", "Team"),
        INVITES("📨", "Invites"),
        PLAYERS("➕", "Invite");

        final String icon;
        final String label;

        Tab(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
    }
}