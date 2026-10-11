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
    public static final int SEARCH_FIELD_LEFT_PADDING = 7;
    public static final int SEARCH_FIELD_TOP_PADDING = 17;
    public static final int SEARCH_FIELD_HEIGHT = 18;
    public static final int SEARCH_FIELD_HORIZONTAL_PADDING = 30;

    public static final int LEAVE_BUTTON_BOTTOM_MARGIN = 38;
    public static final int LEAVE_BUTTON_HEIGHT = 24;
    public static final int LEAVE_BUTTON_WIDTH = 90;

    public static final int SIDEBAR_TAB_START_TOP = 60;
    public static final int SIDEBAR_TAB_HEIGHT = 45;
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
    public static final int INVITE_CARD_ICON_LEFT_PADDING = 12;
    public static final int INVITE_CARD_ICON_TOP_PADDING = 9;
    public static final int INVITE_CARD_TEXT_LEFT_PADDING = 30;
    public static final int INVITE_CARD_TEXT_TOP_PADDING = 10;
    public static final int INVITE_CARD_SUBTEXT_TOP_PADDING = 22;

    public static final int INVITE_CARD_BUTTON_RIGHT_PADDING = 135;
    public static final int INVITE_CARD_BUTTON_TOP_PADDING = 18;
    public static final int INVITE_CARD_BUTTON_WIDTH = 60;
    public static final int INVITE_CARD_BUTTON_HEIGHT = 21;
    public static final int INVITE_CARD_BUTTON_SPACING = 68;

    public static final int INVITE_ACTION_BUTTON_SIZE = 18;
    public static final int INVITE_ACTION_BUTTON_SPACING = 25;
    public static final int INVITE_ACTION_BUTTONS_RIGHT_PADDING = 45;
    public static final int INVITE_ACTION_BUTTON_TOP_PADDING = 17;

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
    public static final int EMPTY_STATE_SUBTITLE_TOP_PADDING = 42;
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
    public static final float TAB_ANIMATION_SPEED = 8.0f;
    public static final float HEALTH_HIGH_THRESHOLD = 0.6f;
    public static final float HEALTH_MEDIUM_THRESHOLD = 0.3f;

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