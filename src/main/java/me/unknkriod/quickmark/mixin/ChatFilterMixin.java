package me.unknkriod.quickmark.mixin;

import com.mojang.authlib.GameProfile;
import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.network.NetworkReceiver;
import me.unknkriod.quickmark.network.NetworkSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatFilterMixin {

    // Паттерн для формата: [left -> right] message
    private static final Pattern PRIVATE_MSG_PATTERN =
            Pattern.compile("^\\s*\\[(.+?)\\s*->\\s*(.+?)\\]\\s+(.*)$");

    @Inject(
            method = "onChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void filterChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
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

    @Inject(
            method = "onGameMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void filterGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String content = packet.content().getString();

        Matcher m = PRIVATE_MSG_PATTERN.matcher(content);
        if (!m.matches()) {
            return; // не похоже на личное сообщение в формате [A -> B] ...
        }

        String left = m.group(1).trim();
        String right = m.group(2).trim();
        String messagePart = m.group(3).trim();

        boolean leftIsMe = left.equalsIgnoreCase("me");
        boolean rightIsMe = right.equalsIgnoreCase("me");

        // если ни одна сторона не 'me', то это не PM, адресованный/исходящий от нас
        if (!leftIsMe && !rightIsMe) {
            return;
        }

        // определяем ник отправителя
        String senderNick;
        if (rightIsMe && !leftIsMe) {
            // формат: [sender -> me] message  -> sender = left
            senderNick = left;
        } else if (leftIsMe && !rightIsMe) {
            // формат: [me -> recipient] message  -> отправитель = мы (текущий профиль)
            senderNick = MinecraftClient.getInstance().getGameProfile().getName();
        } else {
            // на всякий: оба 'me' или непредвиденный случай — используем left
            senderNick = left;
        }

        // пытаемся найти PlayerListEntry по нику отправителя (если он онлайн)
        PlayerListEntry playerEntry = null;
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            for (PlayerListEntry e : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
                if (e != null && e.getProfile() != null
                        && senderNick.equalsIgnoreCase(e.getProfile().getName())) {
                    playerEntry = e;
                    break;
                }
            }
        }

        GameProfile senderProfile;
        if (playerEntry != null) {
            senderProfile = playerEntry.getProfile();
        } else {
            if (senderNick.equalsIgnoreCase(MinecraftClient.getInstance().getGameProfile().getName())) {
                senderProfile = MinecraftClient.getInstance().getGameProfile();
            } else {
                senderProfile = NetworkSender.getGameProfileByName(senderNick);
            }
        }

        // Теперь решаем, обрабатывать ли сообщение как QuickMark
        if (!NetworkReceiver.isQuickMarkMessage(messagePart) && !NetworkReceiver.isQuickMarkAuthMessage(messagePart)) {
            return;
        }

        NetworkReceiver.handleChatMessage(messagePart, senderProfile);
        ci.cancel();
    }
}