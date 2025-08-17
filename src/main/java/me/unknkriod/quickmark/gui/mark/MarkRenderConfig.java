package me.unknkriod.quickmark.gui.mark;

import java.awt.Color;

public class MarkRenderConfig {
    // Основные параметры
    private static final int MAX_Y = 255;
    private static final int DOWN_RANGE = 20;
    private static final int HOVER_THRESHOLD = 20;
    private static final long PARTICLE_INTERVAL = 200;

    // Параметры луча
    private static final float BEAM_WIDTH = 2.0f;
    private static final double BEAM_CENTER_TOLERANCE = 0.9;
    private static final double FADE_DISTANCE = 0.9;
    private static final float MIN_INTERACTION_TRANSPARENCY = 0.3f;

    // Параметры иконок
    private static final int MARK_ICON_SIZE = 16;
    private static final int MARK_DISTANCE_OFFSET = 4;
    private static final float MIN_ICON_SCALE = 0.65f;
    private static final float MAX_ICON_SCALE = 1.75f;
    private static final float MAX_SCALE_DISTANCE = 100.0f;

    // Параметры опасных меток
    private static final float DANGER_BEAM_WIDTH = 2.0f;
    private static final int DANGER_UP_RANGE = 150;
    private static final int DANGER_DOWN_RANGE = 15;
    private static final Color DANGER_BEAM_COLOR = new Color(181, 1, 1, 255);

    // Цвета
    private static final Color DEFAULT_COLOR = new Color(255, 255, 255, 255);

    // Геттеры для всех констант
    public int getMaxY() { return MAX_Y; }
    public int getDownRange() { return DOWN_RANGE; }
    public int getHoverThreshold() { return HOVER_THRESHOLD; }
    public long getParticleInterval() { return PARTICLE_INTERVAL; }

    public float getBeamWidth() { return BEAM_WIDTH; }
    public double getBeamCenterTolerance() { return BEAM_CENTER_TOLERANCE; }
    public double getFadeDistance() { return FADE_DISTANCE; }
    public float getMinInteractionTransparency() { return MIN_INTERACTION_TRANSPARENCY; }

    public int getMarkIconSize() { return MARK_ICON_SIZE; }
    public int getMarkDistanceOffset() { return MARK_DISTANCE_OFFSET; }
    public float getMinIconScale() { return MIN_ICON_SCALE; }
    public float getMaxIconScale() { return MAX_ICON_SCALE; }
    public float getMaxScaleDistance() { return MAX_SCALE_DISTANCE; }

    public float getDangerBeamWidth() { return DANGER_BEAM_WIDTH; }
    public int getDangerUpRange() { return DANGER_UP_RANGE; }
    public int getDangerDownRange() { return DANGER_DOWN_RANGE; }
    public Color getDangerBeamColor() { return DANGER_BEAM_COLOR; }

    public Color getDefaultColor() { return DEFAULT_COLOR; }
}