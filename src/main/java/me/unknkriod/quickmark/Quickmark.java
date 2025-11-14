package me.unknkriod.quickmark;

import me.unknkriod.quickmark.gui.overlay.InfoOverlayRenderer;
import me.unknkriod.quickmark.gui.overlay.InviteOverlayRenderer;
import me.unknkriod.quickmark.gui.overlay.SuccessOverlayRenderer;
import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.input.MouseHandler;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.network.NetworkReceiver;
import me.unknkriod.quickmark.gui.mark.renderers.MarkRenderer;
import me.unknkriod.quickmark.network.NetworkSender;
import me.unknkriod.quickmark.team.TeamCommand;
import me.unknkriod.quickmark.team.TeamManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Quickmark implements ClientModInitializer {
    public static final String MOD_ID = "quickmark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyBinding acceptInvitationKey;

    public static KeyBinding getAcceptInvitationKey() {
        return acceptInvitationKey;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("QuickMark mod initialized!");

        // Sounds
        Registry.register(Registries.SOUND_EVENT, Identifier.of(MOD_ID, "normal_ping"),
                SoundEvent.of(Identifier.of(MOD_ID, "normal_ping")));
        Registry.register(Registries.SOUND_EVENT, Identifier.of(MOD_ID, "danger_ping"),
                SoundEvent.of(Identifier.of(MOD_ID, "danger_ping")));
        Registry.register(Registries.SOUND_EVENT, Identifier.of(MOD_ID, "invite"),
                SoundEvent.of(Identifier.of(MOD_ID, "invite")));

        acceptInvitationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickmark.accept_invitation",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.quickmark.main"
        ));

        MouseHandler.initialize();
        MarkManager.initialize();

        // Регистрируем команды
        ClientCommandRegistrationCallback.EVENT.register(TeamCommand::register);
        TeamManager.initialize();

        // WorldRenderEvents — 3D метки
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> {
            MarkRenderer.render3D(context.matrixStack(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());
        });

        // HudRenderCallback — тултип
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            TeamHudRenderer.render(context);
            InviteOverlayRenderer.INSTANCE.render(context);
            SuccessOverlayRenderer.INSTANCE.render(context);
            InfoOverlayRenderer.INSTANCE.render(context);
            MarkRenderer.renderHUD(context);
        });


        // Обработка кликов по приглашениям
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                TeamManager.updateTeamHealth();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, client) -> {
            if (client.player != null) {
                TeamManager.removePlayer(client.player.getUuid());
                NetworkSender.sendTeamUpdate();
            }

            MarkManager.clearAllMarks();
            NetworkReceiver.clearAuthorizedPlayers();
            TeamManager.clearTeam();
            TeamManager.clearPendingInvitations();
        });
    }

    public static void log(String message) {
        LOGGER.info(message);
    }
}
