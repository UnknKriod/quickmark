package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import static me.unknkriod.quickmark.gui.screen.TeamUIUtils.drawRoundedRect;

public class StatusMessageRenderer {

    private static final float STATUS_FADE_SPEED = 3f;
    private static final long STATUS_MESSAGE_DURATION = 3000;

    public static void render(
            DrawContext context,
            TextRenderer textRenderer,
            int width,
            int height,
            int panelHeight,
            String statusMessage,
            long statusMessageTime,
            float statusMessageAlpha,
            float delta,
            float scaleFactor
    ) {
        if (statusMessage != null) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < STATUS_MESSAGE_DURATION) {
                if (elapsed < 500) {
                    statusMessageAlpha = Math.min(1f, statusMessageAlpha + delta * STATUS_FADE_SPEED);
                } else if (elapsed > STATUS_MESSAGE_DURATION - 500) {
                    statusMessageAlpha = Math.max(0f, statusMessageAlpha - delta * STATUS_FADE_SPEED);
                } else {
                    statusMessageAlpha = 1f;
                }

                int centerX = width / 2;
                int panelTop = height / 2 - panelHeight / 2;
                int messageY = panelTop - (int)(30 * scaleFactor);

                int messageWidth = textRenderer.getWidth(statusMessage) + (int)(20 * scaleFactor);
                int messageX = centerX - messageWidth / 2;

                int bgAlpha = (int)(0xCC * statusMessageAlpha);
                int textAlpha = (int)(0xFF * statusMessageAlpha);

                drawRoundedRect(context, messageX, messageY - (int)(5 * scaleFactor),
                        messageWidth, (int)(25 * scaleFactor),
                        (int)(4 * scaleFactor), (bgAlpha << 24) | 0x1E1E26);

                context.drawText(textRenderer, statusMessage,
                        centerX - textRenderer.getWidth(statusMessage) / 2,
                        messageY, (textAlpha << 24) | 0xFFFFFF, false);
            } else {
                statusMessage = null;
                statusMessageAlpha = 0f;
            }
        }
    }
}