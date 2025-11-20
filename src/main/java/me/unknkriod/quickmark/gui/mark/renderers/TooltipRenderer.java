package me.unknkriod.quickmark.gui.mark.renderers;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.gui.mark.utils.GeometryCalculator;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.awt.Color;

/**
 * Renders tooltips for marks when the player looks at them.
 * Shows player name (or "DANGER!"), ownership info, and removal hint for own marks.
 */
public class TooltipRenderer {
    private final MarkRenderConfig config;
    private final float TOOLTIP_SCALE;

    public TooltipRenderer(MarkRenderConfig config) {
        this.config = config;
        this.TOOLTIP_SCALE = config.getTooltipScale();
    }

    public void drawTooltip(DrawContext context, Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();

        String playerName = TeamManager.getPlayerName(mark.getPlayerId());

        Color markColor;
        Text line1;
        Text line2 = Text.translatableWithFallback("quickmark.tooltip.line2", "Click the middle mouse button to delete");

        if (mark.getType() == MarkType.DANGER) {
            line1 = Text.translatableWithFallback("quickmark.tooltip.danger.line1", "DANGER!");
            markColor = config.getDangerBeamColor();
        } else {
            line1 = Text.translatableWithFallback("quickmark.tooltip.line1", playerName + "'s mark", playerName);
            markColor = MarkRenderer.getColorForMark(mark);
        }

        boolean isOwner = client.player != null && client.player.getUuid().equals(mark.getPlayerId());

        int textWidth1 = client.textRenderer.getWidth(line1);
        int textWidth2 = client.textRenderer.getWidth(line2);
        int maxWidth = Math.max(textWidth1, textWidth2);
        int textHeight = client.textRenderer.fontHeight;
        int padding = (int) (8 * TOOLTIP_SCALE);

        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;

        int offsetX = (int) (20 * TOOLTIP_SCALE);
        int offsetY = (int) (-30 * TOOLTIP_SCALE);

        int boxX = centerX + offsetX;
        int boxY = centerY + offsetY;
        int boxWidth = (int) ((maxWidth + padding * 2) * TOOLTIP_SCALE);
        int lines = isOwner ? 2 : 1;
        int boxHeight = (int) ((textHeight * lines + padding * 2 + (isOwner ? 2 : 0)) * TOOLTIP_SCALE);

        // Checking screen borders
        if (boxX + boxWidth > window.getScaledWidth()) {
            boxX = centerX - offsetX - boxWidth;
        }
        if (boxY < 0) {
            boxY = centerY - offsetY;
        }
        if (boxY + boxHeight > window.getScaledHeight()) {
            boxY = window.getScaledHeight() - boxHeight - 10;
        }

        float brightness = (0.299f * markColor.getRed() +
                0.587f * markColor.getGreen() +
                0.114f * markColor.getBlue()) / 255f;

        // If the color is light, we use a darker background, otherwise it is slightly dark
        int backgroundColor = (brightness > 0.6f) ? 0xCC000000 : 0xCC1A1A1A;

        int borderColor = markColor.getRGB() | 0xFF000000;

        // Shadow
        context.fill(boxX + 2, boxY + 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 0x80000000);

        // Background
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, backgroundColor);

        // Outer border (team color)
        context.drawBorder(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, borderColor);

        // Inner border for depth
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFF333333);

        int text1X = boxX + padding + (maxWidth - textWidth1) / 2;
        int text1Y = isOwner ? boxY + padding : boxY + (boxHeight - textHeight) / 2;

        int text2X = boxX + padding + (maxWidth - textWidth2) / 2;
        int text2Y = text1Y + textHeight + 2;

        int textColor = mark.getType() == MarkType.DANGER ? 0xFFFF0000 : 0xFFFFFFFF;

        // Apply tooltip scale
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(boxX, boxY);
        context.getMatrices().scale(TOOLTIP_SCALE, TOOLTIP_SCALE);
        context.getMatrices().translate(-boxX, -boxY);

        context.drawText(client.textRenderer, line1, text1X, text1Y, textColor, false);

        if (isOwner) {
            context.drawText(client.textRenderer, line2, text2X, text2Y, textColor, false);
        }

        context.getMatrices().popMatrix();
    }
}