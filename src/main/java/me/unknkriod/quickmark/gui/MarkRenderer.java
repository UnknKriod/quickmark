package me.unknkriod.quickmark.gui;

import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

import static me.unknkriod.quickmark.gui.GuiComponent.MARK;
import static me.unknkriod.quickmark.gui.GuiComponent.WHITE_PIXEL;

public class MarkRenderer {
    private static final int MAX_Y = 255;
    private static final int DOWN_RANGE = 20;
    private static Mark hoveredMark = null;
    private static long lastParticleTime = 0;
    private static final long PARTICLE_INTERVAL = 200;
    private static final float BEAM_WIDTH = 3.0f;
    private static final int MARK_ICON_SIZE = 16;
    private static final int MARK_DISTANCE_OFFSET = 4;
    private static final double BEAM_CENTER_TOLERANCE = 0.9; // Допустимое отклонение от центра луча для появления иконки
    private static final double FADE_DISTANCE = 1.5; // Расстояние, на котором начинается затухание прозрачности

    private static final float DANGER_BEAM_WIDTH = 2.0f;
    private static final int DANGER_UP_RANGE = 150;
    private static final int DANGER_DOWN_RANGE = 15;
    private static final Color DANGER_BEAM_COLOR = new Color(181, 1, 1, 255);

    /** Рендер 3D — вызывается из WorldRenderEvents */
    public static void render3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (Mark mark : MarkManager.getAllMarks()) {
            if (mark.isExpired()) continue;

            BlockPos pos = mark.getPosition();
            renderVerticalBeam(matrices, vertexConsumers, pos, mark);

            if (mark.getType() == MarkType.DANGER) {
                spawnDangerParticles(pos);
            }
        }

        matrices.pop();
    }

    /** Рендер HUD — вызывается из HudRenderCallback */
    public static void renderHUD(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        hoveredMark = null;

        if (client.player == null || client.world == null) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        Vec3d lookDirection = client.getCameraEntity().getRotationVector();
        List<Mark> marks = MarkManager.getAllMarks();

        Mark beamHoveredMark = null;
        Vec3d beamHoveredPosition = null;
        float beamHoveredTransparency = 0f;

        // Сначала проверяем взаимодействие с лучами для интерактивных иконок
        for (Mark mark : marks) {
            if (mark.isExpired()) continue;

            BlockPos markBlockPos = mark.getPosition();
            BeamInteractionResult result = checkBeamInteraction(cameraPos, lookDirection, markBlockPos);

            if (result != null && result.transparency > 0.3f) {
                beamHoveredMark = mark;
                // Фиксируем позицию на высоте игрока
                beamHoveredPosition = new Vec3d(
                        markBlockPos.getX() + 0.5,
                        client.player.getEyeY(),
                        markBlockPos.getZ() + 0.5
                );
                beamHoveredTransparency = result.transparency;
                break; // Берем первое взаимодействие
            }
        }

        // Если есть взаимодействие с лучом, рисуем интерактивную иконку
        if (beamHoveredMark != null) {
            drawMarkIconAtBeamCenter(context, beamHoveredMark, beamHoveredPosition, cameraPos, beamHoveredTransparency);

            // Устанавливаем как наведенную метку если прозрачность достаточно высокая
            if (beamHoveredTransparency > 0.7f) {
                hoveredMark = beamHoveredMark;
                drawTooltipHUD(context, beamHoveredMark);
            }
        }

        // Теперь рисуем статичные иконки для всех меток, которые не взаимодействуют с лучом взгляда
        for (Mark mark : marks) {
            if (mark.isExpired()) continue;

            // Пропускаем метку, если она уже отображается как интерактивная
            if (mark.equals(beamHoveredMark)) {
                continue;
            }

            BlockPos markBlockPos = mark.getPosition();
            Vec3d markPos = new Vec3d(markBlockPos.getX() + 0.5, markBlockPos.getY() + 0.5, markBlockPos.getZ() + 0.5);

            drawStaticMarkIcon(context, mark, markPos, cameraPos);
        }
    }

    /** Класс для результата взаимодействия с лучом */
    private static class BeamInteractionResult {
        public final double yPosition;
        public final float transparency;

        public BeamInteractionResult(double yPosition, float transparency) {
            this.yPosition = yPosition;
            this.transparency = transparency;
        }
    }

    /** Проверяет взаимодействие луча взгляда с вертикальным лучом метки */
    private static BeamInteractionResult checkBeamInteraction(Vec3d cameraPos, Vec3d lookDirection, BlockPos markPos) {
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        double dx = lookDirection.x;
        double dy = lookDirection.y;
        double dz = lookDirection.z;

        // Если луч почти вертикальный, пропускаем проверку
        if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
            return null;
        }

        double cx = cameraPos.x;
        double cz = cameraPos.z;

        // Находим параметр t, при котором луч взгляда ближе всего к вертикальной линии метки
        double t = ((markX - cx) * dx + (markZ - cz) * dz) / (dx * dx + dz * dz);

        if (t < 0) return null; // Пересечение позади камеры

        // Находим ближайшую точку на луче взгляда
        Vec3d closestPoint = cameraPos.add(lookDirection.multiply(t));

        // Расстояние от луча взгляда до центральной линии метки по горизонтали
        double horizontalDistance = Math.sqrt(Math.pow(closestPoint.x - markX, 2) + Math.pow(closestPoint.z - markZ, 2));

        // Проверяем, находится ли луч достаточно близко к центру
        if (horizontalDistance > FADE_DISTANCE) {
            return null; // Слишком далеко от центра
        }

        // ФИКСИРУЕМ Y ПОЗИЦИЮ НА ВЫСОТЕ ИГРОКА
        double yPosition = cameraPos.y;

        // Вычисляем прозрачность в зависимости от расстояния до центра
        float transparency;
        if (horizontalDistance <= BEAM_CENTER_TOLERANCE) {
            transparency = 1.0f; // Полная непрозрачность в центре
        } else {
            // Линейное затухание от центра до края
            float fadeProgress = (float)(horizontalDistance - BEAM_CENTER_TOLERANCE) / (float)(FADE_DISTANCE - BEAM_CENTER_TOLERANCE);
            transparency = Math.max(0.3f, 1.0f - fadeProgress); // Минимальная прозрачность 30%
        }

        return new BeamInteractionResult(yPosition, transparency);
    }

    /** Вычисляет расстояние от камеры до ближайшей точки на вертикальном луче метки */
    private static double calculateDistanceToBeam(Vec3d cameraPos, BlockPos markPos) {
        double markX = markPos.getX() + 0.5;
        double markZ = markPos.getZ() + 0.5;

        // Горизонтальное расстояние до центральной линии луча
        double horizontalDistance = Math.sqrt(Math.pow(cameraPos.x - markX, 2) + Math.pow(cameraPos.z - markZ, 2));

        // Y координата камеры
        double cameraY = cameraPos.y;

        // Ближайшая Y точка на луче (ограничиваем диапазоном луча)
        double closestY = Math.max(DOWN_RANGE, Math.min(MAX_Y, cameraY));

        // Вертикальное расстояние до ближайшей точки на луче
        double verticalDistance = Math.abs(cameraY - closestY);

        // Общее расстояние до луча
        return Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
    }

    /** Отрисовка интерактивной иконки метки в центре луча с учетом прозрачности */
    private static void drawMarkIconAtBeamCenter(DrawContext context, Mark mark, Vec3d iconPosition, Vec3d cameraPos, float transparency) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d screenPos = projectToScreen(client, iconPosition);
        if (screenPos == null) return;

        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        // Получаем цвет команды для метки
        Color markColor = getColorForMark(mark);
        int baseColorInt = (markColor.getRed() << 16) | (markColor.getGreen() << 8) | markColor.getBlue();

        // Применяем прозрачность к цветам
        int glowAlpha = (int)(0x80 * transparency);
        int borderAlpha = (int)(0xFF * transparency);

        // Размер иконки зависит от прозрачности
        int iconSize = (int)(MARK_ICON_SIZE + (transparency * 8)); // От 16 до 24 пикселей
        int glowSize = iconSize + 6;
        int halfGlowSize = glowSize / 2;
        int halfSize = iconSize / 2;

        // Рисуем свечение с учетом прозрачности
        context.fill(x - halfGlowSize, y - halfGlowSize,
                x + halfGlowSize, y + halfGlowSize,
                (baseColorInt & 0xFFFFFF) | (glowAlpha << 24));

        // Рисуем иконку
        GuiComponent.drawTexture(context, MARK,
                x - halfSize,
                y - halfSize,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize);

        // Рисуем обводку с учетом прозрачности
        int borderThickness = transparency > 0.8f ? 2 : 1;
        context.drawBorder(x - halfSize - borderThickness, y - halfSize - borderThickness,
                iconSize + borderThickness * 2, iconSize + borderThickness * 2,
                (baseColorInt & 0xFFFFFF) | (borderAlpha << 24));

        // Расстояние до луча, а не до иконки
        double distance = calculateDistanceToBeam(cameraPos, mark.getPosition());
        String distanceText = Math.round(distance) + "м";
        int textWidth = client.textRenderer.getWidth(distanceText);

        // Позиция текста под иконкой
        int textX = x - textWidth / 2;
        int textY = y + halfSize + MARK_DISTANCE_OFFSET;

        // Прозрачность фона и текста
        int backgroundAlpha = (int)(0xE0 * transparency);
        int textAlpha = (int)(0xFF * transparency);

        // Рисуем фон для текста с учетом прозрачности
        int padding = 3;
        context.fill(textX - padding, textY - 1,
                textX + textWidth + padding,
                textY + client.textRenderer.fontHeight + 1,
                backgroundAlpha << 24);

        // Цвет текста в зависимости от прозрачности
        int textColor = transparency > 0.8f ? 0xFFFF55 : 0xFFFFFF;
        textColor = (textColor & 0xFFFFFF) | (textAlpha << 24);
        int outlineColor = (textAlpha << 24);

        // Рисуем текст с обводкой и прозрачностью
        drawTextWithOutlineAndAlpha(context, client.textRenderer, distanceText, textX, textY, textColor, outlineColor, transparency);
    }

    /** Отрисовка статичной иконки метки (фиксированная позиция на экране) */
    private static void drawStaticMarkIcon(DrawContext context, Mark mark, Vec3d markPos, Vec3d cameraPos) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Проверяем, виден ли луч метки
        boolean isVisible = isBeamVisible(cameraPos, markPos);

        // Если луч виден, не показываем статичную иконку
        if (isVisible) {
            return;
        }

        // Вычисляем позицию на горизонтальном центре луча на высоте игрока
        BlockPos markBlockPos = mark.getPosition();
        Vec3d projectedPos = new Vec3d(
                markBlockPos.getX() + 0.5,
                cameraPos.y, // Высота игрока
                markBlockPos.getZ() + 0.5
        );

        // Проецируем эту позицию на экран
        Vec3d screenPos = projectToScreen(client, projectedPos);
        if (screenPos == null) return;

        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        // Рассчитываем горизонтальное расстояние до луча метки
        double markX = markPos.x;
        double markZ = markPos.z;
        double dx = cameraPos.x - markX;
        double dz = cameraPos.z - markZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Вычисляем прозрачность аналогично интерактивным иконкам
        float transparency;
        if (horizontalDistance <= BEAM_CENTER_TOLERANCE) {
            transparency = 1.0f;
        } else if (horizontalDistance <= FADE_DISTANCE) {
            float fadeProgress = (float)(horizontalDistance - BEAM_CENTER_TOLERANCE) /
                    (float)(FADE_DISTANCE - BEAM_CENTER_TOLERANCE);
            transparency = Math.max(0.3f, 1.0f - fadeProgress);
        } else {
            transparency = 0.3f;
        }

        // Рассчитываем масштаб иконки в зависимости от расстояния
        double distance = calculateDistanceToBeam(cameraPos, mark.getPosition());
        float scaleFactor = calculateIconScale(distance);

        Color markColor = getColorForMark(mark);
        int colorInt = (markColor.getRed() << 16) | (markColor.getGreen() << 8) | markColor.getBlue();

        // Рассчитываем альфа-каналы на основе прозрачности
        int glowAlpha = (int)(0x30 * transparency);
        int borderAlpha = (int)(0xAA * transparency);
        int backgroundAlpha = (int)(0xC0 * transparency);
        int textAlpha = (int)(0xFF * transparency);

        int scaledIconSize = (int) (MARK_ICON_SIZE * scaleFactor);
        int scaledGlowSize = scaledIconSize + 2;
        int halfScaledGlowSize = scaledGlowSize / 2;
        int halfScaledSize = scaledIconSize / 2;

        context.fill(x - halfScaledGlowSize, y - halfScaledGlowSize,
                x + halfScaledGlowSize, y + halfScaledGlowSize,
                (colorInt & 0xFFFFFF) | (glowAlpha << 24));

        GuiComponent.drawTexture(context, MARK,
                x - halfScaledSize,
                y - halfScaledSize,
                0, 0,
                scaledIconSize, scaledIconSize,
                scaledIconSize, scaledIconSize);

        context.drawBorder(x - halfScaledSize - 1, y - halfScaledSize - 1,
                scaledIconSize + 2, scaledIconSize + 2,
                (colorInt & 0xFFFFFF) | (borderAlpha << 24));

        String distanceText = Math.round(distance) + "м";
        int textWidth = client.textRenderer.getWidth(distanceText);
        int textX = x - textWidth / 2;
        int textY = y + halfScaledSize + MARK_DISTANCE_OFFSET;

        // Фон для текста с учетом прозрачности
        int padding = 2;
        context.fill(textX - padding, textY - 1,
                textX + textWidth + padding,
                textY + client.textRenderer.fontHeight + 1,
                (backgroundAlpha << 24));

        // Цвета текста
        int textColor = 0xFFFFFF | (textAlpha << 24);
        int outlineColor = textAlpha << 24;

        // Рисуем текст с обводкой и прозрачностью
        drawTextWithOutlineAndAlpha(context, client.textRenderer,
                distanceText, textX, textY, textColor, outlineColor, transparency);
    }

    /** Рассчитывает масштаб иконки в зависимости от расстояния */
    private static float calculateIconScale(double distance) {
        // Настройки масштабирования
        final float MIN_SCALE = 0.65f;
        final float MAX_SCALE = 1.75f;
        final float MAX_DISTANCE = 100.0f; // Максимальное расстояние для масштабирования

        // Ограничиваем расстояние
        distance = Math.min(distance, MAX_DISTANCE);

        // Рассчитываем коэффициент интерполяции (0 на близком расстоянии, 1 на максимальном)
        float factor = (float) (distance / MAX_DISTANCE);

        // Линейная интерполяция между MAX_SCALE и MIN_SCALE
        return MAX_SCALE - factor * (MAX_SCALE - MIN_SCALE);
    }

    /** Проверяет, виден ли луч метки (хотя бы частично) */
    private static boolean isBeamVisible(Vec3d cameraPos, Vec3d markPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        double markX = markPos.x;
        double markZ = markPos.z;

        // Определяем диапазон высот для проверки
        double minY = Math.max(DOWN_RANGE, cameraPos.y - 10);
        double maxY = Math.min(MAX_Y, cameraPos.y + 20);

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

    /** Проверяет видимость конкретной точки */
    private static boolean isPointVisible(Vec3d cameraPos, Vec3d point) {
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

    private static void spawnDangerParticles(BlockPos pos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastParticleTime < PARTICLE_INTERVAL) return;
        lastParticleTime = currentTime;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        world.addParticleClient(ParticleTypes.ANGRY_VILLAGER, x, y, z, 0, 0.1, 0);
        for (int i = 0; i < 3; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
            world.addParticleClient(ParticleTypes.LAVA, x + offsetX, y, z + offsetZ, 0, 0.05, 0);
        }
    }

    private static void renderVerticalBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                           BlockPos pos, Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        // Определяем параметры в зависимости от типа метки
        float beamWidth = (mark.getType() == MarkType.DANGER) ? DANGER_BEAM_WIDTH : BEAM_WIDTH;
        int upRange = (mark.getType() == MarkType.DANGER) ? DANGER_UP_RANGE : MAX_Y;
        int downRange = (mark.getType() == MarkType.DANGER) ? DANGER_DOWN_RANGE : DOWN_RANGE;
        Color color = (mark.getType() == MarkType.DANGER) ? DANGER_BEAM_COLOR : getColorForMark(mark);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_PIXEL));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        double xCenter = pos.getX() + 0.5;
        double zCenter = pos.getZ() + 0.5;
        double y1 = downRange;
        double y2 = upRange;

        // Направление к камере для ориентации луча
        Vec3d toCamera = new Vec3d(cameraPos.x - xCenter, 0, cameraPos.z - zCenter);
        if (toCamera.lengthSquared() < 1e-4) {
            toCamera = new Vec3d(1, 0, 0);
        } else {
            toCamera = toCamera.normalize();
        }

        // Перпендикулярный вектор для ширины луча
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = toCamera.crossProduct(up).normalize().multiply(beamWidth);

        // Центральные точки луча
        Vec3d bottomCenter = new Vec3d(xCenter, y1, zCenter);
        Vec3d topCenter = new Vec3d(xCenter, y2, zCenter);

        // Крайние точки луча
        Vec3d bottomLeft = bottomCenter.subtract(right);
        Vec3d bottomRight = bottomCenter.add(right);
        Vec3d topLeft = topCenter.subtract(right);
        Vec3d topRight = topCenter.add(right);

        // Альфа-значения для градиента
        float alphaEdge = 0.85f;    // Края менее прозрачны
        float alphaCenter = 0.45f;   // Центр более прозрачен

        // Рисуем левую половину луча с градиентом
        consumer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
                .color(r, g, b, alphaEdge)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
                .color(r, g, b, alphaEdge)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) topCenter.x, (float) topCenter.y, (float) topCenter.z)
                .color(r, g, b, alphaCenter)
                .texture(0.5f, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) bottomCenter.x, (float) bottomCenter.y, (float) bottomCenter.z)
                .color(r, g, b, alphaCenter)
                .texture(0.5f, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        // Рисуем правую половину луча с градиентом
        consumer.vertex(matrix, (float) bottomCenter.x, (float) bottomCenter.y, (float) bottomCenter.z)
                .color(r, g, b, alphaCenter)
                .texture(0.5f, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) topCenter.x, (float) topCenter.y, (float) topCenter.z)
                .color(r, g, b, alphaCenter)
                .texture(0.5f, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
                .color(r, g, b, alphaEdge)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        consumer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
                .color(r, g, b, alphaEdge)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);
    }

    private static Color getColorForMark(Mark mark) {
        int position = TeamManager.getPlayerPosition(mark.getPlayerId());
        if (position == -1) {
            return new Color(255, 255, 255, 255);
        }

        int argb = TeamManager.getColorForPosition(position);
        return new Color(
                ColorHelper.getRed(argb),
                ColorHelper.getGreen(argb),
                ColorHelper.getBlue(argb),
                255
        );
    }

    /** Улучшенный тултип с лучшим дизайном */
    private static void drawTooltipHUD(DrawContext context, Mark mark) {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();

        String playerName = TeamManager.getPlayerName(mark.getPlayerId());

        Color markColor;
        Text line1;
        Text line2 = Text.translatableWithFallback("quickmark.tooltip.line2", "Click the middle mouse button to delete");

        if (mark.getType() == MarkType.DANGER) {
            line1 = Text.translatableWithFallback("quickmark.tooltip.danger.line1", "DANGER!");;
            markColor = DANGER_BEAM_COLOR;
        } else {
            line1 = Text.translatableWithFallback("quickmark.tooltip.line1", playerName + "`s mark", playerName);;
            markColor = getColorForMark(mark);
        }

        int textWidth1 = client.textRenderer.getWidth(line1);
        int textWidth2 = client.textRenderer.getWidth(line2);
        int maxWidth = Math.max(textWidth1, textWidth2);
        int textHeight = client.textRenderer.fontHeight;
        int padding = 8;

        // Позиция у прицела (центр экрана)
        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;

        // Смещение тултипа относительно прицела
        int offsetX = 20;
        int offsetY = -30;

        int boxX = centerX + offsetX;
        int boxY = centerY + offsetY;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = textHeight * 2 + padding * 2 + 2;

        // Проверка границ экрана
        if (boxX + boxWidth > window.getScaledWidth()) {
            boxX = centerX - offsetX - boxWidth;
        }
        if (boxY < 0) {
            boxY = centerY - offsetY;
        }
        if (boxY + boxHeight > window.getScaledHeight()) {
            boxY = window.getScaledHeight() - boxHeight - 10;
        }

        // Считаем яркость цвета команды
        float brightness = (0.299f * markColor.getRed() +
                0.587f * markColor.getGreen() +
                0.114f * markColor.getBlue()) / 255f;

        // Если цвет светлый — используем более тёмный фон, иначе — слегка тёмный
        int backgroundColor = (brightness > 0.6f) ? 0xCC000000 : 0xCC1A1A1A;

        // Цвет рамки оставляем цветом команды
        int borderColor = markColor.getRGB() | 0xFF000000;

        // Рисуем тень тултипа
        context.fill(boxX + 2, boxY + 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 0x80000000);

        // Рисуем фон тултипа
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, backgroundColor);

        // Рисуем обводку
        context.drawBorder(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, borderColor);

        // Рисуем внутреннюю обводку для объема
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFF333333);

        // Текст с обводкой для лучшей читаемости
        int text1X = boxX + padding + (maxWidth - textWidth1) / 2;
        int text1Y = boxY + padding;
        int text2X = boxX + padding + (maxWidth - textWidth2) / 2;
        int text2Y = boxY + padding + textHeight + 2;

        int textColor = (mark.getType() == MarkType.DANGER) ? 0xFFFF0000 : 0xFFFFFFFF;

        context.drawText(client.textRenderer,
                line1,
                text1X, text1Y,
                textColor, false);

        context.drawText(client.textRenderer,
                line2,
                text2X, text2Y,
                textColor, false);
    }

    /** Вспомогательный метод для рисования текста с обводкой и альфа-каналом */
    private static void drawTextWithOutlineAndAlpha(DrawContext context, TextRenderer textRenderer,
                                                    String text, int x, int y, int textColor, int outlineColor, float alpha) {
        // Если прозрачность очень низкая, рисуем обычным способом
        if (alpha < 0.5f) {
            drawTextWithOutline(context, textRenderer, text, x, y, textColor, outlineColor);
            return;
        }

        // Рисуем обводку с альфа-каналом
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                context.drawText(textRenderer, text, x + dx, y + dy, outlineColor, false);
            }
        }
        // Рисуем основной текст с альфа-каналом
        context.drawText(textRenderer, text, x, y, textColor, false);
    }

    /** Вспомогательный метод для рисования текста с обводкой */
    private static void drawTextWithOutline(DrawContext context, TextRenderer textRenderer,
                                            String text, int x, int y, int textColor, int outlineColor) {
        // Рисуем обводку
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                context.drawText(textRenderer, text, x + dx, y + dy, outlineColor, false);
            }
        }
        // Рисуем основной текст
        context.drawText(textRenderer, text, x, y, textColor, false);
    }

    private static Vec3d projectToScreen(MinecraftClient client, Vec3d worldPos) {
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

    public static boolean isHovered() {
        return hoveredMark != null;
    }

    public static Mark getHoveredMark() {
        return hoveredMark;
    }
}