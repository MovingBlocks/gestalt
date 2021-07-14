package org.terasology.networking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.terasology.networking.internal.InboundPacketHandler;
import org.terasology.networking.internal.OutboundPacketHandler;

public class Server {

    public void main() {
        EventLoopGroup main = new NioEventLoopGroup(); // (1)
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(main)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
//                        PacketChannel channel = new PacketChannel(ch, null);
//                        ch.pipeline().addLast(new InboundPacketHandler(channel));
//
//                        ch.pipeline().addLast(new OutboundPacketHandler(channel));
                    }
                });
    }
}
