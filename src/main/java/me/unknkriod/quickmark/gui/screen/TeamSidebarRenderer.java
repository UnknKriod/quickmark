package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import me.unknkriod.quickmark.gui.GuiComponent;

import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;
import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.drawRoundedRect;

public class TeamSidebarRenderer {

    public enum Tab {
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

    private final TextRenderer textRenderer;

    public TeamSidebarRenderer() {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }

    public void renderSidebar(DrawContext context, int x, int y, int mouseX, int mouseY,
                              Tab currentTab, int sidebarWidth, int panelHeight) {
        drawRoundedRect(context, x, y, sidebarWidth, panelHeight,
                scale(PANEL_BORDER_RADIUS), BG_SECONDARY);

        renderLogo(context, x, y, sidebarWidth);

        int tabY = y + scale(SIDEBAR_TAB_START_TOP);
        for (Tab tab : Tab.values()) {
            boolean isHovered = mouseX >= x && mouseX < x + sidebarWidth &&
                    mouseY >= tabY && mouseY < tabY + scale(SIDEBAR_TAB_HEIGHT);
            boolean isActive = tab == currentTab;

            int tabColor = isActive ? ACCENT_PRIMARY :
                    (isHovered ? BG_CARD_HOVER : BG_CARD);
            int alpha = (int) (255 * (isActive ? 1.0f : (isHovered ? 0.8f : 0.5f)));
            tabColor = (tabColor & 0x00FFFFFF) | (alpha << 24);

            drawRoundedRect(context, x + scale(SIDEBAR_TAB_HORIZONTAL_PADDING), tabY,
                    sidebarWidth - scale(SIDEBAR_TAB_INNER_PADDING),
                    scale(SIDEBAR_TAB_HEIGHT), scale(TAB_BORDER_RADIUS), tabColor);

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

    /**
     * Draws the mod icon above the tabs, centred across the sidebar.
     * The logo asset is opaque, so it reads as a small app tile with rounded corners.
     */
    private void renderLogo(DrawContext context, int x, int y, int sidebarWidth) {
        int size = scale(SIDEBAR_LOGO_SIZE);
        int logoX = x + (sidebarWidth - size) / 2;
        int logoY = y + sidebarLogoTop();

        // Scale the whole texture down to the tile. The 9-argument overload would treat size as the
        // size of the sampled region too, which grabbed only the plain top-left corner of the logo.
        GuiComponent.drawTexture(context, GuiComponent.LOGO, logoX, logoY, 0, 0,
                size, size,
                GuiComponent.LOGO_TEXTURE_SIZE, GuiComponent.LOGO_TEXTURE_SIZE,
                GuiComponent.LOGO_TEXTURE_SIZE, GuiComponent.LOGO_TEXTURE_SIZE);

        roundCorners(context, logoX, logoY, size, Math.min(scale(SIDEBAR_LOGO_CORNER_RADIUS), size / 2));
    }

    /**
     * Rounds the tile by painting over what falls outside the rounded square.
     * <p>
     * The logo has no alpha channel, so its corners cannot be masked away — they are covered with
     * the sidebar's own background colour instead. That colour is forced opaque so the blue tile
     * does not tint through it, and it is taken from the same constant the sidebar is filled with
     * so the two stay in step if the colour changes.
     * <p>
     * The corner is drawn as a grid of square blocks of
     * {@link TeamScreenLayout#SIDEBAR_LOGO_CORNER_STEP} GUI units (see
     * {@link TeamScreenLayout#chunkyCornerSpans}), so a larger step gives a coarser, blockier arc
     * while the corner keeps its size. The step is fixed, not derived from the window's GUI scale,
     * so the corner looks the same in a small window and in fullscreen.
     */
    private static void roundCorners(DrawContext context, int x, int y, int size, int radius) {
        if (radius <= 0) return;

        int step = Math.max(1, SIDEBAR_LOGO_CORNER_STEP);
        int background = BG_SECONDARY | 0xFF000000;

        int[] spans = chunkyCornerSpans(radius, step);

        for (int block = 0; block < spans.length; block++) {
            int span = spans[block];
            if (span <= 0) continue;

            int offset = block * step;
            int rows = Math.min(step, radius - offset);

            int topY = y + offset;
            int bottomY = y + size - offset - rows;
            int rightX = x + size - span;

            context.fill(x, topY, x + span, topY + rows, background);
            context.fill(rightX, topY, x + size, topY + rows, background);
            context.fill(x, bottomY, x + span, bottomY + rows, background);
            context.fill(rightX, bottomY, x + size, bottomY + rows, background);
        }
    }

    public Tab getTabAt(int mouseX, int mouseY, int sidebarX, int sidebarY, int sidebarWidth) {
        int tabY = sidebarY + scale(SIDEBAR_TAB_START_TOP);
        for (Tab tab : Tab.values()) {
            int tabHeight = scale(SIDEBAR_TAB_HEIGHT);
            if (mouseX >= sidebarX &&
                mouseX < sidebarX + sidebarWidth &&
                mouseY >= tabY &&
                mouseY < tabY + tabHeight) {
                return tab;
            }
            tabY += scale(SIDEBAR_TAB_SPACING);
        }
        return null;
    }
}