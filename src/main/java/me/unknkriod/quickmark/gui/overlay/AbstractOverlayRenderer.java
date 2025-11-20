package me.unknkriod.quickmark.gui.overlay;

import com.mojang.authlib.GameProfile;
import me.unknkriod.quickmark.gui.GuiComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.util.UUID;

public abstract class AbstractOverlayRenderer {
    // Animation states
    protected static final int STATE_HIDDEN = 0;
    protected static final int STATE_APPEARING = 1;
    protected static final int STATE_VISIBLE = 2;
    protected static final int STATE_DISAPPEARING = 3;

    // Animation timing configuration
    protected long appearAnimationTime = 500;
    protected long disappearAnimationTime = 400;
    protected long displayTime = 5000;

    protected int animationState = STATE_HIDDEN;
    protected long animationStartTime;
    protected int lastX;

    protected UUID playerId = null;
    protected String playerName = null;
    protected long startTime = 0;

    protected float scale = 0.75f;

    public void show(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.startTime = System.currentTimeMillis();
        this.animationState = STATE_APPEARING;
        this.animationStartTime = System.currentTimeMillis();
    }

    public void hide() {
        if (animationState == STATE_VISIBLE || animationState == STATE_APPEARING) {
            animationState = STATE_DISAPPEARING;
            animationStartTime = System.currentTimeMillis();
            lastX = 5; // Target X position for animation
        }
    }

    protected Identifier getSkinTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (playerName == null) return null;

        GameProfile profile = new GameProfile(playerId, playerName);
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerId);
        if (entry != null) {
            return entry.getSkinTextures().texture();
        }

        PlayerSkinProvider provider = client.getSkinProvider();
        SkinTextures textures = provider.getSkinTextures(profile);
        return textures.texture();
    }

    protected void renderPlayerHead(DrawContext context, int x, int y) {
        Identifier skin = getSkinTexture();
        if (skin != null) {
            int headSize = (int) (32 * scale);

            // Render player head (8x8 pixels from texture)
            GuiComponent.drawTexture(context, skin,
                    x,
                    y,
                    8, 8,
                    headSize, headSize,
                    8, 8, 64, 64);

            // Enhanced rendering for fine details (glasses, etc.)
            float scaleFactor = 1.05f;
            int scaledHeadSize = (int) (headSize * scaleFactor);
            int offsetX = x - (scaledHeadSize - headSize) / 2;
            int offsetY = y - (scaledHeadSize - headSize) / 2;

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(offsetX, offsetY);
            context.getMatrices().scale(scaleFactor, scaleFactor);

            // Render player hat layer (40x8 pixels from texture)
            GuiComponent.drawTexture(context, skin,
                    x,
                    y,
                    40, 8,
                    scaledHeadSize, scaledHeadSize,
                    8, 8, 64, 64);

            context.getMatrices().popMatrix();
        }
    }

    protected void renderProgressBar(DrawContext context, int x, int y, int width, int height, long elapsed, long displayTime) {
        int barHeight = (int) (4 * scale);
        int barY = y + height - barHeight;
        float progress = 1.0f - (float) elapsed / displayTime;
        int progressWidth = (int) (width * progress);
        context.fill(x, barY, x + progressWidth, barY + barHeight, 0xFFFFD700);
    }

    protected int calculateAnimationX(int targetX, int width) {
        long now = System.currentTimeMillis();
        long elapsed = now - animationStartTime;

        switch (animationState) {
            case STATE_APPEARING:
                if (elapsed > appearAnimationTime) {
                    animationState = STATE_VISIBLE;
                    animationStartTime = now;
                    return targetX;
                } else {
                    // Cubic easing out function for smooth animation
                    float progress = (float) elapsed / appearAnimationTime;
                    progress = progress * progress * (3.0f - 2.0f * progress);
                    return (int) (targetX - width + (width * progress));
                }

            case STATE_VISIBLE:
                if (now - startTime > displayTime) {
                    hide(); // Start hide animation when display time expires
                }
                return targetX;

            case STATE_DISAPPEARING:
                if (elapsed > disappearAnimationTime) {
                    animationState = STATE_HIDDEN;
                    playerId = null;
                    playerName = null;
                    return targetX - width; // Hidden beyond left border
                } else {
                    // First shift 2 pixels to the right
                    if (elapsed < disappearAnimationTime * 0.1f) {
                        return lastX + 2;
                    }
                    // Then animate movement to the left
                    else {
                        float progress = (float) (elapsed - disappearAnimationTime * 0.1f) /
                                (disappearAnimationTime * 0.9f);
                        progress = progress * progress * (3.0f - 2.0f * progress);
                        return (int) (lastX + 2 - (width + lastX + 2) * progress);
                    }
                }

            default: // STATE_HIDDEN
                return targetX - width; // Hidden beyond left border
        }
    }

    public boolean isVisible() {
        return animationState != STATE_HIDDEN;
    }

    public abstract void render(DrawContext context);
}