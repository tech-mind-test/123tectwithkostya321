package sky.core.utils.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ServerHandler {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private Consumer<String> messageListener;

    public boolean start(int port, Consumer<String> listener) {
        this.messageListener = listener;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_REUSEADDR, true).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8), new SimpleChannelInboundHandler<String>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                            messageListener.accept(msg);
                            ctx.close();
                        }
                    });
                }
            });
            channelFuture = b.bind(port).sync();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof BindException) {
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
