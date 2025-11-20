package me.unknkriod.quickmark;

import me.unknkriod.quickmark.gui.overlay.InfoOverlayRenderer;
import me.unknkriod.quickmark.gui.overlay.InviteOverlayRenderer;
import me.unknkriod.quickmark.gui.overlay.SuccessOverlayRenderer;
import me.unknkriod.quickmark.gui.TeamHudRenderer;
import me.unknkriod.quickmark.input.MouseHandler;
import me.unknkriod.quickmark.mark.MarkManager;
import me.unknkriod.quickmark.network.*;
import me.unknkriod.quickmark.gui.mark.renderers.MarkRenderer;
import me.unknkriod.quickmark.team.TeamCommand;
import me.unknkriod.quickmark.team.TeamManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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

/**
 * Main entry point for the Quickmark client-side mod.
 * Handles initialization of networking, keybindings, renderers, and event registrations.
 */
public class Quickmark implements ClientModInitializer {
    public static final String MOD_ID = "quickmark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding acceptInvitationKey;
    private static int pluginCheckTimer = 0;
    private static boolean waitingForPluginCheck = false;

    public static KeyBinding getAcceptInvitationKey() {
        return acceptInvitationKey;
    }

    @Override
    public void onInitializeClient() {
        log("Quickmark initialing started");

        // Set up server-side networking for single-player/LAN servers
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            log("Initializing Quickmark server networking for integrated server");
            ServerNetworking.initialize();
            ServerNetworking.registerTickHandler();
        });

        registerPacketTypes();

        // Register custom sound events
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

        // Register client-side commands
        ClientCommandRegistrationCallback.EVENT.register(TeamCommand::register);

        TeamManager.initialize();

        // Register 3D mark rendering after translucent blocks
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> {
            MarkRenderer.render3D(context.matrixStack(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());
        });

        // Register HUD overlays and tooltips
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            TeamHudRenderer.render(context);
            InviteOverlayRenderer.INSTANCE.render(context);
            SuccessOverlayRenderer.INSTANCE.render(context);
            InfoOverlayRenderer.INSTANCE.render(context);
            MarkRenderer.renderHUD(context);
        });

        // Handle invitation key presses and plugin checks on client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                TeamManager.updateTeamHealth();
            }

            if (waitingForPluginCheck) {
                pluginCheckTimer--;
                if (pluginCheckTimer <= 0) {
                    waitingForPluginCheck = false;
                    LOGGER.info("Checking for server plugin...");
                    NetworkSender.checkServerPlugin();
                }
            }
        });

        // Reset state on joining a server
        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, client, minecraftClient) -> {
            MarkManager.clearAllMarks();
            NetworkReceiver.clearAuthorizedPlayers();
            TeamManager.clearTeam();
            TeamManager.clearAllInvitations();

            NetworkSender.setServerHasPlugin(false);

            // Delay plugin check by 2 seconds (40 ticks)
            waitingForPluginCheck = true;
            pluginCheckTimer = 40;
        });

        // Clean up on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, client) -> {
            waitingForPluginCheck = false;
            pluginCheckTimer = 0;

            if (client.player != null) {
                TeamManager.removePlayer(client.player.getUuid());
                NetworkSender.sendTeamUpdate();
            }

            MarkManager.clearAllMarks();
            NetworkReceiver.clearAuthorizedPlayers();
            TeamManager.clearTeam();
            TeamManager.clearAllInvitations();

            NetworkSender.setServerHasPlugin(false);
        });

        log("Quickmark initialized");
    }

    /**
     * Registers packet types for client-side networking.
     */
    private void registerPacketTypes() {
        NetworkingInit.registerPayloads();

        ClientPlayNetworking.registerGlobalReceiver(QuickmarkPayload.ID,
                (payload, context) -> {
                    NetworkReceiver.handlePluginMessage(payload.data());
                });
    }

    public static void log(String message) {
        LOGGER.info(message);
    }
}