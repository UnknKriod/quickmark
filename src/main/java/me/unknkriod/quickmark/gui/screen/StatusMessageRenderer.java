package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.drawRoundedRect;

/**
 * Shows a short status line above the panel, fading in and out.
 * <p>
 * Owns the message and the time it was shown. The fade is derived from how long the message has
 * been up rather than accumulated per frame, so it is frame-rate independent and the state cannot
 * be lost by passing it through a parameter.
 */
public class StatusMessageRenderer {

    private static final long DISPLAY_DURATION = 3000;
    private static final long FADE_DURATION = 500;

    private String message = null;
    private long messageTime = 0;

    /** Starts showing a message, fading in from transparent. */
    public void show(String message) {
        this.message = message;
        this.messageTime = System.currentTimeMillis();
    }

    public void render(DrawContext context, TextRenderer textRenderer, int width, int height,
                       int panelHeight, float scaleFactor) {
        if (message == null) return;

        long elapsed = System.currentTimeMillis() - messageTime;
        if (elapsed >= DISPLAY_DURATION) {
            // Expired: drop it here so the screen does not hold a stale string forever.
            message = null;
            return;
        }

        float alpha;
        if (elapsed < FADE_DURATION) {
            alpha = elapsed / (float) FADE_DURATION;
        } else if (elapsed > DISPLAY_DURATION - FADE_DURATION) {
            alpha = (DISPLAY_DURATION - elapsed) / (float) FADE_DURATION;
        } else {
            alpha = 1f;
        }

        int centerX = width / 2;
        int panelTop = height / 2 - panelHeight / 2;
        int messageY = panelTop - (int)(30 * scaleFactor);

        int messageWidth = textRenderer.getWidth(message) + (int)(20 * scaleFactor);
        int messageX = centerX - messageWidth / 2;

        int bgAlpha = (int)(0xCC * alpha);
        int textAlpha = (int)(0xFF * alpha);

        drawRoundedRect(context, messageX, messageY - (int)(5 * scaleFactor),
                messageWidth, (int)(25 * scaleFactor),
                (int)(4 * scaleFactor), (bgAlpha << 24) | 0x1E1E26);

        context.drawText(textRenderer, message,
                centerX - textRenderer.getWidth(message) / 2,
                messageY, (textAlpha << 24) | 0xFFFFFF, false);
    }
}