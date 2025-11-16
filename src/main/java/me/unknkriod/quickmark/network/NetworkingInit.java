package me.unknkriod.quickmark.network;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.network.QuickmarkPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class NetworkingInit {
    private static boolean initialized = false;

    public static void registerPayloads() {
        if (initialized) {
            Quickmark.log("Payloads already registered, skipping.");
            return;
        }

        PayloadTypeRegistry.playC2S().register(QuickmarkPayload.ID, QuickmarkPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(QuickmarkPayload.ID, QuickmarkPayload.CODEC);

        initialized = true;
        Quickmark.log("Payloads registered successfully.");
    }
}