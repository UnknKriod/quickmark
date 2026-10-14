package me.unknkriod.quickmark.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.MathHelper;

import static me.unknkriod.quickmark.gui.screen.TeamScreenLayout.*;

public final class TeamUIUtils {

    private TeamUIUtils() {}

    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height,
                                        int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        context.fill(x + radius, y, x + radius + scale(1), y + radius, color);
        context.fill(x + width - radius - scale(1), y, x + width - radius, y + radius, color);
        context.fill(x + radius, y + height - radius, x + radius + scale(1), y + height, color);
        context.fill(x + width - radius - scale(1), y + height - radius, x + width - radius, y + height, color);
    }

    /**
     * Draws a button that is never allowed to spill past {@code clipRight}.
     * <p>
     * {@code clipRight} is the panel's right edge in screen coordinates. Callers used to pass a
     * width here instead, which made the clamp compute a nonsense edge and silently dropped the
     * button entirely.
     */
    public static void drawButton(DrawContext context, int x, int y, int width, int height,
                                   String text, int color,
                                   int clipRight,
                                   TextRenderer textRenderer,
                                   int scaledBorderRadius) {
        if (x + width > clipRight) {
            width = clipRight - x;
        }

        if (width > scale(20)) {
            drawRoundedRect(context, x, y, width, height, scaledBorderRadius, color);
            String trimmedText = textRenderer.trimToWidth(text, width - scale(BUTTON_LABEL_PADDING));
            int textWidth = textRenderer.getWidth(trimmedText);
            context.drawText(textRenderer, trimmedText,
                    buttonLabelX(x, width, textWidth),
                    buttonLabelY(y, height, textRenderer.fontHeight),
                    TEXT_PRIMARY, false);
        }
    }

    /**
     * Draws a square icon button that is never allowed to spill past {@code clipRight}.
     * {@code clipRight} is the panel's right edge in screen coordinates.
     */
    public static void drawIconButton(DrawContext context, int x, int y, int size, String icon, int color,
                                       int clipRight,
                                       TextRenderer textRenderer,
                                       int scaledBorderRadius) {
        int actualSize = Math.min(size, clipRight - x);

        if (actualSize > 0) {
            drawRoundedRect(context, x, y, actualSize, actualSize, scaledBorderRadius, color);
            int iconWidth = textRenderer.getWidth(icon);
            int iconX = x + (actualSize - iconWidth) / 2;
            int iconY = y + (actualSize - textRenderer.fontHeight) / 2 + 1;
            context.drawText(textRenderer, icon, iconX, iconY, TEXT_PRIMARY, false);
        }
    }

    /**
     * Draws a single glyph so that its own slot is centred on {@code centreX}.
     * <p>
     * Centring by {@code getWidth()} is wrong for emoji: the renderer lays such glyphs out in a
     * square slot as tall as the font, while the reported string width can be narrower, so the
     * glyph ends up visibly off-axis. Drawing the glyph at {@code centreX - fontHeight / 2} lines
     * the slot up with the target instead.
     */
    public static void drawCentredGlyph(DrawContext context, TextRenderer textRenderer, String glyph,
                                        int centreX, int y, int color) {
        context.drawText(textRenderer, glyph, centreX - textRenderer.fontHeight / 2, y, color, true);
    }

    public static boolean mouseInRect(int mouseX, int mouseY, int rx, int ry, int rw, int rh) {
        return mouseX >= rx && mouseX < rx + rw &&
                mouseY >= ry && mouseY < ry + rh;
    }

    public static int getHealthColor(float percent) {
        if (percent > HEALTH_HIGH_THRESHOLD) return ACCENT_SUCCESS;
        if (percent > HEALTH_MEDIUM_THRESHOLD) return 0xFF_F59E0B;
        return ACCENT_DANGER;
    }

    public static int lerpColor(int color1, int color2, float t) {
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
}
