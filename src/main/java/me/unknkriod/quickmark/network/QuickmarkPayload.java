package me.unknkriod.quickmark.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record QuickmarkPayload(PacketByteBuf data) implements CustomPayload {

    public static final CustomPayload.Id<QuickmarkPayload> ID =
            new CustomPayload.Id<>(Identifier.of("quickmark", "main"));

    public static final PacketCodec<PacketByteBuf, QuickmarkPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                PacketByteBuf out = new PacketByteBuf(buf);
                byte[] raw = value.data().copy().array();
                out.writeBytes(raw);
            },
            buf -> {
                PacketByteBuf in = new PacketByteBuf(buf);
                byte[] raw = new byte[in.readableBytes()];
                in.readBytes(raw);
                return new QuickmarkPayload(new PacketByteBuf(Unpooled.wrappedBuffer(raw)));
            }
    );

    public static final CustomPayload.Type<PacketByteBuf, QuickmarkPayload> TYPE =
            new CustomPayload.Type<>(ID, CODEC);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
