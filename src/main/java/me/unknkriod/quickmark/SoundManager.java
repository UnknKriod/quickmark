package me.unknkriod.quickmark;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class SoundManager {
    private static final Identifier NORMAL_PING_SOUND = Identifier.of("quickmark:normal_ping");
    private static final Identifier DANGER_PING_SOUND = Identifier.of("quickmark:danger_ping");
    private static final Identifier INVITE_SOUND = Identifier.of("quickmark:invite");

    public static void playNormalPing(BlockPos pos) {
        playPositionedSound(NORMAL_PING_SOUND, pos);
    }

    public static void playDangerPing(BlockPos pos) {
        playPositionedSound(DANGER_PING_SOUND, pos);
    }

    public static void playInviteSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvent.of(INVITE_SOUND),
                    1.0F, 1.0F
            ));
        }
    }

    private static void playPositionedSound(Identifier soundId, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            client.getSoundManager().play(new PositionedSoundInstance(
                    SoundEvent.of(soundId),
                    net.minecraft.sound.SoundCategory.MASTER,
                    1.0F, 1.0F,
                    client.world.getRandom(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
            ));
        }
    }
}