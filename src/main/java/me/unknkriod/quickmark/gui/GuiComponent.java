package me.unknkriod.quickmark.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class GuiComponent {
    public static final Identifier INVITE_TEXTURE = Identifier.of("quickmark", "textures/gui/pending_invite.png");
    public static final Identifier INFO_TEXTURE = Identifier.of("quickmark", "textures/gui/info.png");
    public static final Identifier SUCCESS_TEXTURE = Identifier.of("quickmark", "textures/gui/success.png");
    public static final Identifier HEART_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/full.png");
    public static final Identifier HEART_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/half.png");
    public static final Identifier HEART_ABSORBING_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/absorbing_full.png");
    public static final Identifier HEART_ABSORBING_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/absorbing_half.png");
    public static final Identifier HEART_EMPTY = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/container.png");
    public static final Identifier MARK = Identifier.of("quickmark", "textures/mark.png");
    public static final Identifier WHITE_PIXEL = Identifier.of("quickmark", "textures/misc/white_pixel.png");

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, -1);
    }
}