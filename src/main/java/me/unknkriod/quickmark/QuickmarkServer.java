package me.unknkriod.quickmark;

import me.unknkriod.quickmark.network.ServerNetworking;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickmarkServer implements DedicatedServerModInitializer {
    public static final String MOD_ID = "quickmark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("QuickMark server mod initialized!");
        ServerNetworking.initialize();
        ServerNetworking.registerTickHandler();
    }
}