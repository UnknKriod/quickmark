package me.unknkriod.quickmark.mixin;

import com.mojang.authlib.GameProfile;
import me.unknkriod.quickmark.network.NetworkReceiver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatFilterMixin {

    @Inject(
            method = "onChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void filterChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        // Получаем текст сообщения
        String content = packet.body().content();

        String ping_message = "quickmark://If you see this message, it means that you do not have the QuickMark mod installed";

        if (content.equals(ping_message)) {
            ci.cancel();
            return;
        }

        if (!NetworkReceiver.isQuickMarkMessage(content) && !NetworkReceiver.isQuickMarkAuthMessage(content)) {
            return;
        }

        UUID senderUuid = packet.sender();

        // Получаем GameProfile через список игроков
        PlayerListEntry playerEntry = MinecraftClient.getInstance()
                .getNetworkHandler()
                .getPlayerListEntry(senderUuid);

        if (playerEntry != null) {
            // Получаем полный профиль игрока
            GameProfile senderProfile = playerEntry.getProfile();

            NetworkReceiver.handleChatMessage(content, senderProfile);

            ci.cancel(); // Подавляем сообщение
        }
    }
}