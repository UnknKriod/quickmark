package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

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

    private final Map<Tab, Float> tabAnimations = new HashMap<>();
    private final TextRenderer textRenderer;

    public TeamSidebarRenderer() {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        for (Tab tab : Tab.values()) {
            tabAnimations.put(tab, 0f);
        }
    }

    public void renderSidebar(DrawContext context, int x, int y, int mouseX, int mouseY, float delta,
                              Tab currentTab, int sidebarWidth, int panelHeight) {
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

    public void updateAnimations(float delta, Tab currentTab) {
        for (Tab tab : Tab.values()) {
            float target = tab == currentTab ? 1.0f : 0.0f;
            float current = tabAnimations.getOrDefault(tab, 0f);
            float newValue = current + (target - current) * delta * TAB_ANIMATION_SPEED;
            tabAnimations.put(tab, newValue);
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

    public float getTabAnimation(Tab tab) {
        return tabAnimations.getOrDefault(tab, 0f);
    }
}