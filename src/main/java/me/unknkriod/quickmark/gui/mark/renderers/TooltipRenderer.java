package me.unknkriod.quickmark.gui.mark.renderers;

import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.awt.Color;

public class TooltipRenderer {

    private static final Color DANGER_BEAM_COLOR = new Color(181, 1, 1, 255);

    public void drawTooltip(DrawContext context, Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();

        String playerName = TeamManager.getPlayerName(mark.getPlayerId());

        Color markColor;
        Text line1;
        Text line2 = Text.translatableWithFallback("quickmark.tooltip.line2", "Click the middle mouse button to delete");

        if (mark.getType() == MarkType.DANGER) {
            line1 = Text.translatableWithFallback("quickmark.tooltip.danger.line1", "DANGER!");
            markColor = DANGER_BEAM_COLOR;
        } else {
            line1 = Text.translatableWithFallback("quickmark.tooltip.line1", playerName + "`s mark", playerName);
            markColor = MarkRenderer.getColorForMark(mark);
        }

        int textWidth1 = client.textRenderer.getWidth(line1);
        int textWidth2 = client.textRenderer.getWidth(line2);
        int maxWidth = Math.max(textWidth1, textWidth2);
        int textHeight = client.textRenderer.fontHeight;
        int padding = 8;

        // Позиция у прицела (центр экрана)
        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;

        // Смещение тултипа относительно прицела
        int offsetX = 20;
        int offsetY = -30;

        int boxX = centerX + offsetX;
        int boxY = centerY + offsetY;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = textHeight * 2 + padding * 2 + 2;

        // Проверка границ экрана
        if (boxX + boxWidth > window.getScaledWidth()) {
            boxX = centerX - offsetX - boxWidth;
        }
        if (boxY < 0) {
            boxY = centerY - offsetY;
        }
        if (boxY + boxHeight > window.getScaledHeight()) {
            boxY = window.getScaledHeight() - boxHeight - 10;
        }

        // Считаем яркость цвета команды
        float brightness = (0.299f * markColor.getRed() +
                0.587f * markColor.getGreen() +
                0.114f * markColor.getBlue()) / 255f;

        // Если цвет светлый — используем более тёмный фон, иначе — слегка тёмный
        int backgroundColor = (brightness > 0.6f) ? 0xCC000000 : 0xCC1A1A1A;

        // Цвет рамки оставляем цветом команды
        int borderColor = markColor.getRGB() | 0xFF000000;

        // Рисуем тень тултипа
        context.fill(boxX + 2, boxY + 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 0x80000000);

        // Рисуем фон тултипа
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, backgroundColor);

        // Рисуем обводку
        context.drawBorder(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, borderColor);

        // Рисуем внутреннюю обводку для объема
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFF333333);

        // Текст с обводкой для лучшей читаемости
        int text1X = boxX + padding + (maxWidth - textWidth1) / 2;
        int text1Y = boxY + padding;
        int text2X = boxX + padding + (maxWidth - textWidth2) / 2;
        int text2Y = boxY + padding + textHeight + 2;

        int textColor = (mark.getType() == MarkType.DANGER) ? 0xFFFF0000 : 0xFFFFFFFF;

        context.drawText(client.textRenderer,
                line1,
                text1X, text1Y,
                textColor, false);

        context.drawText(client.textRenderer,
                line2,
                text2X, text2Y,
                textColor, false);
    }
}