package me.unknkriod.quickmark.gui.mark.utils;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.mark.MarkType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The beam hangs on its mark: the configured ranges are offsets from the marked block, not an
 * absolute world Y range. The old absolute range (20..255) made any mark below y=20 report a
 * distance measured to a beam floating 60+ blocks above it.
 */
class GeometryCalculatorTest {

    private final MarkRenderConfig config = new MarkRenderConfig();
    private final GeometryCalculator geometry = new GeometryCalculator(config);

    @Test
    void deepMarkBeamIsAnchoredToTheMark() {
        BlockPos deepMark = new BlockPos(0, -40, 0);
        // Camera level with the mark: the beam reaches it, so the distance is purely horizontal.
        Vec3d camera = new Vec3d(3.5, -40, 4.5);

        assertEquals(5.0, geometry.calculateDistanceToBeam(camera, deepMark, MarkType.NORMAL), 1.0e-9);
    }

    @Test
    void highMarkBeamIsAnchoredToTheMark() {
        BlockPos highMark = new BlockPos(0, 300, 0);
        Vec3d camera = new Vec3d(0.5, 300, 0.5);

        assertEquals(0.0, geometry.calculateDistanceToBeam(camera, highMark, MarkType.NORMAL), 1.0e-9);
    }

    @Test
    void distanceIsHorizontalAnywhereInsideTheBeam() {
        BlockPos mark = new BlockPos(10, 64, -20);
        Vec3d camera = new Vec3d(10.5, 64 + config.getUpRange(MarkType.NORMAL) / 2, -19.5);

        assertEquals(0.0, geometry.calculateDistanceToBeam(camera, mark, MarkType.NORMAL), 1.0e-9);
    }

    @Test
    void distanceGrowsOnlyBeyondTheBeamEnds() {
        BlockPos mark = new BlockPos(0, 64, 0);
        int upRange = config.getUpRange(MarkType.NORMAL);
        Vec3d camera = new Vec3d(0.5, 64 + upRange + 10, 0.5);

        assertEquals(10.0, geometry.calculateDistanceToBeam(camera, mark, MarkType.NORMAL), 1.0e-9);
    }

    @Test
    void dangerBeamUsesItsOwnRanges() {
        assertTrue(config.getUpRange(MarkType.DANGER) < config.getUpRange(MarkType.NORMAL));
        assertTrue(config.getDownRange(MarkType.DANGER) < config.getDownRange(MarkType.NORMAL));

        BlockPos mark = new BlockPos(0, 64, 0);
        int dangerAbove = config.getUpRange(MarkType.DANGER);
        // Inside the normal beam, but 10 blocks above the top of the shorter danger beam.
        double cameraY = 64 + dangerAbove + 10;
        Vec3d camera = new Vec3d(0.5, cameraY, 0.5);

        assertTrue(cameraY < 64 + config.getUpRange(MarkType.NORMAL));
        assertEquals(10.0, geometry.calculateDistanceToBeam(camera, mark, MarkType.DANGER), 1.0e-9);
        assertEquals(0.0, geometry.calculateDistanceToBeam(camera, mark, MarkType.NORMAL), 1.0e-9);
    }
}
