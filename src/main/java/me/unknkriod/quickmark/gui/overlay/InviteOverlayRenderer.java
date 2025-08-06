package me.unknkriod.quickmark.gui.overlay;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.gui.GuiComponent;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.UUID;

import static me.unknkriod.quickmark.gui.GuiComponent.INVITE_TEXTURE;

public class InviteOverlayRenderer extends AbstractOverlayRenderer {
    private static final int BASE_WIDTH = 462;
    private static final int BASE_HEIGHT = 57;
    private static final long DISPLAY_TIME = 5000;

    private boolean responseSent = false;

    public static final InviteOverlayRenderer INSTANCE = new InviteOverlayRenderer();

    @Override
    public void render(DrawContext context) {
        if (!isVisible() || playerName == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int scaledWidth = (int) (BASE_WIDTH * scale);
        int scaledHeight = (int) (BASE_HEIGHT * scale);

        int targetX = 5;
        int y = 60;

        // Вычисляем анимированную позицию
        int x = calculateAnimationX(targetX, scaledWidth);
        if (!isVisible()) return; // Проверяем, не скрылся ли оверлей во время анимации

        // Фон
        GuiComponent.drawTexture(context, INVITE_TEXTURE, x, y, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);

        // Голова игрока
        int headX = x + (int) (15 * scale);
        int headY = y + (scaledHeight - (int) (32 * scale)) / 2;
        renderPlayerHead(context, headX, headY);

        // Текст
        int textLeft = x + (int) (60 * scale);
        context.drawText(client.textRenderer,
                Text.translatableWithFallback("quickmark.invitation.title", "ПРИГЛАШЕНИЕ В ГРУППУ"),
                textLeft, y + (int) (12 * scale), 0xFF000000, false);

        context.drawText(client.textRenderer,
                Text.translatableWithFallback("quickmark.invitation.message",
                        "Игрок " + playerName + " приглашает вас вступить в группу", playerName),
                textLeft, y + (int) (32 * scale), 0xFF666666, false);

        // Инструкция по принятию
        Text keyName = Quickmark.getAcceptInvitationKey().getBoundKeyLocalizedText();
        Text acceptText = Text.translatableWithFallback("quickmark.invitation.accept", "Принять");
        Text keyText = Text.literal("[" + keyName.getString() + "]");

        // Координаты для синей области
        int blueAreaX = x + scaledWidth - (int) (67 * scale);
        int blueAreaY = y;
        int blueAreaWidth = (int) (67 * scale);
        int blueAreaHeight = scaledHeight;

        // Центрируем тексты
        int acceptTextWidth = client.textRenderer.getWidth(acceptText);
        int fontHeight = client.textRenderer.fontHeight;

        int acceptTextX = blueAreaX + (blueAreaWidth - acceptTextWidth) / 2;
        int acceptTextY = blueAreaY + (blueAreaHeight - fontHeight) / 2;

        context.drawText(client.textRenderer, acceptText, acceptTextX, acceptTextY, 0xFFFFFFFF, true);

        // Текст клавиши
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(0.65f, 0.65f);
        int scaledKeyTextWidth = (int) (client.textRenderer.getWidth(keyText) * 0.65f);
        int keyTextX = (int) ((blueAreaX + (blueAreaWidth - scaledKeyTextWidth) / 2) / 0.65f);
        int keyTextY = (int) ((acceptTextY + fontHeight + 3) / 0.65f);
        context.drawText(client.textRenderer, keyText, keyTextX, keyTextY, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();

        if (Quickmark.getAcceptInvitationKey().wasPressed() && animationState != STATE_DISAPPEARING) {
            long flashTime = System.currentTimeMillis() % 500;
            if (flashTime < 250) {
                // Принимаем
                acceptInvitation();
                // Мигаем
                context.fill(x, y, x + scaledWidth, y + scaledHeight, 0x40FFFFFF);
            }
        }

        // Прогресс-бар
        long elapsed = System.currentTimeMillis() - startTime;
        renderProgressBar(context, x, y, scaledWidth, scaledHeight, elapsed, DISPLAY_TIME);

        if (elapsed > DISPLAY_TIME && playerId != null && !responseSent) {
            TeamManager.declineInvitation(playerId);
            responseSent = true;
            hide();
        }
    }

    private void acceptInvitation() {
        if (playerId != null && animationState != STATE_DISAPPEARING && !responseSent) {
            TeamManager.acceptInvitation(playerId);
            responseSent = true;
            hide();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.playSound(
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.0f
                );
            }
        }
    }

    @Override
    public void show(UUID playerId, String playerName) {
        super.show(playerId, playerName);
        responseSent = false;
    }
}