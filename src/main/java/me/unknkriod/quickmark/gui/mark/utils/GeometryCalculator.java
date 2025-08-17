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

    /** Проверяет взаимодействие луча взгляда с вертикальным лучом метки */
    public BeamInteractionResult checkBeamInteraction(Vec3d cameraPos, Vec3d lookDirection, Mark mark) {
        BlockPos markPos = mark.getPosition();
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        double dx = lookDirection.x;
        double dy = lookDirection.y;
        double dz = lookDirection.z;

        // Если луч почти вертикальный, пропускаем проверку
        double den = dx * dx + dz * dz;
        if (den < 0.001) {
            return null;
        }

        double cx = cameraPos.x;
        double cz = cameraPos.z;

        // Находим параметр t, при котором луч взгляда ближе всего к вертикальной линии метки
        double t = ((markX - cx) * dx + (markZ - cz) * dz) / den;

        if (t < 0) return null; // Пересечение позади камеры

        // Находим ближайшую точку на луче взгляда
        double closestX = cx + t * dx;
        double closestZ = cz + t * dz;

        // Расстояние от луча взгляда до центральной линии метки по горизонтали
        double horizontalDistance = Math.sqrt(Math.pow(closestX - markX, 2) + Math.pow(closestZ - markZ, 2));

        // Динамический расчет fadeDistance на основе визуальной ширины луча
        MinecraftClient client = MinecraftClient.getInstance();
        double beamDist = calculateDistanceToBeam(cameraPos, markPos);
        float fov = client.options.getFov().getValue();
        int screenW = client.getWindow().getWidth();
        double halfWidth = getBeamWorldHalfWidth(beamDist, fov, screenW, mark.getType());

        double fadeDistance = halfWidth;
        double centerTolerance = halfWidth * (config.getBeamCenterTolerance() / config.getFadeDistance());

        // Проверяем, находится ли луч достаточно близко к центру
        if (horizontalDistance > fadeDistance) {
            return null; // Слишком далеко от центра
        }

        // Фиксируем Y позицию на высоте игрока
        double yPosition = cameraPos.y;

        // Вычисляем прозрачность в зависимости от расстояния до центра
        float transparency = calculateTransparency(horizontalDistance, centerTolerance, fadeDistance);

        return new BeamInteractionResult(yPosition, transparency);
    }

    private float calculateTransparency(double dist, double tol, double fade) {
        if (dist <= tol) {
            return 1.0f;
        } else {
            float fadeProgress = (float) (dist - tol) / (float) (fade - tol);
            return Math.max(config.getMinInteractionTransparency(), 1.0f - fadeProgress);
        }
    }

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

    /** Вычисляет расстояние от камеры до ближайшей точки на вертикальном луче метки */
    public double calculateDistanceToBeam(Vec3d cameraPos, BlockPos markPos) {
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        // Горизонтальное расстояние до центральной линии луча
        double horizontalDistance = Math.sqrt(Math.pow(cameraPos.x - markX, 2) + Math.pow(cameraPos.z - markZ, 2));

        // Y координата камеры
        double cameraY = cameraPos.y;

        // Ближайшая Y точка на луче (ограничиваем диапазоном луча)
        double closestY = Math.max(config.getDownRange(), Math.min(config.getMaxY(), cameraY));

        // Вертикальное расстояние до ближайшей точки на луче
        double verticalDistance = Math.abs(cameraY - closestY);

        // Общее расстояние до луча
        return Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
    }

    /** Рассчитывает прозрачность для статичных иконок */
    public float calculateStaticTransparency(Vec3d cameraPos, Vec3d markPos) {
        double markX = markPos.x;
        double markZ = markPos.z;
        double dx = cameraPos.x - markX;
        double dz = cameraPos.z - markZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        return calculateTransparencyByDistance(horizontalDistance);
    }

    /** Рассчитывает масштаб иконки в зависимости от расстояния */
    public float calculateIconScale(double distance) {
        // Ограничиваем расстояние
        distance = Math.min(distance, config.getMaxScaleDistance());

        // Рассчитываем коэффициент интерполяции (0 на близком расстоянии, 1 на максимальном)
        float factor = (float) (distance / config.getMaxScaleDistance());

        // Линейная интерполяция между MAX_SCALE и MIN_SCALE
        return config.getMaxIconScale() - factor * (config.getMaxIconScale() - config.getMinIconScale());
    }

    /** Проверяет, виден ли луч метки (хотя бы частично) */
    public boolean isBeamVisible(Vec3d cameraPos, Vec3d markPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        double markX = markPos.x;
        double markZ = markPos.z;

        // Определяем диапазон высот для проверки
        double minY = Math.max(config.getDownRange(), cameraPos.y - 10);
        double maxY = Math.min(config.getMaxY(), cameraPos.y + 20);

        // Шаг проверки - каждые 2 блока
        double step = 2.0;

        // Проверяем несколько точек вдоль луча
        for (double y = minY; y <= maxY; y += step) {
            Vec3d beamPoint = new Vec3d(markX, y, markZ);
            if (isPointVisible(cameraPos, beamPoint)) {
                return true;
            }
        }

        return false;
    }

    /** Проецирует точку мира на экран */
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
        if (dir.dotProduct(forward) <= 0) return null; // За камерой или под углом 90+

        // Горизонтальный угол
        Vec3d forwardHor = new Vec3d(forward.x, 0, forward.z);
        double forwardHorLen = forwardHor.length();
        if (forwardHorLen < 0.001) {
            return null; // Смотрим почти вертикально, проекция неоднозначна
        }
        forwardHor = forwardHor.normalize();
        Vec3d dirHor = new Vec3d(dir.x, 0, dir.z);
        double dot = forwardHor.dotProduct(dirHor);
        double det = forwardHor.x * dirHor.z - forwardHor.z * dirHor.x;
        double angleHor = Math.atan2(det, dot);
        if (Math.abs(angleHor) > hFovRad / 2) return null;

        double x = screenWidth / 2.0 + (angleHor / (hFovRad / 2.0)) * (screenWidth / 2.0);

        // Вертикальный угол (аппроксимация разницы pitch)
        double angleVert = Math.asin(dir.y) - Math.asin(forward.y);
        if (Math.abs(angleVert) > vFovRad / 2) return null;

        double y = screenHeight / 2.0 - (angleVert / (vFovRad / 2.0)) * (screenHeight / 2.0);

        return new Vec3d(x, y, distance);
    }

    private float calculateTransparencyByDistance(double horizontalDistance) {
        if (horizontalDistance <= config.getBeamCenterTolerance()) {
            return 1.0f; // Полная непрозрачность в центре
        } else {
            // Линейное затухание от центра до края
            float fadeProgress = (float)(horizontalDistance - config.getBeamCenterTolerance()) /
                    (float)(config.getFadeDistance() - config.getBeamCenterTolerance());
            return Math.max(config.getMinInteractionTransparency(), 1.0f - fadeProgress);
        }
    }

    /** Проверяет видимость конкретной точки */
    private boolean isPointVisible(Vec3d cameraPos, Vec3d point) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        // Делаем raycast от камеры к точке
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