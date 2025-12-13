package me.unknkriod.quickmark.input;

import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.network.NetworkSender;
import me.unknkriod.quickmark.gui.mark.renderers.MarkRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.mark.MarkType;

/**
 * Handles middle mouse button input for creating normal/danger marks via single/double-click detection.
 * Also handles mark removal on hover-click if owned by the player.
 */
public class MouseHandler {
    private static long lastPressTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 250; // ms for double-click detection
    private static final long MIN_MARK_INTERVAL = 100; // ms cooldown between marks
    private static boolean waitingForSecondClick = false;
    private static MarkType pendingMarkType = null;
    private static boolean middlePressed = false;
    private static boolean dangerMarkTriggered = false;
    private static long lastMarkTime = 0; // Timestamp of last mark creation

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;

            long currentTime = System.currentTimeMillis();
            boolean currentlyPressed = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

            // Spam protection: skip if too soon after last mark
            if (currentTime - lastMarkTime < MIN_MARK_INTERVAL) {
                if (!currentlyPressed && middlePressed) {
                    middlePressed = false;
                }
                return;
            }

            // Detect button press
            if (currentlyPressed && !middlePressed) {
                // First press
                if (lastPressTime == 0) {
                    lastPressTime = currentTime;
                    waitingForSecondClick = true;
                    pendingMarkType = MarkType.NORMAL;
                }
                // Second press within interval -> danger mark
                else if (waitingForSecondClick && currentTime - lastPressTime <= DOUBLE_CLICK_INTERVAL) {
                    pendingMarkType = MarkType.DANGER;
                    dangerMarkTriggered = true;
                    createMark(pendingMarkType);
                    resetState();
                }
            }

            // Detect button release
            if (!currentlyPressed && middlePressed) {
                // Single click (no double) -> normal mark after timeout
                if (waitingForSecondClick && !dangerMarkTriggered) {
                    if (currentTime - lastPressTime > DOUBLE_CLICK_INTERVAL) {
                        createMark(MarkType.NORMAL);
                    } else {
                        pendingMarkType = MarkType.NORMAL;
                    }
                }
            }

            // Timeout for single click
            if (waitingForSecondClick && !dangerMarkTriggered &&
                    currentTime - lastPressTime > DOUBLE_CLICK_INTERVAL) {

                if (pendingMarkType != null) {
                    createMark(pendingMarkType);
                }
                resetState();
            }

            middlePressed = currentlyPressed;
        });
    }

    private static void resetState() {
        lastPressTime = 0;
        waitingForSecondClick = false;
        pendingMarkType = null;
        dangerMarkTriggered = false;
    }

    private static void createMark(MarkType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        lastMarkTime = System.currentTimeMillis();

        // Check for hovered mark removal
        if (MarkRenderer.isHovered()) {
            Mark mark = MarkRenderer.getHoveredMark();
            if (mark != null) {
                boolean isOwner = client.player.getUuid().equals(mark.getPlayerId());

                if (isOwner) {
                    MarkManager.removeMark(mark.getId());
                    NetworkSender.sendRemoveCommand(mark.getId());
                }

                return;
            }
        }

        HitResult hitResult = client.player.raycast(7.0, 0.0f, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            resetState();
            return;
        }

        MarkManager.createMark(type, client.player.getUuid());
        resetState();
    }
}