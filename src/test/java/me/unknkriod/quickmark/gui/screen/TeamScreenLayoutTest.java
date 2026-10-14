package me.unknkriod.quickmark.gui.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the invariant that broke the team screen: a card is drawn clamped to the content
 * viewport, so every hit box has to be measured with that clamped width rather than with the
 * configured card width.
 */
class TeamScreenLayoutTest {

    private static final int PANEL = TeamScreenLayout.scale(TeamScreenLayout.BASE_PANEL_WIDTH);
    private static final int SIDEBAR = TeamScreenLayout.scale(TeamScreenLayout.BASE_SIDEBAR_WIDTH);
    private static final int CARD = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_WIDTH);
    private static final int ACTUAL = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
    private static final int BUTTON_TOP = TeamScreenLayout.scale(TeamScreenLayout.ACTION_BUTTONS_TOP_PADDING);
    private static final int BUTTON_MIDDLE = TeamScreenLayout.scale(TeamScreenLayout.ACTION_BUTTON_SIZE) / 2;

    @Test
    void cardWidthIsClampedToTheContentViewport() {
        int viewport = PANEL - SIDEBAR - TeamScreenLayout.scale(TeamScreenLayout.CARD_CONTENT_LEFT_PADDING) * 2;
        assertEquals(Math.min(CARD, viewport), ACTUAL);
    }

    @Test
    void actionButtonsAreHitWhereTheyAreDrawn() {
        int promoteX = TeamScreenLayout.promoteButtonX(ACTUAL);
        int kickX = TeamScreenLayout.kickButtonX(ACTUAL);

        assertTrue(TeamScreenLayout.isOverPromoteButton(promoteX + BUTTON_MIDDLE, BUTTON_TOP + BUTTON_MIDDLE, ACTUAL),
                "click on the drawn promote button must be accepted");
        assertTrue(TeamScreenLayout.isOverKickButton(kickX + BUTTON_MIDDLE, BUTTON_TOP + BUTTON_MIDDLE, ACTUAL),
                "click on the drawn kick button must be accepted");
    }

    @Test
    void hitBoxesDoNotOverlapEachOther() {
        int promoteX = TeamScreenLayout.promoteButtonX(ACTUAL);

        assertFalse(TeamScreenLayout.isOverKickButton(promoteX + BUTTON_MIDDLE, BUTTON_TOP + BUTTON_MIDDLE, ACTUAL),
                "the promote button must not be read as the kick button");
    }

    @Test
    void clickingOutsideTheButtonIsNotAHit() {
        int kickX = TeamScreenLayout.kickButtonX(ACTUAL);
        int size = TeamScreenLayout.scale(TeamScreenLayout.ACTION_BUTTON_SIZE);

        assertFalse(TeamScreenLayout.isOverKickButton(kickX - size, BUTTON_TOP + BUTTON_MIDDLE, ACTUAL));
        assertFalse(TeamScreenLayout.isOverKickButton(kickX + BUTTON_MIDDLE, BUTTON_TOP - size, ACTUAL));
    }

    @Test
    void measuringFromTheUnclampedCardWidthMissesTheDrawnButton() {
        // Regression for the original bug. Only meaningful when the layout actually clamps.
        if (ACTUAL == CARD) return;

        int staleKickX = TeamScreenLayout.kickButtonX(CARD);

        assertFalse(TeamScreenLayout.isOverKickButton(staleKickX + BUTTON_MIDDLE, BUTTON_TOP + BUTTON_MIDDLE, ACTUAL),
                "a hit box measured from cardWidth lands off the drawn button");
    }

    @Test
    void viewportHeightIsPanelHeightMinusBothPaddings() {
        int panelHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_PANEL_HEIGHT);
        int expected = panelHeight
                - TeamScreenLayout.scale(TeamScreenLayout.CONTENT_SCROLL_AREA_TOP)
                - TeamScreenLayout.scale(TeamScreenLayout.CONTENT_SCROLL_AREA_BOTTOM);

        assertEquals(expected, TeamScreenLayout.contentScrollHeight(panelHeight));
    }

    @Test
    void panelRightIsAScreenCoordinateNotAWidth() {
        int contentLeft = 300;
        int panelLeft = contentLeft - SIDEBAR;

        assertEquals(panelLeft + PANEL, TeamScreenLayout.panelRight(contentLeft, SIDEBAR, PANEL));
    }

    @Test
    void twoLineTextBlockIsCenteredInTheCard() {
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        int lineGap = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_LINE_GAP);

        int top = TeamScreenLayout.centeredTextBlockTop(cardHeight, fontHeight, lineGap);
        double blockCentre = top + (fontHeight * 2 + lineGap) / 2.0;
        double cardCentre = cardHeight / 2.0;

        // Only integer rounding may remain.
        assertTrue(Math.abs(blockCentre - cardCentre) <= 1.0,
                "text block centre " + blockCentre + " vs card centre " + cardCentre);
    }

    @Test
    void topPinnedLayoutWouldNotBeCentered() {
        // Regression: the invite cards used fixed top paddings (10 and 22), which left the block
        // 6.5 px above the centre of a 54 px card.
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        double oldBlockCentre = TeamScreenLayout.scale(10)
                + (TeamScreenLayout.scale(22) + fontHeight - TeamScreenLayout.scale(10)) / 2.0;

        assertTrue(cardHeight / 2.0 - oldBlockCentre > 5.0,
                "the old fixed paddings really were visibly off centre");
    }

    @Test
    void leaveButtonFitsOnTheFooterRow() {
        // Leave Team shares the footer row with Close and must fit inside the panel's right edge.
        int contentLeft = 300;
        int clipRight = TeamScreenLayout.panelRight(contentLeft, SIDEBAR, PANEL);
        int x = contentLeft + TeamScreenLayout.searchFieldSidePadding();
        int width = Math.min(TeamScreenLayout.scale(TeamScreenLayout.LEAVE_BUTTON_WIDTH), clipRight - x);

        assertTrue(width > TeamScreenLayout.scale(20), "wide enough to be drawn at all");
        assertTrue(x + width <= clipRight, "must not spill past the panel edge");
    }

    @Test
    void searchFieldMarginsAreEqualOnBothSides() {
        int contentWidth = PANEL - SIDEBAR;
        int margin = TeamScreenLayout.searchFieldSidePadding();
        int width = TeamScreenLayout.searchFieldWidth(contentWidth);

        assertEquals(margin, contentWidth - margin - width, "left and right margins must match");
        assertTrue(margin > 0, "there must be a margin on the right");
    }

    @Test
    void buttonLabelsFitAtEveryGuiScale() throws Exception {
        // The label is drawn at the client's fixed font height while the button's nominal width is
        // scaled, so on a small window the caption used to be trimmed. Drives currentScale directly
        // because it is the only way to exercise the scaling from a unit test.
        java.lang.reflect.Field scaleField = TeamScreenLayout.class.getDeclaredField("currentScale");
        scaleField.setAccessible(true);
        float original = scaleField.getFloat(null);
        int cancelText = 32;   // "Cancel" at the fixed font height
        int leaveText = 48;    // "Leave Team"

        try {
            for (float s : new float[] {0.4f, 0.5f, 0.75f, 1.0f, 1.75f}) {
                scaleField.setFloat(null, s);
                int padding = TeamScreenLayout.scale(TeamScreenLayout.BUTTON_LABEL_PADDING);

                assertTrue(TeamScreenLayout.buttonWidth(TeamScreenLayout.INVITE_CARD_BUTTON_WIDTH, cancelText)
                                - padding >= cancelText, "Cancel trimmed at scale " + s);
                assertTrue(TeamScreenLayout.buttonWidth(TeamScreenLayout.LEAVE_BUTTON_WIDTH, leaveText)
                                - padding >= leaveText, "Leave Team trimmed at scale " + s);
                assertTrue(TeamScreenLayout.buttonWidth(TeamScreenLayout.CLOSE_BUTTON_WIDTH, 30)
                                - padding >= 30, "Close trimmed at scale " + s);
            }

            // And the label really does widen the button where the nominal width is too small.
            scaleField.setFloat(null, 0.4f);
            assertTrue(TeamScreenLayout.buttonWidth(TeamScreenLayout.INVITE_CARD_BUTTON_WIDTH, cancelText)
                            > TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_BUTTON_WIDTH),
                    "the label must widen the button when the scale shrinks");
        } finally {
            scaleField.setFloat(null, original);
        }
    }

    @Test
    void widerCancelButtonStillFitsInsideTheCard() {
        int cardWidth = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
        int width = TeamScreenLayout.buttonWidth(TeamScreenLayout.INVITE_CARD_BUTTON_WIDTH, 32);
        int x = TeamScreenLayout.cancelInviteButtonX(cardWidth, width);

        assertTrue(x >= 0, "the button must stay inside the card");
        assertTrue(x + width <= cardWidth, "the button must not hang out of the card");
    }

    @Test
    void searchFieldSitsAboveTheTitleLineAndClearsTheSubtitle() {
        int fieldTop = TeamScreenLayout.scale(TeamScreenLayout.SEARCH_FIELD_TOP_PADDING);
        int fieldBottom = fieldTop + TeamScreenLayout.scale(TeamScreenLayout.SEARCH_FIELD_HEIGHT);
        int titleTop = TeamScreenLayout.scale(TeamScreenLayout.CONTENT_HEADER_TOP_PADDING);
        int subtitleTop = TeamScreenLayout.scale(TeamScreenLayout.CONTENT_HEADER_BOTTOM_PADDING);

        assertTrue(fieldTop < titleTop, "the field starts above the title's line");
        assertTrue(fieldBottom < subtitleTop, "the field must not reach the subtitle");
        assertTrue(subtitleTop - fieldBottom >= 2, "there must be a visible gap above the subtitle");
    }

    @Test
    void buttonLabelSitsWhereVanillaPutsIt() {
        // Vanilla ClickableWidget.drawScrollableText: (top + bottom - 9) / 2 + 1, with top = 0.
        int fontHeight = 9;

        for (int height : new int[] {9, 15, 18, 21, 24, 27, 33, 42}) {
            assertEquals((height - 9) / 2 + 1, TeamScreenLayout.buttonLabelY(0, height, fontHeight),
                    "vanilla y offset for a button " + height + " tall");
        }
    }

    @Test
    void oldFormulaLiftedOddHeightButtons() {
        // Regression: (height - 8) / 2 agreed with vanilla only for an even height; at an odd one it
        // put the caption one GUI unit high, which a large GUI scale makes obvious.
        int fontHeight = 9;

        for (int height : new int[] {15, 21, 27}) {
            int old = (height - 8) / 2;
            int vanilla = (height - 9) / 2 + 1;

            assertNotEquals(old, vanilla, "the old formula was off for a button " + height + " tall");
            assertEquals(vanilla, TeamScreenLayout.buttonLabelY(0, height, fontHeight));
        }
    }

    @Test
    void buttonLabelIsCentredOnTheButton() {
        for (int width : new int[] {60, 67, 90, 157}) {
            for (int text : new int[] {32, 47, 48, 49}) {
                int left = TeamScreenLayout.buttonLabelX(0, width, text);
                int leftMargin = left;
                int rightMargin = width - (left + text);

                assertTrue(Math.abs(leftMargin - rightMargin) <= 1,
                        "width " + width + " text " + text + ": margins " + leftMargin + "/" + rightMargin);
            }
        }
    }

    @Test
    void inviteCardHeadFitsTheLeftColumn() {
        int headSize = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_HEAD_SIZE);
        int headLeft = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_HEAD_LEFT_PADDING);
        int textLeft = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_TEXT_LEFT_PADDING);
        int cardWidth = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);

        // Both invite cards draw the head and the icon through the same helper, so this covers both.
        assertTrue(headLeft >= 0 && headLeft + headSize <= cardWidth, "the head must fit inside the card");
        assertTrue(headLeft + headSize <= textLeft, "the head must not run under the card's text");
        assertTrue(headSize <= cardHeight, "the head must fit the card's height");
    }

    @Test
    void inviteCardIconSitsOnTheRightCentredInTheCard() {
        int cardWidth = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        int textLeft = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_TEXT_LEFT_PADDING);
        int headLeft = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_HEAD_LEFT_PADDING);
        int headSize = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_HEAD_SIZE);

        // The glyph is drawn in a square slot as tall as the font, so that slot is its extent.
        int iconX = TeamScreenLayout.inviteCardIconX(cardWidth, fontHeight);
        int iconY = TeamScreenLayout.inviteCardCentredGlyphY(cardHeight, fontHeight);

        assertTrue(iconX + fontHeight <= cardWidth, "the icon must stay inside the card");
        assertTrue(iconX >= 0, "the icon must not start off the card");
        // Right of the head column, so the two never overlap.
        assertTrue(iconX >= headLeft + headSize, "the icon must clear the head");
        // Centred vertically: equal margins above and below.
        assertTrue(Math.abs(iconY - (cardHeight - (iconY + fontHeight))) <= 1,
                "the icon must be centred in the card");
        // And it is to the right of the text column, at the card's trailing edge.
        assertTrue(iconX > textLeft, "the icon belongs on the trailing side of the card");
    }

    @Test
    void inviteCardLinesHaveVisibleAirBetweenThem() {
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        int gap = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_LINE_GAP);

        int top = TeamScreenLayout.centeredTextBlockTop(cardHeight, fontHeight, gap);
        int second = top + fontHeight + gap;

        assertTrue(gap >= TeamScreenLayout.scale(6), "the two lines must not be cramped");
        assertTrue(second + fontHeight <= cardHeight, "the second line must stay inside the card");
    }

    @Test
    void inviteActionButtonsSitOnTheSecondTextLine() {
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        int lineGap = TeamScreenLayout.scale(TeamScreenLayout.INVITE_CARD_LINE_GAP);
        int buttonSize = TeamScreenLayout.scale(TeamScreenLayout.INVITE_ACTION_BUTTON_SIZE);

        int subtitleTop = TeamScreenLayout.centeredTextBlockTop(cardHeight, fontHeight, lineGap)
                + fontHeight + lineGap;
        int buttonTop = TeamScreenLayout.inviteActionButtonTop(cardHeight, fontHeight);

        double lineCentre = subtitleTop + fontHeight / 2.0;
        double buttonCentre = buttonTop + buttonSize / 2.0;

        assertTrue(Math.abs(buttonCentre - lineCentre) <= 1.0,
                "button centre " + buttonCentre + " vs subtitle centre " + lineCentre);
        assertTrue(buttonTop + buttonSize <= cardHeight, "button must stay inside the card");
        // And it really is lower than the card's midline, which is where it used to sit.
        assertTrue(buttonTop > (cardHeight - buttonSize) / 2, "button moved down onto the subtitle line");
    }

    @Test
    void inviteActionButtonsAreHitWhereTheyAreDrawn() {
        int cardHeight = TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT);
        int fontHeight = 9;
        int actualCardWidth = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
        int buttonSize = TeamScreenLayout.scale(TeamScreenLayout.INVITE_ACTION_BUTTON_SIZE);
        int buttonTop = TeamScreenLayout.inviteActionButtonTop(cardHeight, fontHeight);

        int acceptX = TeamScreenLayout.acceptInviteButtonX(actualCardWidth);
        int declineX = TeamScreenLayout.declineInviteButtonX(actualCardWidth);

        assertTrue(TeamScreenLayout.isOverButton(acceptX + 1, buttonTop + 1, acceptX, buttonTop, buttonSize),
                "click on the drawn accept button must be accepted");
        assertTrue(TeamScreenLayout.isOverButton(declineX + 1, buttonTop + 1, declineX, buttonTop, buttonSize),
                "click on the drawn decline button must be accepted");
        assertTrue(declineX < acceptX, "decline is drawn to the left of accept");
        assertTrue(declineX >= 0 && acceptX + buttonSize <= actualCardWidth, "both stay on the card");
    }

    @Test
    void declineIsNotHitAtTheOldAcceptPlusSpacingPosition() {
        // Regression: the click handler computed decline as accept + spacing while the renderer
        // drew it as accept - spacing, so the hit box sat 50 px away from the visible button.
        int actualCardWidth = TeamScreenLayout.cardContentWidth(PANEL, SIDEBAR, CARD);
        int buttonSize = TeamScreenLayout.scale(TeamScreenLayout.INVITE_ACTION_BUTTON_SIZE);
        int buttonTop = TeamScreenLayout.inviteActionButtonTop(
                TeamScreenLayout.scale(TeamScreenLayout.BASE_CARD_HEIGHT), 9);

        int acceptX = TeamScreenLayout.acceptInviteButtonX(actualCardWidth);
        int declineX = TeamScreenLayout.declineInviteButtonX(actualCardWidth);
        int staleX = acceptX + TeamScreenLayout.scale(TeamScreenLayout.INVITE_ACTION_BUTTON_SPACING);

        assertNotEquals(declineX, staleX);
        assertFalse(TeamScreenLayout.isOverButton(staleX + 1, buttonTop + 1, declineX, buttonTop, buttonSize),
                "the old accept-plus-spacing hit box lands off the drawn decline button");
    }

    @Test
    void modLogoIsAboutATabSizedButSlightlyBigger() {
        int sidebarWidth = TeamScreenLayout.scale(TeamScreenLayout.BASE_SIDEBAR_WIDTH);
        int logo = TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_LOGO_SIZE);
        int logoTop = TeamScreenLayout.sidebarLogoTop();
        int tabWidth = sidebarWidth - TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_TAB_INNER_PADDING);
        int tabHeight = TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_TAB_HEIGHT);
        int tabsStart = TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_TAB_START_TOP);

        assertTrue(logo > tabWidth && logo > tabHeight, "logo should be a little bigger than a tab");
        assertTrue(logo - tabWidth <= TeamScreenLayout.scale(2), "but only a little bigger than a tab");
        assertTrue(logo + logoTop * 2 <= sidebarWidth, "logo must fit across the sidebar");
        assertTrue(logoTop + logo < tabsStart, "logo must end above the first tab");
        // Centred in the space above the tabs, allowing for integer rounding.
        assertTrue(Math.abs((logoTop + logo) - (tabsStart - logoTop)) <= 1,
                "logo must sit centred in the gap above the tabs");
    }

    @Test
    void roundedCornerInsetsFollowTheQuarterCircle() {
        int radius = TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_LOGO_CORNER_RADIUS);
        int detail = 3;
        double[] insets = TeamScreenLayout.roundedCornerInsetFractions(radius, detail);

        assertEquals(radius * detail, insets.length);

        for (int row = 0; row < insets.length; row++) {
            double dy = radius - (row + 0.5) / detail;
            double expected = radius - Math.sqrt(Math.max(0.0, (double) radius * radius - dy * dy));

            assertEquals(expected, insets[row], 1.0e-9, "row " + row + " must sit on the arc");
            assertTrue(insets[row] >= 0 && insets[row] <= radius, "inset must stay within the radius");
            if (row > 0) {
                assertTrue(insets[row] <= insets[row - 1] + 1.0e-9, "the cut narrows down the corner");
            }
        }

        assertTrue(insets[0] > radius / 2.0, "the top of the arc cuts the most");
        assertTrue(insets[insets.length - 1] < 1.0, "the arc runs out at the bottom of the corner");
    }

    @Test
    void cornerMaskResolvesFinerThanWholeGuiUnits() {
        // The old whole-unit mask could only step once per GUI unit, which is what made the corners
        // look chamfered once the GUI scale grew.
        int radius = TeamScreenLayout.scale(TeamScreenLayout.SIDEBAR_LOGO_CORNER_RADIUS);

        double[] coarse = TeamScreenLayout.roundedCornerInsetFractions(radius, 1);
        double[] fine = TeamScreenLayout.roundedCornerInsetFractions(radius, 4);

        assertEquals(radius, coarse.length);
        assertEquals(radius * 4, fine.length);
        assertTrue(largestStep(fine) < largestStep(coarse), "finer detail must smooth the arc");

        // Every step is sub-unit except the top of the arc, where the circle runs tangent to the
        // tile's edge and the inset changes faster than a pixel per row.
        int subUnitSteps = 0;
        for (int row = 1; row < fine.length; row++) {
            if (fine[row - 1] - fine[row] <= 1.0) subUnitSteps++;
        }
        assertTrue(subUnitSteps >= fine.length - 2, "steps must be sub-unit but for the tangent point");
    }

    private static double largestStep(double[] values) {
        double largest = 0;
        for (int i = 1; i < values.length; i++) {
            largest = Math.max(largest, values[i - 1] - values[i]);
        }
        return largest;
    }

    @Test
    void roundedCornerInsetsHandleRadiusZero() {
        assertEquals(0, TeamScreenLayout.roundedCornerInsetFractions(0, 4).length);
    }

    @Test
    void emptyStateLinesAreSeparatedByTheLineGap() {
        int titleTop = TeamScreenLayout.scale(TeamScreenLayout.EMPTY_STATE_TITLE_TOP_PADDING);
        int fontHeight = 9;
        int gap = TeamScreenLayout.scale(TeamScreenLayout.EMPTY_STATE_LINE_GAP);
        int subtitleTop = TeamScreenLayout.emptyStateSubtitleTop(titleTop, fontHeight);

        assertEquals(fontHeight + gap, subtitleTop - titleTop,
                "the second line sits one font height plus the gap below the first");
        assertTrue(subtitleTop >= titleTop + fontHeight, "the two lines must not overlap");
        // The old fixed paddings (30 and 42) left only 3 units of blank between the lines.
        assertTrue(subtitleTop - (titleTop + fontHeight) > 3, "more air than the old fixed paddings");
    }
}
