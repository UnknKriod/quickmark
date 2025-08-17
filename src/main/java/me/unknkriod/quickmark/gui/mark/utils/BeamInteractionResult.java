package me.unknkriod.quickmark.gui.mark.utils;

public class BeamInteractionResult {
    private final double yPosition;
    private final float transparency;

    public BeamInteractionResult(double yPosition, float transparency) {
        this.yPosition = yPosition;
        this.transparency = transparency;
    }

    public double getYPosition() { return yPosition; }
    public float getTransparency() { return transparency; }
}