package me.unknkriod.quickmark.gui.overlay;

import me.unknkriod.quickmark.gui.GuiComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import static me.unknkriod.quickmark.gui.GuiComponent.SUCCESS_TEXTURE;

/**
 * Success overlay shown after joining a team.
 */
public class SuccessOverlayRenderer extends AbstractOverlayRenderer {
    private static final int BASE_WIDTH = 400;
    private static final int BASE_HEIGHT = 56;
    private static final long DISPLAY_TIME = 3000;

    public static final SuccessOverlayRenderer INSTANCE = new SuccessOverlayRenderer();

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

        // Background
        GuiComponent.drawTexture(context, SUCCESS_TEXTURE, x, y, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);

        int headX = x + (int) (15 * scale);
        int headY = y + (scaledHeight - (int) (32 * scale)) / 2;
        renderPlayerHead(context, headX, headY);

        int textLeft = x + (int) (60 * scale);

        context.drawText(client.textRenderer,
                Text.translatableWithFallback("quickmark.success.title", "JOINED TEAM"),
                textLeft, y + (int) (12 * scale), 0xFF000000, false);

        context.drawText(client.textRenderer,
                Text.translatableWithFallback("quickmark.success.message", "You joined " + playerName + "'s team", playerName),
                textLeft, y + (int) (32 * scale), 0xFF666666, false);
    }
}