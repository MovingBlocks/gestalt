package org.terasology.networking.piplineFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.DatagramPacketDecoder;
import io.netty.handler.codec.DatagramPacketEncoder;
import org.terasology.networking.NetworkConfig;
import org.terasology.networking.PacketChannel;
import org.terasology.networking.internal.InboundPacketHandler;
import org.terasology.networking.internal.OutboundPacketHandler;

public class PacketPipeline extends ChannelInitializer<DuplexChannel> {
    public static String InboundPackets = "INBOUND_PACKETS";
    public static String OutboundPackets = "INBOUND_PACKETS";

    private final NetworkConfig config;

    public PacketPipeline(NetworkConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(DuplexChannel ch) throws Exception {
        PacketChannel channel = new PacketChannel(ch);
        ch.pipeline().addLast(InboundPackets, new DatagramPacketDecoder(new InboundPacketHandler(channel)));
        ch.pipeline().addLast(OutboundPackets, new DatagramPacketEncoder<>(new OutboundPacketHandler(config, channel)));
    }
}
