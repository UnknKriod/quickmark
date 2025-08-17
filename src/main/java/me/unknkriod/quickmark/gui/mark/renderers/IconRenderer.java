package me.unknkriod.quickmark.gui.mark.renderers;

import me.unknkriod.quickmark.gui.GuiComponent;
import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.gui.mark.utils.GeometryCalculator;
import me.unknkriod.quickmark.gui.mark.utils.IconRenderData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

import static me.unknkriod.quickmark.gui.GuiComponent.MARK;

public class IconRenderer {
    private final MarkRenderConfig config;
    private final GeometryCalculator geometryCalc;

    public IconRenderer(MarkRenderConfig config) {
        this.config = config;
        this.geometryCalc = new GeometryCalculator(config);
    }

    public void drawMarkIcon(DrawContext context, IconRenderData iconData) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d screenPos = geometryCalc.projectToScreen(client, iconData.getPosition());
        if (screenPos == null) return;

        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        if (iconData.isInteractive()) {
            drawInteractiveIcon(context, iconData, x, y, client);
        } else {
            drawStaticIcon(context, iconData, x, y, client);
        }
    }

    private void drawInteractiveIcon(DrawContext context, IconRenderData iconData, int x, int y, MinecraftClient client) {
        Color markColor = MarkRenderer.getColorForMark(iconData.getMark());
        int baseColorInt = (markColor.getRed() << 16) | (markColor.getGreen() << 8) | markColor.getBlue();

        float transparency = iconData.getTransparency();

        // Применяем прозрачность к цветам
        int glowAlpha = (int)(0x80 * transparency);
        int borderAlpha = (int)(0xFF * transparency);

        // Размер иконки зависит от прозрачности
        int iconSize = (int)(config.getMarkIconSize() + (transparency * 8)); // От 16 до 24 пикселей
        int glowSize = iconSize + 6;
        int halfGlowSize = glowSize / 2;
        int halfSize = iconSize / 2;

        // Рисуем свечение с учетом прозрачности
        context.fill(x - halfGlowSize, y - halfGlowSize,
                x + halfGlowSize, y + halfGlowSize,
                (baseColorInt & 0xFFFFFF) | (glowAlpha << 24));

        // Рисуем иконку
        GuiComponent.drawTexture(context, MARK,
                x - halfSize,
                y - halfSize,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize);

        // Рисуем обводку с учетом прозрачности
        int borderThickness = transparency > 0.8f ? 2 : 1;
        context.drawBorder(x - halfSize - borderThickness, y - halfSize - borderThickness,
                iconSize + borderThickness * 2, iconSize + borderThickness * 2,
                (baseColorInt & 0xFFFFFF) | (borderAlpha << 24));

        drawDistanceText(context, iconData, x, y, halfSize, transparency, client);
    }

    private void drawStaticIcon(DrawContext context, IconRenderData iconData, int x, int y, MinecraftClient client) {
        Color markColor = MarkRenderer.getColorForMark(iconData.getMark());
        int colorInt = (markColor.getRed() << 16) | (markColor.getGreen() << 8) | markColor.getBlue();

        float transparency = iconData.getTransparency();
        float scale = iconData.getScale();

        // Рассчитываем альфа-каналы на основе прозрачности
        int glowAlpha = (int)(0x30 * transparency);
        int borderAlpha = (int)(0xAA * transparency);

        int scaledIconSize = (int) (config.getMarkIconSize() * scale);
        int scaledGlowSize = scaledIconSize + 2;
        int halfScaledGlowSize = scaledGlowSize / 2;
        int halfScaledSize = scaledIconSize / 2;

        context.fill(x - halfScaledGlowSize, y - halfScaledGlowSize,
                x + halfScaledGlowSize, y + halfScaledGlowSize,
                (colorInt & 0xFFFFFF) | (glowAlpha << 24));

        GuiComponent.drawTexture(context, MARK,
                x - halfScaledSize,
                y - halfScaledSize,
                0, 0,
                scaledIconSize, scaledIconSize,
                scaledIconSize, scaledIconSize);

        context.drawBorder(x - halfScaledSize - 1, y - halfScaledSize - 1,
                scaledIconSize + 2, scaledIconSize + 2,
                (colorInt & 0xFFFFFF) | (borderAlpha << 24));

        drawDistanceText(context, iconData, x, y, halfScaledSize, transparency, client);
    }

    private void drawDistanceText(DrawContext context, IconRenderData iconData,
                                  int x, int y, int halfIconSize, float transparency, MinecraftClient client) {
        // Расстояние до луча, а не до иконки
        double distance = geometryCalc.calculateDistanceToBeam(iconData.getCameraPos(), iconData.getMark().getPosition());
        String distanceText = Math.round(distance) + "м";
        int textWidth = client.textRenderer.getWidth(distanceText);

        // Позиция текста под иконкой
        int textX = x - textWidth / 2;
        int textY = y + halfIconSize + config.getMarkDistanceOffset();

        // Прозрачность фона и текста
        int backgroundAlpha = iconData.isInteractive() ? (int)(0xE0 * transparency) : (int)(0xC0 * transparency);
        int textAlpha = (int)(0xFF * transparency);

        // Рисуем фон для текста с учетом прозрачности
        int padding = iconData.isInteractive() ? 3 : 2;
        context.fill(textX - padding, textY - 1,
                textX + textWidth + padding,
                textY + client.textRenderer.fontHeight + 1,
                backgroundAlpha << 24);

        // Цвет текста в зависимости от прозрачности
        int textColor = (iconData.isInteractive() && transparency > 0.8f) ? 0xFFFF55 : 0xFFFFFF;
        textColor = (textColor & 0xFFFFFF) | (textAlpha << 24);
        int outlineColor = (textAlpha << 24);

        // Рисуем текст с обводкой и прозрачностью
        drawTextWithOutlineAndAlpha(context, client.textRenderer, distanceText, textX, textY, textColor, outlineColor, transparency);
    }

    private void drawTextWithOutlineAndAlpha(DrawContext context, TextRenderer textRenderer,
                                             String text, int x, int y, int textColor, int outlineColor, float alpha) {
        // Если прозрачность очень низкая, рисуем обычным способом
        if (alpha < 0.5f) {
            drawTextWithOutline(context, textRenderer, text, x, y, textColor, outlineColor);
            return;
        }

        // Рисуем обводку с альфа-каналом
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                context.drawText(textRenderer, text, x + dx, y + dy, outlineColor, false);
            }
        }
        // Рисуем основной текст с альфа-каналом
        context.drawText(textRenderer, text, x, y, textColor, false);
    }

    private void drawTextWithOutline(DrawContext context, TextRenderer textRenderer,
                                     String text, int x, int y, int textColor, int outlineColor) {
        // Рисуем обводку
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                context.drawText(textRenderer, text, x + dx, y + dy, outlineColor, false);
            }
        }
        // Рисуем основной текст
        context.drawText(textRenderer, text, x, y, textColor, false);
    }
}