package me.unknkriod.quickmark.gui.mark;

import me.unknkriod.quickmark.mark.MarkType;

import java.awt.Color;

public class MarkRenderConfig {
    // Основные параметры
    /** Blocks the beam extends above the mark. Relative to the mark, not an absolute world Y. */
    private static final int UP_RANGE = 255;
    /** Blocks the beam extends below the mark. Relative to the mark. */
    private static final int DOWN_RANGE = 20;
    private static final int HOVER_THRESHOLD = 20;
    private static final long PARTICLE_INTERVAL = 200;

    // Параметры луча
    private static final float BEAM_SCREEN_WIDTH = 150.0f;
    private static final double BEAM_CENTER_TOLERANCE = 0.9;
    private static final double FADE_DISTANCE = 6.5; // Затухание прозрачности
    private static final float MIN_INTERACTION_TRANSPARENCY = 0.3f;

    // Параметры иконок
    private static final int MARK_ICON_SIZE = 16;
    private static final int MARK_DISTANCE_OFFSET = 4;
    private static final float MIN_ICON_SCALE = 0.65f;
    private static final float MAX_ICON_SCALE = 1.75f;
    private static final float MAX_SCALE_DISTANCE = 100.0f;

    private static final float TOOLTIP_SCALE = 0.75f;

    // Параметры опасных меток
    private static final float DANGER_BEAM_SCREEN_WIDTH = 150.0f;
    private static final int DANGER_UP_RANGE = 150;
    private static final int DANGER_DOWN_RANGE = 15;
    private static final Color DANGER_BEAM_COLOR = new Color(181, 1, 1, 255);

    // Цвета
    private static final Color DEFAULT_COLOR = new Color(255, 255, 255, 255);

    // Геттеры для всех констант
    /**
     * Blocks the beam extends above its mark. Anchored to the mark so a marker placed in a deep
     * cave (overworld spans -64..320 in 1.21) still gets a beam through the marked block.
     */
    public int getUpRange(MarkType type) {
        return type == MarkType.DANGER ? DANGER_UP_RANGE : UP_RANGE;
    }

    /** Blocks the beam extends below its mark. */
    public int getDownRange(MarkType type) {
        return type == MarkType.DANGER ? DANGER_DOWN_RANGE : DOWN_RANGE;
    }

    public int getHoverThreshold() { return HOVER_THRESHOLD; }
    public long getParticleInterval() { return PARTICLE_INTERVAL; }

    public float getBeamScreenWidth() { return BEAM_SCREEN_WIDTH; }
    public double getBeamCenterTolerance() { return BEAM_CENTER_TOLERANCE; }
    public double getFadeDistance() { return FADE_DISTANCE; }
    public float getMinInteractionTransparency() { return MIN_INTERACTION_TRANSPARENCY; }

    public int getMarkIconSize() { return MARK_ICON_SIZE; }
    public int getMarkDistanceOffset() { return MARK_DISTANCE_OFFSET; }
    public float getTooltipScale() { return TOOLTIP_SCALE; }
    public float getMinIconScale() { return MIN_ICON_SCALE; }
    public float getMaxIconScale() { return MAX_ICON_SCALE; }
    public float getMaxScaleDistance() { return MAX_SCALE_DISTANCE; }

    public float getDangerBeamScreenWidth() { return DANGER_BEAM_SCREEN_WIDTH; }
    public Color getDangerBeamColor() { return DANGER_BEAM_COLOR; }
    public Color getDefaultColor() { return DEFAULT_COLOR; }
}