package me.unknkriod.quickmark.gui.mark.utils;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class GeometryCalculator {
    private final MarkRenderConfig config;

    public GeometryCalculator(MarkRenderConfig config) {
        this.config = config;
    }

    /**
     * Checks if player's gaze ray interacts with a vertical mark beam
     * Uses line-to-line distance calculation for beam interaction detection
     */
    public BeamInteractionResult checkBeamInteraction(Vec3d cameraPos, Vec3d lookDirection, Mark mark) {
        BlockPos markPos = mark.getPosition();
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        double dx = lookDirection.x;
        double dy = lookDirection.y;
        double dz = lookDirection.z;

        // Skip if ray is nearly vertical (avoid division by zero)
        double den = dx * dx + dz * dz;
        if (den < 0.001) {
            return null;
        }

        double cx = cameraPos.x;
        double cz = cameraPos.z;

        // Find parameter t where gaze ray is closest to mark's vertical line
        double t = ((markX - cx) * dx + (markZ - cz) * dz) / den;

        if (t < 0) return null; // Intersection behind camera

        // Find closest point on gaze ray to mark center
        double closestX = cx + t * dx;
        double closestZ = cz + t * dz;

        double horizontalDistance = Math.sqrt(Math.pow(closestX - markX, 2) + Math.pow(closestZ - markZ, 2));

        // Calculate dynamic fade distance based on beam's visual width
        MinecraftClient client = MinecraftClient.getInstance();
        double beamDist = calculateDistanceToBeam(cameraPos, markPos);
        float fov = client.options.getFov().getValue();
        int screenW = client.getWindow().getWidth();
        double halfWidth = getBeamWorldHalfWidth(beamDist, fov, screenW, mark.getType());

        double fadeDistance = halfWidth;
        double centerTolerance = halfWidth * (config.getBeamCenterTolerance() / config.getFadeDistance());

        if (horizontalDistance > fadeDistance) {
            return null; // Too far from beam center
        }

        // Use player's eye height for interaction
        double yPosition = cameraPos.y;

        float transparency = calculateTransparency(horizontalDistance, centerTolerance, fadeDistance);

        return new BeamInteractionResult(yPosition, transparency);
    }

    /**
     * Calculates transparency based on distance from beam center
     * Full opacity in center, fading out towards edges
     */
    private float calculateTransparency(double dist, double tol, double fade) {
        if (dist <= tol) {
            return 1.0f;
        } else {
            float fadeProgress = (float) (dist - tol) / (float) (fade - tol);
            return Math.max(config.getMinInteractionTransparency(), 1.0f - fadeProgress);
        }
    }

    /**
     * Converts screen-space beam width to world-space units
     * Accounts for FOV and distance to maintain consistent visual width
     */
    private double getBeamWorldHalfWidth(double distance, float fov, int screenWidth, MarkType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        double vFovRad = Math.toRadians(fov);
        double aspect = (double) screenWidth / client.getWindow().getHeight();
        double hFovRad = 2 * Math.atan(Math.tan(vFovRad / 2) * aspect);
        double tanHalfHFov = Math.tan(hFovRad / 2);
        double pixelsPerBlockAtDist1 = screenWidth / (2 * tanHalfHFov);

        float beamScreenWidth = (type == MarkType.DANGER) ? config.getDangerBeamScreenWidth() : config.getBeamScreenWidth();
        double adjustedBeamWidth = (beamScreenWidth * distance) / pixelsPerBlockAtDist1;
        return adjustedBeamWidth / 2;
    }

    /**
     * Calculates 3D distance from camera to the nearest point on mark beam
     * Considers both horizontal distance and vertical alignment
     */
    public double calculateDistanceToBeam(Vec3d cameraPos, BlockPos markPos) {
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        double horizontalDistance = Math.sqrt(Math.pow(cameraPos.x - markX, 2) + Math.pow(cameraPos.z - markZ, 2));

        double cameraY = cameraPos.y;
        double closestY = Math.max(config.getDownRange(), Math.min(config.getMaxY(), cameraY));

        double verticalDistance = Math.abs(cameraY - closestY);
        return Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
    }

    /**
     * Calculates transparency for static icons based on horizontal distance
     */
    public float calculateStaticTransparency(Vec3d cameraPos, Vec3d markPos) {
        double markX = markPos.x;
        double markZ = markPos.z;
        double dx = cameraPos.x - markX;
        double dz = cameraPos.z - markZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        return calculateTransparencyByDistance(horizontalDistance);
    }

    /**
     * Calculates icon scale based on distance with linear interpolation
     * Icons get smaller as distance increases
     */
    public float calculateIconScale(double distance) {
        distance = Math.min(distance, config.getMaxScaleDistance());
        float factor = (float) (distance / config.getMaxScaleDistance());
        return config.getMaxIconScale() - factor * (config.getMaxIconScale() - config.getMinIconScale());
    }

    /**
     * Checks if any part of the beam is visible by testing multiple points along its height
     */
    public boolean isBeamVisible(Vec3d cameraPos, Vec3d markPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        double markX = markPos.x;
        double markZ = markPos.z;

        double minY = Math.max(config.getDownRange(), cameraPos.y - 10);
        double maxY = Math.min(config.getMaxY(), cameraPos.y + 20);

        double step = 2.0;

        for (double y = minY; y <= maxY; y += step) {
            Vec3d beamPoint = new Vec3d(markX, y, markZ);
            if (isPointVisible(cameraPos, beamPoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Projects 3D world position to 2D screen coordinates
     * Handles FOV calculations and camera transformations
     */
    public Vec3d projectToScreen(MinecraftClient client, Vec3d worldPos) {
        Window window = client.getWindow();
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();

        double vFovRad = Math.toRadians(client.options.getFov().getValue());
        double aspect = screenWidth / (double) screenHeight;
        double hFovRad = 2 * Math.atan(Math.tan(vFovRad / 2) * aspect);

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d transformed = worldPos.subtract(cameraPos);
        double distance = transformed.length();
        if (distance < 0.01) return null;

        Vec3d dir = transformed.normalize();
        Vec3d forward = client.getCameraEntity().getRotationVector();
        if (dir.dotProduct(forward) <= 0) return null;

        Vec3d forwardHor = new Vec3d(forward.x, 0, forward.z);
        double forwardHorLen = forwardHor.length();
        if (forwardHorLen < 0.001) {
            return null;
        }
        forwardHor = forwardHor.normalize();
        Vec3d dirHor = new Vec3d(dir.x, 0, dir.z);
        double dot = forwardHor.dotProduct(dirHor);
        double det = forwardHor.x * dirHor.z - forwardHor.z * dirHor.x;
        double angleHor = Math.atan2(det, dot);
        if (Math.abs(angleHor) > hFovRad / 2) return null;

        double x = screenWidth / 2.0 + (angleHor / (hFovRad / 2.0)) * (screenWidth / 2.0);

        double angleVert = Math.asin(dir.y) - Math.asin(forward.y);
        if (Math.abs(angleVert) > vFovRad / 2) return null;

        double y = screenHeight / 2.0 - (angleVert / (vFovRad / 2.0)) * (screenHeight / 2.0);

        return new Vec3d(x, y, distance);
    }

    private float calculateTransparencyByDistance(double horizontalDistance) {
        if (horizontalDistance <= config.getBeamCenterTolerance()) {
            return 1.0f; // Full opacity in the center
        } else {
            // Linear attenuation from center to edge
            float fadeProgress = (float)(horizontalDistance - config.getBeamCenterTolerance()) /
                    (float)(config.getFadeDistance() - config.getBeamCenterTolerance());
            return Math.max(config.getMinInteractionTransparency(), 1.0f - fadeProgress);
        }
    }

    /**
     * Performs raycast to check if point is visible (not obstructed by blocks)
     */
    private boolean isPointVisible(Vec3d cameraPos, Vec3d point) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        RaycastContext context = new RaycastContext(
                cameraPos,
                point,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        );

        HitResult result = client.world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }
}