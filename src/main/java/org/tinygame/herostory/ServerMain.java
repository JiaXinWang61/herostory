package org.tinygame.herostory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {
    static private final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new HttpServerCodec(),
                        new HttpObjectAggregator(65535),
                        new WebSocketServerProtocolHandler("/websocket"),
                        new GameMsgDecoder(),//自定义解码器
                        new GameMsgEncoder(),//自定义编码器
                        new GameMsgHandler()//自定义消息处理器
                );
            }
        });

        try {
            ChannelFuture f = b.bind(12345).sync();

            if (f.isSuccess()) {
                LOGGER.info("服务器启动成功");
            }

            f.channel().closeFuture().sync();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
