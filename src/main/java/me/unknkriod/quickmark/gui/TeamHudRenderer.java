package me.unknkriod.quickmark.gui;

import me.unknkriod.quickmark.team.TeamManager;
import me.unknkriod.quickmark.team.TeamPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.UUID;

import static me.unknkriod.quickmark.team.TeamManager.getColorForPosition;

public class TeamHudRenderer {
    private static final int PLAYER_HEIGHT = 42;
    private static final int PLAYER_WIDTH = 128;
    private static final int HEAD_SIZE = 24;
    private static final int HEALTH_ICON_SIZE = 9;
    private static final int HEAD_PADDING = 4;
    private static final int MAX_HEALTH = 20;
    private static final int HEALTH_TEXT_Y_OFFSET = 10; // Отступ между строками здоровья
    private static final int NAME_HEALTH_SPACING = 2; // Отступ между ником и здоровьем
    private static final int HEAD_TEXT_SPACING = 8;   // Горизонтальный отступ текста от головы

    private static final float HUD_SCALE_FACTOR = 0.85f;

    public static void render(DrawContext context) {
        List<TeamPlayer> teamMembers = TeamManager.getTeamMembers();
        if (teamMembers.isEmpty() || teamMembers.size() == 1) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenHeight = client.getWindow().getScaledHeight();

        context.getMatrices().pushMatrix();

        // Рассчитываем общую высоту HUD после масштабирования
        float scale = HUD_SCALE_FACTOR;
        int scaledTotalHeight = (int) (teamMembers.size() * PLAYER_HEIGHT * scale);

        // Центрируем по вертикали
        int startY = (screenHeight - scaledTotalHeight) / 2;

        // Применяем трансформации
        context.getMatrices().translate(10, startY);
        context.getMatrices().scale(scale, scale);

        int y = 0;
        for (TeamPlayer player : teamMembers) {
            renderPlayerInfo(context, player, 0, y);
            y += PLAYER_HEIGHT;
        }

        context.getMatrices().popMatrix();
    }

    private static void renderPlayerInfo(DrawContext context, TeamPlayer player, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Получаем позицию игрока в команде для цвета
        int position = TeamManager.getPlayerPosition(player.getPlayerId());
        int color = getColorForPosition(position);

        // Рисуем фоновую панель
        context.fill(x, y, x + PLAYER_WIDTH, y + PLAYER_HEIGHT, 0x80000000);

        int centerY = y + (PLAYER_HEIGHT - HEAD_SIZE) / 2;

        // Получаем текстуру скина
        Identifier skinTexture = getSkinTexture(player.getPlayerId(), player.getPlayerName());
        if (skinTexture != null) {
            // Рисуем голову игрока (8x8 пикселей из текстуры)
            GuiComponent.drawTexture(context, skinTexture,
                    x + HEAD_PADDING,
                    centerY,
                    8, 8,
                    HEAD_SIZE, HEAD_SIZE,
                    8, 8, 64, 64);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0, 0);

            // Рисуем шляпу игрока поверх головы (40x8 пикселей из текстуры)
            GuiComponent.drawTexture(context, skinTexture,
                    x + HEAD_PADDING,
                    centerY,
                    40, 8,
                    HEAD_SIZE, HEAD_SIZE,
                    8, 8, 64, 64);

            context.getMatrices().popMatrix();
        }

        // Ник игрока
        int textX = x + HEAD_SIZE + HEAD_TEXT_SPACING;

        context.drawText(client.textRenderer,
                Text.literal(player.getPlayerName()),
                textX, centerY,
                color, false);

        float health = player.getHealth();
        float absorption = player.getAbsorption();

        int heartX = textX;
        int heartY;

        // Если нет абсорбции — центрируем здоровье
        if (absorption <= 0) {
            heartY = centerY + client.textRenderer.fontHeight + NAME_HEALTH_SPACING + (HEALTH_TEXT_Y_OFFSET / 2);
        } else {
            // Если есть абсорбция — две строки
            heartY = centerY + client.textRenderer.fontHeight + NAME_HEALTH_SPACING;
        }

        // Числовой режим для обычного здоровья
        boolean numericHealth = health > MAX_HEALTH;
        // Числовой режим для абсорбции
        boolean numericAbsorption = absorption > MAX_HEALTH;

        // Рендер обычного здоровья
        if (numericHealth) {
            renderNumericSingle(context, GuiComponent.HEART_FULL, health, heartX, heartY);
        } else {
            renderHearts(context, health, heartX, heartY, false);
        }

        // Рендер абсорбции, если есть
        if (absorption > 0) {
            if (numericAbsorption) {
                renderNumericSingle(context, GuiComponent.HEART_ABSORBING_FULL, absorption, heartX, heartY + HEALTH_TEXT_Y_OFFSET);
            } else {
                renderHearts(context, absorption, heartX, heartY + HEALTH_TEXT_Y_OFFSET, true);
            }
        }
    }

    /**
     * Отображает здоровье в виде сердечек
     */
    private static void renderHearts(DrawContext context, float healthValue, int startX, int startY, boolean isAbsorption) {
        int heartCount = MathHelper.ceil(healthValue / 2.0f);
        int x = startX;

        for (int i = 0; i < heartCount; i++) {
            int heartIndex = i * 2;
            Identifier texture;

            if (healthValue > heartIndex + 1) {
                // Полное сердце
                texture = isAbsorption ? GuiComponent.HEART_ABSORBING_FULL : GuiComponent.HEART_FULL;
            } else if (healthValue > heartIndex) {
                // Половина сердца
                texture = isAbsorption ? GuiComponent.HEART_ABSORBING_HALF : GuiComponent.HEART_HALF;
            } else {
                // Пустое сердце
                texture = GuiComponent.HEART_EMPTY;
            }

            GuiComponent.drawTexture(context, texture,
                    x, startY,
                    0, 0,
                    HEALTH_ICON_SIZE, HEALTH_ICON_SIZE,
                    9, 9);

            x += HEALTH_ICON_SIZE;
        }
    }

    /**
     * Отображает здоровье в числовом формате
     */
    private static void renderNumericSingle(DrawContext context, Identifier icon, float value, int startX, int startY) {
        MinecraftClient client = MinecraftClient.getInstance();

        int color = ColorHelper.getArgb(255, 243, 243, 243);

        // Переводим здоровье в сердца
        float hearts = value / 2.0f;

        // Округляем до ближайшей половинки
        hearts = Math.round(hearts * 2) / 2.0f;

        // Форматируем
        String text = "x " + ((hearts % 1 == 0) ? String.valueOf((int) hearts) : (int) hearts + ".5");

        // Рисуем иконку
        GuiComponent.drawTexture(context, icon,
                startX, startY,
                0, 0,
                HEALTH_ICON_SIZE, HEALTH_ICON_SIZE,
                9, 9);

        // Рисуем текст
        context.drawText(client.textRenderer, text,
                startX + HEALTH_ICON_SIZE + 2, startY,
                color, false);
    }

    private static Identifier getSkinTexture(UUID playerId, String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return null;

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerId);
        if (entry != null) {
            return entry.getSkinTextures().texture();
        }
        return null;
    }
}