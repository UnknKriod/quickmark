package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class TeamScreenLayout {

    // ───────────────────────────────────────────────
    //                  Base dimensions
    // ───────────────────────────────────────────────
    public static final int BASE_PANEL_WIDTH = 325;
    public static final int BASE_PANEL_HEIGHT = 390;
    public static final int BASE_CARD_WIDTH = 285;
    public static final int BASE_CARD_HEIGHT = 54;
    public static final int BASE_SIDEBAR_WIDTH = 60;
    public static final int BASE_CARD_SPACING = 6;
    public static final int SCROLLBAR_WIDTH = 4;
    /** Distance from the content area's right edge to the scrollbar's left edge. */
    public static final int SCROLLBAR_RIGHT_OFFSET = 6;

    // ───────────────────────────────────────────────
    //                  Border radii
    // ───────────────────────────────────────────────
    public static final int PANEL_BORDER_RADIUS = 9;
    public static final int CARD_BORDER_RADIUS = 6;
    public static final int BUTTON_BORDER_RADIUS = 3;
    public static final int PLAYER_CARD_BORDER_RADIUS = 4;
    public static final int SCROLLBAR_BORDER_RADIUS = 2;
    public static final int HEALTH_BAR_BORDER_RADIUS = 2;
    public static final int TAB_BORDER_RADIUS = 6;

    // ───────────────────────────────────────────────
    //                  UI element sizes & paddings
    // ───────────────────────────────────────────────
    /**
     * Margin of the search field and of the header text from the content area's edges. It sets the
     * margin on <em>both</em> sides, so the left and right margins are equal by construction and
     * cannot drift apart; the field's width is the content width less this on each side.
     */
    public static final int SEARCH_FIELD_SIDE_PADDING = 10;
    /**
     * Y of the search field. Above the title's line so the field cannot cover the title, and clear
     * of the subtitle, which stays on the same line as on the other tabs.
     */
    public static final int SEARCH_FIELD_TOP_PADDING = 14;
    public static final int SEARCH_FIELD_HEIGHT = 18;

    public static final int LEAVE_BUTTON_HEIGHT = 24;
    public static final int LEAVE_BUTTON_WIDTH = 90;

    public static final int SIDEBAR_TAB_START_TOP = 60;
    /** Height of a sidebar tab. Used for both drawing and hit testing. */
    public static final int SIDEBAR_TAB_HEIGHT = 42;
    /**
     * The mod logo sits above the tabs. A square a hair larger than a tab, which is drawn
     * {@code BASE_SIDEBAR_WIDTH - SIDEBAR_TAB_INNER_PADDING} wide by {@link #SIDEBAR_TAB_HEIGHT} tall.
     */
    public static final int SIDEBAR_LOGO_SIZE = 46;
    /** Corner radius applied to the logo tile. */
    public static final int SIDEBAR_LOGO_CORNER_RADIUS = 8;
    /**
     * Size in GUI units of one square block of the logo's rounded corner. The arc is built from a
     * grid of such blocks, so a larger value gives a coarser, blockier corner while the corner keeps
     * its overall size (the radius). 1 = block per GUI unit. Fixed, so the corner looks the same at
     * every window size.
     */
    public static final int SIDEBAR_LOGO_CORNER_STEP = 3;
    public static final int SIDEBAR_TAB_SPACING = 51;
    public static final int SIDEBAR_TAB_HORIZONTAL_PADDING = 7;
    public static final int SIDEBAR_TAB_INNER_PADDING = 15;
    public static final int SIDEBAR_ACTIVE_TAB_INDICATOR_WIDTH = 3;
    public static final int SIDEBAR_ACTIVE_TAB_INDICATOR_TOP_PADDING = 7;
    public static final int SIDEBAR_ACTIVE_TAB_INDICATOR_HEIGHT = 28;

    public static final int CONTENT_HEADER_TOP_PADDING = 21;
    public static final int CONTENT_HEADER_BOTTOM_PADDING = 35;
    public static final int CONTENT_SCROLL_AREA_TOP = 45;
    public static final int CONTENT_SCROLL_AREA_BOTTOM = 52;
    public static final int CONTENT_SCROLL_AREA_INNER_TOP_PADDING = 7;
    public static final int CONTENT_SECTION_SPACING = 15;
    public static final int CONTENT_SECTION_LABEL_SPACING = 7;

    public static final int CARD_CONTENT_LEFT_PADDING = 10;
    public static final int CARD_POSITION_INDICATOR_WIDTH = 3;
    public static final int CARD_POSITION_INDICATOR_TOP_PADDING = 6;

    public static final int PLAYER_HEAD_SIZE = 36;
    public static final int PLAYER_HEAD_LEFT_PADDING = 9;
    public static final int PLAYER_HEAD_TOP_PADDING = 9;
    public static final int PLAYER_INFO_LEFT_PADDING = 51;
    public static final int PLAYER_INFO_TOP_PADDING = 10;

    public static final int HEALTH_BAR_TOP_PADDING = 24;
    public static final int HEALTH_BAR_WIDTH = 150;
    public static final int HEALTH_BAR_HEIGHT = 4;
    public static final int HEALTH_TEXT_TOP_PADDING = 9;
    public static final int ABSORPTION_TEXT_LEFT_OFFSET = 75;

    public static final int ACTION_BUTTONS_RIGHT_PADDING = 60;
    public static final int ACTION_BUTTONS_TOP_PADDING = 17;
    public static final int ACTION_BUTTON_WIDTH = 41;
    public static final int ACTION_BUTTON_SIZE = 18;
    public static final int ACTION_BUTTON_SPACING = 25;

    // Invites tab
    // Card text is positioned by centring the two-line block in the card, so only the horizontal
    // paddings and the gap between the lines are fixed.
    public static final int INVITE_CARD_ICON_LEFT_PADDING = 12;
    public static final int INVITE_CARD_TEXT_LEFT_PADDING = 30;
    /** Blank space between the two lines of an invite card: the title and the subtitle below it. */
    public static final int INVITE_CARD_LINE_GAP = 6;

    public static final int INVITE_CARD_BUTTON_RIGHT_PADDING = 135;
    public static final int INVITE_CARD_BUTTON_WIDTH = 60;
    public static final int INVITE_CARD_BUTTON_HEIGHT = 21;
    public static final int INVITE_CARD_BUTTON_SPACING = 68;

    public static final int INVITE_ACTION_BUTTON_SIZE = 18;
    public static final int INVITE_ACTION_BUTTON_SPACING = 25;
    public static final int INVITE_ACTION_BUTTONS_RIGHT_PADDING = 45;

    // Players tab
    public static final int PLAYER_LIST_CARD_HEIGHT = 30;
    public static final int PLAYER_LIST_CARD_BORDER_RADIUS = 4;
    public static final int PLAYER_LIST_AVATAR_LEFT_PADDING = 6;
    public static final int PLAYER_LIST_AVATAR_TOP_PADDING = 6;
    public static final int PLAYER_LIST_AVATAR_SIZE = 18;
    public static final int PLAYER_LIST_NAME_LEFT_PADDING = 30;
    public static final int PLAYER_LIST_NAME_TOP_PADDING = 9;

    public static final int PLAYER_LIST_INVITE_BUTTON_RIGHT_PADDING = 20;
    public static final int PLAYER_LIST_INVITE_BUTTON_TOP_PADDING = 6;
    public static final int PLAYER_LIST_INVITE_BUTTON_HEIGHT = 18;
    public static final int PLAYER_LIST_INVITE_MIN_WIDTH = 70;
    public static final int PLAYER_LIST_INVITE_TEXT_PADDING = 24;
    public static final int PLAYER_LIST_INVITE_RESERVED_WIDTH = 90;
    public static final int PLAYER_LIST_ITEM_SPACING = 36;

    // Empty state / footer / close
    public static final int EMPTY_STATE_TOP_PADDING = 75;
    public static final int EMPTY_STATE_TITLE_TOP_PADDING = 30;
    /** Blank space between the empty state's title and the subtitle below it. */
    public static final int EMPTY_STATE_LINE_GAP = 6;
    public static final int EMPTY_STATE_ICON_SCALE = 2;

    public static final int FOOTER_TOP_MARGIN = 45;
    public static final int FOOTER_BORDER_HEIGHT = 1;
    public static final int FOOTER_BUTTON_HORIZONTAL_PADDING = 15;

    public static final int CLOSE_BUTTON_WIDTH = 67;
    public static final int CLOSE_BUTTON_HEIGHT = 24;
    public static final int CLOSE_BUTTON_RIGHT_PADDING = 82;
    public static final int CLOSE_BUTTON_TOP_PADDING = 9;

    // ───────────────────────────────────────────────
    //                       Colors
    // ───────────────────────────────────────────────
    public static final int BG_PRIMARY = 0xE01E1E26;
    public static final int BG_SECONDARY = 0xF02B2D36;
    public static final int BG_CARD = 0xF0353743;
    public static final int BG_CARD_HOVER = 0xF03F414E;
    public static final int ACCENT_PRIMARY = 0xFF5865F2;
    public static final int ACCENT_SUCCESS = 0xFF3BA55C;
    public static final int ACCENT_DANGER = 0xFFED4245;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xB3B9BBBE;
    public static final int BORDER_COLOR = 0x40FFFFFF;
    public static final int ABSORPTION_COLOR = 0xFFF59E0B;

    // ───────────────────────────────────────────────
    //                 Scale & animation
    // ───────────────────────────────────────────────
    public static final float WINDOW_SCALE_REFERENCE = 1080.0f;
    public static final float MAX_SCALE = 1.75f;
    public static final float MIN_SCALE = 0.75f;
    public static final float SCROLL_SPEED = 15.0f;
    public static final float HOVER_ANIMATION_SPEED = 4.0f;
    public static final float HEALTH_HIGH_THRESHOLD = 0.6f;
    public static final float HEALTH_MEDIUM_THRESHOLD = 0.3f;

    // ───────────────────────────────────────────────
    //              Derived geometry
    //
    // Everything a card is drawn with must come from here, so the
    // renderer and the click handler can never disagree about where a
    // card or its buttons are.
    // ───────────────────────────────────────────────

    /**
     * Width a card is actually drawn at: the configured card width clamped to the content viewport.
     * The tab renderers draw with this, and the click handler must use the same value.
     */
    public static int cardContentWidth(int panelWidth, int sidebarWidth, int cardWidth) {
        int availableWidth = panelWidth - sidebarWidth - scale(CARD_CONTENT_LEFT_PADDING) * 2;
        return Math.min(cardWidth, availableWidth);
    }

    /** Height of the scrollable content viewport inside the panel. */
    public static int contentScrollHeight(int panelHeight) {
        return panelHeight - scale(CONTENT_SCROLL_AREA_TOP) - scale(CONTENT_SCROLL_AREA_BOTTOM);
    }

    /**
     * Y offset of the first of two stacked text lines that centres their block inside a card.
     * <p>
     * {@code cardHeight} and {@code fontHeight} are both in the same scaled GUI space: the card
     * height is scaled by this class, the font height comes straight from the text renderer.
     * Shared so the invite cards cannot drift back to a top-pinned layout.
     */
    public static int centeredTextBlockTop(int cardHeight, int fontHeight, int lineGap) {
        return (cardHeight - (fontHeight * 2 + lineGap)) / 2;
    }

    /**
     * Y that centres a {@code elementHeight}-tall element on a text line starting at {@code lineTop}.
     * Used to put a card's action buttons on the same line as its second text row.
     */
    public static int centeredOnLine(int lineTop, int fontHeight, int elementHeight) {
        return lineTop + (fontHeight - elementHeight) / 2;
    }

    /** X of the search field and of the header text, relative to the content area's left edge. */
    public static int searchFieldSidePadding() {
        return scale(SEARCH_FIELD_SIDE_PADDING);
    }

    /**
     * Width of the search field: the content width less the margin on both sides, so the field's
     * left and right margins are equal by construction and cannot drift apart.
     */
    public static int searchFieldWidth(int contentWidth) {
        return contentWidth - searchFieldSidePadding() * 2;
    }

    /** Y of the empty state's second line: one font height below the title, plus the line gap.
     * Derived from the title rather than given its own padding, so changing the gap moves the
     * subtitle and the two can never drift into each other.
     */
    public static int emptyStateSubtitleTop(int titleTop, int fontHeight) {
        return titleTop + fontHeight + scale(EMPTY_STATE_LINE_GAP);
    }

    /** Whether a point, relative to a card's top-left corner, is inside a square button. */
    public static boolean isOverButton(int relX, int relY, int buttonX, int buttonY, int buttonSize) {
        return relX >= buttonX && relX < buttonX + buttonSize
                && relY >= buttonY && relY < buttonY + buttonSize;
    }

    /** Y offset of the logo, centred in the space above the first tab. */
    public static int sidebarLogoTop() {
        return (scale(SIDEBAR_TAB_START_TOP) - scale(SIDEBAR_LOGO_SIZE)) / 2;
    }

    /** X of an invite card's accept button, relative to the card's left edge. */
    public static int acceptInviteButtonX(int actualCardWidth) {
        return actualCardWidth - scale(INVITE_ACTION_BUTTONS_RIGHT_PADDING)
                - scale(INVITE_ACTION_BUTTON_SIZE);
    }

    /**
     * X of an invite card's decline button, relative to the card's left edge.
     * Derived from the accept button, so the two can never be laid out with opposite signs.
     */
    public static int declineInviteButtonX(int actualCardWidth) {
        return acceptInviteButtonX(actualCardWidth) - scale(INVITE_ACTION_BUTTON_SPACING);
    }

    /** Y of an invite card's action buttons: centred on the card's second text line. */
    public static int inviteActionButtonTop(int cardHeight, int fontHeight) {
        int lineGap = scale(INVITE_CARD_LINE_GAP);
        int subtitleTop = centeredTextBlockTop(cardHeight, fontHeight, lineGap) + fontHeight + lineGap;
        return centeredOnLine(subtitleTop, fontHeight, scale(INVITE_ACTION_BUTTON_SIZE));
    }

    /**
     * Block-grid mask of a rounded corner. The corner square (side {@code radius}) is split into
     * square blocks of {@code step} GUI units, aligned to the tile's corner; the last block of a row
     * or column is clipped to the radius. A block is cut away when its centre lies outside the quarter
     * circle, and the corner block is always cut, so the corner stays visibly rounded however coarse
     * the step is.
     * <p>
     * {@code result[block]} is the width, from the tile's edge, of the cut in the block row
     * {@code block}; the row covers {@code [block * step, min((block + 1) * step, radius))}.
     */
    public static int[] chunkyCornerSpans(int radius, int step) {
        if (radius <= 0) return new int[0];

        int size = Math.max(1, step);
        int blocks = (radius + size - 1) / size;
        int[] spans = new int[blocks];
        double radiusSquared = (double) radius * radius;

        for (int row = 0; row < blocks; row++) {
            double cy = (row * size + Math.min((row + 1) * size, radius)) / 2.0;

            for (int col = 0; col < blocks; col++) {
                int right = Math.min((col + 1) * size, radius);
                double cx = (col * size + right) / 2.0;
                double dx = radius - cx;
                double dy = radius - cy;
                if (dx * dx + dy * dy > radiusSquared) {
                    spans[row] = right;
                }
            }
        }

        spans[0] = Math.max(spans[0], Math.min(size, radius));
        return spans;
    }

    /**
     * Per-row horizontal inset of a rounded corner of the given radius.
     * <p>
     * {@code insets[row]} is how far the rounded edge is from the corner on that row, as a fraction
     * of a GUI unit. {@code detail} subdivides a GUI unit, so the arc can be resolved at physical
     * pixel resolution: a plain fill can never be thinner than one GUI unit, which is what left the
     * corners looking chamfered once the window — and with it the GUI scale — grew.
     */
    public static double[] roundedCornerInsetFractions(int radius, int detail) {
        int steps = Math.max(1, detail);
        int rows = Math.max(0, radius) * steps;
        double[] insets = new double[rows];

        for (int row = 0; row < rows; row++) {
            double dy = radius - (row + 0.5) / steps;
            double dxSquared = (double) radius * radius - dy * dy;
            insets[row] = dxSquared <= 0 ? radius : Math.max(0, radius - Math.sqrt(dxSquared));
        }

        return insets;
    }

    /** Left edge of the scrollbar, relative to the panel's content area. */
    public static int scrollbarX(int contentLeft, int contentWidth) {
        return contentLeft + contentWidth - scale(SCROLLBAR_RIGHT_OFFSET);
    }

    /**
     * Screen X of the panel's right edge. Buttons must not be drawn past it, and it is a screen
     * coordinate — not a width, which is what callers used to pass by mistake.
     */
    public static int panelRight(int contentLeft, int sidebarWidth, int panelWidth) {
        return contentLeft - sidebarWidth + panelWidth;
    }

    /** X of the promote (make leader) button, relative to a card's left edge. */
    public static int promoteButtonX(int actualCardWidth) {
        return actionButtonsBaseX(actualCardWidth);
    }

    /** X of the kick button, relative to a card's left edge. */
    public static int kickButtonX(int actualCardWidth) {
        return actionButtonsBaseX(actualCardWidth) + scale(ACTION_BUTTON_SPACING);
    }

    private static int actionButtonsBaseX(int actualCardWidth) {
        return actualCardWidth - scale(ACTION_BUTTONS_RIGHT_PADDING);
    }

    /** Whether a point, relative to a card's top-left corner, is on the promote button. */
    public static boolean isOverPromoteButton(int relX, int relY, int actualCardWidth) {
        return isOverActionButton(relX, relY, promoteButtonX(actualCardWidth));
    }

    /** Whether a point, relative to a card's top-left corner, is on the kick button. */
    public static boolean isOverKickButton(int relX, int relY, int actualCardWidth) {
        return isOverActionButton(relX, relY, kickButtonX(actualCardWidth));
    }

    private static boolean isOverActionButton(int relX, int relY, int buttonX) {
        int size = scale(ACTION_BUTTON_SIZE);
        int top = scale(ACTION_BUTTONS_TOP_PADDING);
        return relX >= buttonX && relX < buttonX + size && relY >= top && relY < top + size;
    }

    private static float currentScale = 1.0f;

    public static void calculateScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            currentScale = 1.0f;
            return;
        }

        int windowHeight = client.getWindow().getScaledHeight();
        int windowWidth = client.getWindow().getScaledWidth();

        float scaleFactor = MathHelper.clamp(windowHeight / WINDOW_SCALE_REFERENCE, MIN_SCALE, MAX_SCALE);

        if (BASE_PANEL_HEIGHT * scaleFactor > windowHeight - 20) {
            scaleFactor = (windowHeight - 20) / (float) BASE_PANEL_HEIGHT;
        }

        currentScale = scaleFactor;
    }

    public static int scale(int value) {
        return (int) (value * currentScale);
    }

    public static float scale(float value) {
        return value * currentScale;
    }

    public static float getCurrentScale() {
        return currentScale;
    }
}