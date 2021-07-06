package org.terasology.networking.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.terasology.networking.NetworkConfig;
import org.terasology.networking.PacketChannel;

import java.util.List;

public class OutboundPacketHandler extends MessageToMessageEncoder<ByteBuf> {
    private final PacketChannel channel;
    private final NetworkConfig config;
    public OutboundPacketHandler(NetworkConfig config, PacketChannel channel) {
        super(null);
        this.channel = channel;
        this.config = config;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.readableBytes() <= config.fragmentAbove) {

        }
    }


    //    @Override
//    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
//        ByteBuf buffer = (ByteBuf) msg;
//
//
//        if (buffer.readableBytes() <= config.fragmentAbove) {
//        }
//    }
}
