package me.unknkriod.quickmark.gui.mark.utils;

import me.unknkriod.quickmark.mark.Mark;
import net.minecraft.util.math.Vec3d;

public class IconRenderData {
    private final Mark mark;
    private final Vec3d position;
    private final Vec3d cameraPos;
    private final float transparency;
    private final float scale;
    private final boolean interactive;

    private IconRenderData(Builder builder) {
        this.mark = builder.mark;
        this.position = builder.position;
        this.cameraPos = builder.cameraPos;
        this.transparency = builder.transparency;
        this.scale = builder.scale;
        this.interactive = builder.interactive;
    }

    public static class Builder {
        private Mark mark;
        private Vec3d position;
        private Vec3d cameraPos;
        private float transparency = 1.0f;
        private float scale = 1.0f;
        private boolean interactive = false;

        public Builder mark(Mark mark) { this.mark = mark; return this; }
        public Builder position(Vec3d position) { this.position = position; return this; }
        public Builder cameraPos(Vec3d cameraPos) { this.cameraPos = cameraPos; return this; }
        public Builder transparency(float transparency) { this.transparency = transparency; return this; }
        public Builder scale(float scale) { this.scale = scale; return this; }
        public Builder interactive(boolean interactive) { this.interactive = interactive; return this; }

        public IconRenderData build() {
            return new IconRenderData(this);
        }
    }

    // Геттеры
    public Mark getMark() { return mark; }
    public Vec3d getPosition() { return position; }
    public Vec3d getCameraPos() { return cameraPos; }
    public float getTransparency() { return transparency; }
    public float getScale() { return scale; }
    public boolean isInteractive() { return interactive; }
}