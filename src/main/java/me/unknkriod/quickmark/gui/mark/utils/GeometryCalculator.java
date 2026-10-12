package me.unknkriod.quickmark.gui.mark.utils;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.mixin.GameRendererInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

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
        double beamDist = calculateDistanceToBeam(cameraPos, markPos, mark.getType());
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
    public double calculateDistanceToBeam(Vec3d cameraPos, BlockPos markPos, MarkType type) {
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        double horizontalDistance = Math.sqrt(Math.pow(cameraPos.x - markX, 2) + Math.pow(cameraPos.z - markZ, 2));

        // The beam hangs on the mark, so measure against the mark's own vertical extent rather
        // than an absolute world Y range.
        double beamBottom = markPos.getY() - config.getDownRange(type);
        double beamTop = markPos.getY() + config.getUpRange(type);
        double closestY = Math.max(beamBottom, Math.min(beamTop, cameraPos.y));

        double verticalDistance = Math.abs(cameraPos.y - closestY);
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
     * Projects a 3D world position to 2D screen coordinates.
     *
     * <p>This rebuilds the exact view-projection matrix the world is rendered
     * with: the FOV (slider + sprint/death/water effects), the hurt tilt, the
     * view bobbing, the camera rotation and the camera translation. The previous
     * hand-rolled implementation computed horizontal/vertical angles with
     * {@code asin}/{@code atan2} and mapped them <em>linearly</em> to pixels;
     * that is only accurate right at the screen centre and drifts as the camera
     * pitches/yaws. {@code GameRenderer#project(Vec3d)} fixes that, but it omits
     * the hurt tilt and view bobbing, which makes markers drift while moving —
     * hence the full rebuild here.</p>
     *
     * @return screen coordinates ({@code x}, {@code y}) with the distance from
     *         the camera stored in {@code z}, or {@code null} when the position
     *         is behind the camera or outside the view frustum.
     */
    public Vec3d projectToScreen(MinecraftClient client, Vec3d worldPos) {
        GameRenderer gameRenderer = client.gameRenderer;
        Camera camera = gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        double distance = worldPos.distanceTo(cameraPos);
        if (distance < 0.01) {
            return null;
        }

        GameRendererInvoker invoker = (GameRendererInvoker) gameRenderer;
        float tickProgress = camera.getLastTickProgress();

        // Projection matrix, tilted and bobbed exactly like the rendered world.
        Matrix4f projection = gameRenderer.getBasicProjectionMatrix(
                invoker.invokeGetFov(camera, tickProgress, true));

        MatrixStack viewTransforms = new MatrixStack();
        invoker.invokeTiltViewWhenHurt(viewTransforms, tickProgress);
        if (client.options.getBobView().getValue()) {
            invoker.invokeBobView(viewTransforms, tickProgress);
        }
        projection.mul(viewTransforms.peek().getPositionMatrix());

        // World -> camera space: move the camera to the origin, then rotate by
        // the conjugate of the camera rotation (what Minecraft's renderer uses).
        Matrix4f view = new Matrix4f()
                .rotation(camera.getRotation().conjugate(new Quaternionf()))
                .translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

        Vector4f clip = projection.mul(view).transform(new Vector4f(
                (float) worldPos.x, (float) worldPos.y, (float) worldPos.z, 1.0f
        ));

        // clip.w <= 0 means the point lies on or behind the camera plane.
        if (clip.w() <= 0.0f) {
            return null;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();

        // Outside the visible frustum (normalized device coordinates).
        if (Math.abs(ndcX) > 1.0f || Math.abs(ndcY) > 1.0f) {
            return null;
        }

        Window window = client.getWindow();
        double screenX = (ndcX * 0.5 + 0.5) * window.getScaledWidth();
        double screenY = (0.5 - ndcY * 0.5) * window.getScaledHeight();

        return new Vec3d(screenX, screenY, distance);
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
}