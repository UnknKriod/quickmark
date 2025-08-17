package me.unknkriod.quickmark.gui.mark.renderers;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

import static me.unknkriod.quickmark.gui.GuiComponent.WHITE_PIXEL;

public class BeamRenderer {
    private final MarkRenderConfig config;

    public BeamRenderer(MarkRenderConfig config) {
        this.config = config;
    }

    public void renderVerticalBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   BlockPos pos, Mark mark, Vec3d cameraPos) {
        // Определяем параметры в зависимости от типа метки
        float beamWidth = (mark.getType() == MarkType.DANGER) ?
                config.getDangerBeamWidth() : config.getBeamWidth();
        int upRange = (mark.getType() == MarkType.DANGER) ?
                config.getDangerUpRange() : config.getMaxY();
        int downRange = (mark.getType() == MarkType.DANGER) ?
                config.getDangerDownRange() : config.getDownRange();
        Color color = (mark.getType() == MarkType.DANGER) ?
                config.getDangerBeamColor() : MarkRenderer.getColorForMark(mark);

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
        addBeamQuad(consumer, matrix,
                bottomLeft, topLeft, topCenter, bottomCenter,
                r, g, b, alphaEdge, alphaCenter);

        // Рисуем правую половину луча с градиентом
        addBeamQuad(consumer, matrix,
                bottomCenter, topCenter, topRight, bottomRight,
                r, g, b, alphaCenter, alphaEdge);
    }

    private void addBeamQuad(VertexConsumer consumer, Matrix4f matrix,
                             Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4,
                             float r, float g, float b, float alpha1, float alpha2) {
        // Первая вершина
        consumer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
                .color(r, g, b, alpha1)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        // Вторая вершина
        consumer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
                .color(r, g, b, alpha1)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        // Третья вершина
        consumer.vertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z)
                .color(r, g, b, alpha2)
                .texture(0.5f, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);

        // Четвертая вершина
        consumer.vertex(matrix, (float) v4.x, (float) v4.y, (float) v4.z)
                .color(r, g, b, alpha2)
                .texture(0.5f, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0, 1, 0);
    }
}