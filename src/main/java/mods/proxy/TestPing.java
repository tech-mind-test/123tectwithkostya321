package mods.proxy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.network.status.IClientStatusNetHandler;
import net.minecraft.network.*;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.status.client.CPingPacket;
import net.minecraft.network.status.client.CServerQueryPacket;
import net.minecraft.network.status.server.SPongPacket;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import other.bot.connection.BotNetwork;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class TestPing {
    public String state = "";

    private long pingSentAt;
    private NetworkManager pingDestination = null;
    private Proxy proxy;
    private static final ThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());

    public void run(String ip, int port, Proxy proxy) {
        this.proxy = proxy;
        TestPing.EXECUTOR.submit(() -> ping(ip, port));
    }

    private void ping(String ip, int port) {
        state = "Pinging " + ip + "...";
        NetworkManager clientConnection;
        try {
            clientConnection = createTestClientConnection(InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            state = TextFormatting.RED + "Can't connect to proxy";
            return;
        } catch (Exception e) {
            state = TextFormatting.RED + "Can't ping " + ip;
            return;
        }
        pingDestination = clientConnection;
        clientConnection.setNetHandler(new IClientStatusNetHandler() {
            private boolean successful;

            public void handleServerInfo(SServerInfoPacket packet) {
                pingSentAt = Util.milliTime();
                clientConnection.sendPacket(new CPingPacket(pingSentAt));
            }

            public void handlePong(SPongPacket packet) {
                successful = true;
                pingDestination = null;
                long pingToServer = Util.milliTime() - pingSentAt;
                state = "Ping: " + pingToServer;
                clientConnection.closeChannel(new TranslationTextComponent("multiplayer.status.finished"));
            }

            public void onDisconnect(ITextComponent reason) {
                pingDestination = null;
                if (!this.successful) {
                    state = TextFormatting.RED + "Can't ping " + ip + ": " + reason.getString();
                }
            }

            public NetworkManager getNetworkManager() {
                return clientConnection;
            }

            @Override
            public BotNetwork getBotNetwork() {
                return null;
            }


        });

        try {
            clientConnection.sendPacket(new CHandshakePacket(ip, port, ProtocolType.STATUS));
            clientConnection.sendPacket(new CServerQueryPacket());
        } catch (Throwable throwable) {
            state = TextFormatting.RED + "Can't ping " + ip;
        }
    }
    public BotNetwork getBotNetwork() {
        return null;
    }
    private NetworkManager createTestClientConnection(InetAddress address, int port) {
        final NetworkManager clientConnection = new NetworkManager(PacketDirection.CLIENTBOUND);

        (new Bootstrap()).group(NetworkManager.CLIENT_NIO_EVENTLOOP.getValue()).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException ignored) {
                }

                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new NettyVarint21FrameDecoder())
                        .addLast("decoder", new NettyPacketDecoder(PacketDirection.CLIENTBOUND))
                        .addLast("prepender", new NettyVarint21FrameEncoder())
                        .addLast("encoder", new NettyPacketEncoder(PacketDirection.SERVERBOUND))
                        .addLast("packet_handler", clientConnection);

                if (proxy.type == Proxy.ProxyType.SOCKS5) {
                    channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username, proxy.password.isEmpty() ? null : proxy.password));
                } else {
                    channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username.isEmpty() ? null : proxy.username));
                }
            }
        }).channel(NioSocketChannel.class).connect(address, port).syncUninterruptibly();
        return clientConnection;
    }

    public void pingPendingNetworks() {
        if (pingDestination != null) {
            if (pingDestination.isChannelOpen()) {
                pingDestination.tick();
            } else {
                pingDestination.handleDisconnection();
            }
        }
    }
}
