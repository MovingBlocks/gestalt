package org.terasology.networking.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.terasology.networking.PacketChannel;

import java.util.List;

public class InboundPacketHandler extends MessageToMessageDecoder<ByteBuf> {
    private final PacketChannel channel;

    public InboundPacketHandler(PacketChannel channel) {
        this.channel = channel;

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

    }
}
