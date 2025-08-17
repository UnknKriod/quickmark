package me.unknkriod.quickmark.gui.mark.renderers;

import me.unknkriod.quickmark.gui.mark.MarkRenderConfig;
import me.unknkriod.quickmark.gui.mark.utils.BeamInteractionResult;
import me.unknkriod.quickmark.gui.mark.utils.GeometryCalculator;
import me.unknkriod.quickmark.gui.mark.utils.IconRenderData;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.team.TeamManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.List;

public class MarkRenderer {
    private static final MarkRenderConfig CONFIG = new MarkRenderConfig();
    private static final BeamRenderer BEAM_RENDERER = new BeamRenderer(CONFIG);
    private static final IconRenderer ICON_RENDERER = new IconRenderer(CONFIG);
    private static final TooltipRenderer TOOLTIP_RENDERER = new TooltipRenderer();
    private static final GeometryCalculator GEOMETRY_CALC = new GeometryCalculator(CONFIG);

    private static Mark hoveredMark = null;
    private static long lastParticleTime = 0;

    /** Рендер 3D — вызывается из WorldRenderEvents */
    public static void render3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float fov = client.options.getFov().getValue();
        int screenWidth = client.getWindow().getWidth();

        for (Mark mark : MarkManager.getAllMarks()) {
            if (mark.isExpired()) continue;

            BlockPos pos = mark.getPosition();
            BEAM_RENDERER.renderVerticalBeam(
                    matrices, vertexConsumers, pos, mark, cameraPos, fov, screenWidth
            );

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

        // Проверяем взаимодействие с лучами для интерактивных иконок
        Mark beamHoveredMark = findBeamHoveredMark(marks, cameraPos, lookDirection);

        // Рисуем интерактивную иконку, если есть взаимодействие с лучом
        if (beamHoveredMark != null) {
            renderInteractiveIcon(context, beamHoveredMark, cameraPos, lookDirection, client);

            if (hoveredMark != null) {
                TOOLTIP_RENDERER.drawTooltip(context, hoveredMark);
            }
        }

        // Рисуем статичные иконки для остальных меток
        renderStaticIcons(context, marks, beamHoveredMark, cameraPos, client);
    }

    private static Mark findBeamHoveredMark(List<Mark> marks, Vec3d cameraPos, Vec3d lookDirection) {
        for (Mark mark : marks) {
            if (mark.isExpired()) continue;

            BlockPos markBlockPos = mark.getPosition();
            BeamInteractionResult result = GEOMETRY_CALC.checkBeamInteraction(cameraPos, lookDirection, markBlockPos);

            if (result != null && result.getTransparency() > CONFIG.getMinInteractionTransparency()) {
                return mark;
            }
        }
        return null;
    }

    private static void renderInteractiveIcon(DrawContext context, Mark mark, Vec3d cameraPos, Vec3d lookDirection, MinecraftClient client) {
        BlockPos markBlockPos = mark.getPosition();
        BeamInteractionResult result = GEOMETRY_CALC.checkBeamInteraction(cameraPos, lookDirection, markBlockPos);

        if (result == null) return;

        Vec3d iconPosition = new Vec3d(
                markBlockPos.getX() + 0.5,
                client.player.getEyeY(),
                markBlockPos.getZ() + 0.5
        );

        IconRenderData iconData = new IconRenderData.Builder()
                .mark(mark)
                .position(iconPosition)
                .cameraPos(cameraPos)
                .transparency(result.getTransparency())
                .interactive(true)
                .build();

        ICON_RENDERER.drawMarkIcon(context, iconData);
        checkIconHover(iconData, client);
    }

    private static void renderStaticIcons(DrawContext context, List<Mark> marks, Mark beamHoveredMark, Vec3d cameraPos, MinecraftClient client) {
        for (Mark mark : marks) {
            if (mark.isExpired() || mark.equals(beamHoveredMark)) {
                continue;
            }

            BlockPos markBlockPos = mark.getPosition();
            Vec3d markPos = new Vec3d(markBlockPos.getX() + 0.5, markBlockPos.getY() + 0.5, markBlockPos.getZ() + 0.5);

            // Проверяем, виден ли луч метки
            if (GEOMETRY_CALC.isBeamVisible(cameraPos, markPos)) {
                continue;
            }

            Vec3d projectedPos = new Vec3d(
                    markBlockPos.getX() + 0.5,
                    cameraPos.y,
                    markBlockPos.getZ() + 0.5
            );

            double distance = GEOMETRY_CALC.calculateDistanceToBeam(cameraPos, markBlockPos);
            float transparency = GEOMETRY_CALC.calculateStaticTransparency(cameraPos, markPos);
            float scale = GEOMETRY_CALC.calculateIconScale(distance);

            IconRenderData iconData = new IconRenderData.Builder()
                    .mark(mark)
                    .position(projectedPos)
                    .cameraPos(cameraPos)
                    .transparency(transparency)
                    .scale(scale)
                    .interactive(false)
                    .build();

            ICON_RENDERER.drawMarkIcon(context, iconData);
            checkIconHover(iconData, client);
        }
    }

    private static void checkIconHover(IconRenderData iconData, MinecraftClient client) {
        Vec3d screenPos = GEOMETRY_CALC.projectToScreen(client, iconData.getPosition());
        if (screenPos == null) return;

        int x = (int) screenPos.x;
        int y = (int) screenPos.y;
        int iconSize = (int) (CONFIG.getMarkIconSize() * iconData.getScale());
        int halfSize = iconSize / 2;

        double mouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
        double mouseY = client.mouse.getY() / client.getWindow().getScaleFactor();

        int hoverThreshold = CONFIG.getHoverThreshold();
        if (mouseX >= x - halfSize - hoverThreshold && mouseX <= x + halfSize + hoverThreshold &&
                mouseY >= y - halfSize - hoverThreshold && mouseY <= y + halfSize + hoverThreshold) {
            hoveredMark = iconData.getMark();
        }
    }

    private static void spawnDangerParticles(BlockPos pos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastParticleTime < CONFIG.getParticleInterval()) return;
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

    public static Color getColorForMark(Mark mark) {
        int position = TeamManager.getPlayerPosition(mark.getPlayerId());
        if (position == -1) {
            return CONFIG.getDefaultColor();
        }

        int argb = TeamManager.getColorForPosition(position);
        return new Color(
                ColorHelper.getRed(argb),
                ColorHelper.getGreen(argb),
                ColorHelper.getBlue(argb),
                255
        );
    }

    public static boolean isHovered() {
        return hoveredMark != null;
    }

    public static Mark getHoveredMark() {
        return hoveredMark;
    }
}