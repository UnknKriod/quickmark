package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.drawRoundedRect;
import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;

public class TeamScrollController {

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragStartY = 0;
    private int scrollbarDragStartOffset = 0;

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void resetScroll() {
        scrollOffset = 0;
    }

    public int getMaxScroll() {
        return maxScroll;
    }

    public float getScrollPercent() {
        return maxScroll > 0 ? (float) scrollOffset / maxScroll : 0f;
    }

    public int getScrollbarDragStartY() {
        return scrollbarDragStartY;
    }

    public void setMaxScroll(int newMaxScroll) {
        this.maxScroll = newMaxScroll;
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    public void startDragging(int mouseY, int currentOffset) {
        isDraggingScrollbar = true;
        scrollbarDragStartY = mouseY;
        scrollbarDragStartOffset = currentOffset;
    }

    public boolean isDragging() {
        return isDraggingScrollbar;
    }

    public void stopDragging() {
        isDraggingScrollbar = false;
    }

    public boolean handleDrag(int mouseY, int scrollbarHeight, int maxScroll) {
        if (!isDraggingScrollbar) return false;

        int deltaMove = mouseY - scrollbarDragStartY;
        float scrollPercent = (float) deltaMove / scrollbarHeight;
        scrollOffset = MathHelper.clamp(
                scrollbarDragStartOffset + (int) (maxScroll * scrollPercent),
                0,
                maxScroll
        );
        return true;
    }

    public boolean handleScroll(double verticalAmount, float scrollSpeedScaled) {
        if (maxScroll <= 0) return false;

        int newOffset = scrollOffset - (int) (verticalAmount * scrollSpeedScaled);
        int oldOffset = scrollOffset;
        scrollOffset = MathHelper.clamp(newOffset, 0, maxScroll);

        return scrollOffset != oldOffset;
    }

    public void renderScrollbar(DrawContext context, int x, int y, int height, int mouseX, int mouseY,
                                TeamManagementScreen screen) {
        if (maxScroll <= 0) return;

        drawRoundedRect(context, x, y, scale(SCROLLBAR_WIDTH), height,
                scale(SCROLLBAR_BORDER_RADIUS), 0x40_FFFFFF);

        float scrollPercent = getScrollPercent();
        int thumbHeight = Math.max(scale(15), (int) (height * 0.3f));
        int thumbY = y + (int) ((height - thumbHeight) * scrollPercent);

        boolean isHovered = mouseX >= x && mouseX < x + scale(SCROLLBAR_WIDTH) &&
                mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        int thumbColor = isHovered || isDraggingScrollbar ? 0x80_FFFFFF : 0x60_FFFFFF;

        drawRoundedRect(context, x, thumbY, scale(SCROLLBAR_WIDTH), thumbHeight,
                scale(SCROLLBAR_BORDER_RADIUS), thumbColor);
    }
}
