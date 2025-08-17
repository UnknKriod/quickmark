package me.unknkriod.quickmark.input;

import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.network.NetworkSender;
import me.unknkriod.quickmark.gui.mark.renderers.MarkRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.mark.MarkType;

public class MouseHandler {
    private static long lastPressTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 250; // Интервал для двойного клика
    private static final long MIN_MARK_INTERVAL = 100; // Минимальный интервал между метками
    private static boolean waitingForSecondClick = false;
    private static MarkType pendingMarkType = null;
    private static boolean middlePressed = false;
    private static boolean dangerMarkTriggered = false;
    private static long lastMarkTime = 0; // Время последней установленной метки

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;

            long currentTime = System.currentTimeMillis();
            boolean currentlyPressed = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

            // Защита от спама - пропускаем обработку если не прошло достаточно времени
            if (currentTime - lastMarkTime < MIN_MARK_INTERVAL) {
                // Если кнопка отпущена - обновляем состояние нажатия
                if (!currentlyPressed && middlePressed) {
                    middlePressed = false;
                }
                return;
            }

            // Обработка нажатия кнопки
            if (currentlyPressed && !middlePressed) {
                // Первое нажатие
                if (lastPressTime == 0) {
                    lastPressTime = currentTime;
                    waitingForSecondClick = true;
                    pendingMarkType = MarkType.NORMAL;
                }
                // Второе нажатие в пределах интервала
                else if (waitingForSecondClick && currentTime - lastPressTime <= DOUBLE_CLICK_INTERVAL) {
                    pendingMarkType = MarkType.DANGER;
                    dangerMarkTriggered = true;
                    createMark(pendingMarkType);
                    resetState();
                }
            }

            // Обработка отпускания кнопки
            if (!currentlyPressed && middlePressed) {
                // Если было одиночное нажатие и не было двойного клика
                if (waitingForSecondClick && !dangerMarkTriggered) {
                    // Проверяем, не истек ли таймаут ожидания второго клика
                    if (currentTime - lastPressTime > DOUBLE_CLICK_INTERVAL) {
                        createMark(MarkType.NORMAL);
                    } else {
                        // Устанавливаем флаг, что нужно создать метку после таймаута
                        pendingMarkType = MarkType.NORMAL;
                    }
                }
            }

            // Обработка таймаута для одиночного клика
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

        // Запоминаем время создания метки для защиты от спама
        lastMarkTime = System.currentTimeMillis();

        // Проверка на удаление при наведении
        if (MarkRenderer.isHovered()) {
            Mark mark = MarkRenderer.getHoveredMark();
            if (mark != null) {
                MarkManager.removeMark(mark.getId());
                NetworkSender.sendRemoveCommand(mark.getId());
                return;
            }
        }

        MarkManager.createMark(type, client.player.getUuid());
        resetState();
    }
}