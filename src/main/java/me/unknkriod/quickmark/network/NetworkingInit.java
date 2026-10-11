package me.unknkriod.quickmark.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the mod's payload types.
 * <p>
 * This class runs on both the client and the dedicated server, so it must not reference the
 * client entry point {@code Quickmark} (which implements {@code ClientModInitializer} and pulls in
 * {@code net.minecraft.client} classes): on a dedicated server the loader refuses to load those
 * classes and the {@code server} entry point fails. It therefore keeps its own logger instead of
 * reusing {@code Quickmark.LOGGER}.
 */
public class NetworkingInit {
    private static final Logger LOGGER = LoggerFactory.getLogger("quickmark");

    private static boolean initialized = false;

    public static void registerPayloads() {
        if (initialized) {
            LOGGER.debug("Payloads already registered, skipping.");
            return;
        }

        PayloadTypeRegistry.playC2S().register(QuickmarkPayload.ID, QuickmarkPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(QuickmarkPayload.ID, QuickmarkPayload.CODEC);

        initialized = true;
        LOGGER.info("Payloads registered successfully.");
    }
}