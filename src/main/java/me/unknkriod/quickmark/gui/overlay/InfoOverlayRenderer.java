package me.unknkriod.quickmark.gui.overlay;

import me.unknkriod.quickmark.gui.GuiComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.UUID;

import static me.unknkriod.quickmark.gui.GuiComponent.INFO_TEXTURE;

public class InfoOverlayRenderer extends AbstractOverlayRenderer {
    private static final int BASE_WIDTH = 400;
    private static final int BASE_HEIGHT = 56;

    public Text infoTitle;
    public Text infoMessage;

    public static final InfoOverlayRenderer INSTANCE = new InfoOverlayRenderer();

    public void show(Text title, Text message, UUID playerId, String playerName) {
        this.infoTitle = title;
        this.infoMessage = message;

        super.show(playerId, playerName);
    }

    @Override
    public void render(DrawContext context) {
        if (!isVisible() || playerName == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int scaledWidth = (int) (BASE_WIDTH * scale);
        int scaledHeight = (int) (BASE_HEIGHT * scale);

        int targetX = 5;
        int y = 60;

        int x = calculateAnimationX(targetX, scaledWidth);
        if (!isVisible()) return;

        // Фон
        GuiComponent.drawTexture(context, INFO_TEXTURE, x, y, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);

        // Голова лидера группы
        int headX = x + (int) (15 * scale);
        int headY = y + (scaledHeight - (int) (32 * scale)) / 2;
        renderPlayerHead(context, headX, headY);

        // Текст
        int textLeft = x + (int) (60 * scale);

        context.drawText(client.textRenderer,
                infoTitle,
                textLeft, y + (int) (12 * scale), 0xFF000000, false);

        context.drawText(client.textRenderer,
                infoMessage,
                textLeft, y + (int) (32 * scale), 0xFF666666, false);
    }
}
