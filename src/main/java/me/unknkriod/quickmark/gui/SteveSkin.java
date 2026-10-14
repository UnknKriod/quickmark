package me.unknkriod.quickmark.gui;

import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

/**
 * The skin used whenever a player's real one cannot be resolved.
 * <p>
 * Takes the texture straight from vanilla's {@link DefaultSkinHelper}, so no skin file has to be
 * shipped with the mod and it always matches the running game version. Without a fallback a head
 * whose owner has left the server, or whose player-list entry carries no skin yet, drew as an empty
 * hole next to its text.
 */
public final class SteveSkin {

    public static final Identifier TEXTURE = DefaultSkinHelper.getSteve().texture();

    private SteveSkin() {}
}
