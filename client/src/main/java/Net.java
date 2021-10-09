import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import qbrick.Command;

@Slf4j
public class Net {

    private static Net INSTANCE;

    private final Callback callback;
    private SocketChannel channel;

    public static Net getInstance(Callback callback) {
        if (INSTANCE == null) {
            INSTANCE = new Net(callback);
        }
        return INSTANCE;
    }

    Net(Callback callback) {
        this.callback = callback;

        Thread thread = new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();

            try {
                Bootstrap bootstrap = new Bootstrap();

                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel c) throws Exception {
                                channel = c;
                                channel.pipeline().addLast(
//                                        new StringEncoder(),
//                                        new StringDecoder(),
//                                        new ClientStringHandler(callback)
                                        new ObjectEncoder(),
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                        new ClientCommandHandler(callback)
                                );
                            }
                        });

                ChannelFuture future = bootstrap.connect("localhost", 8189).sync();
                log.debug("Client connected");
                future.channel().closeFuture().sync(); // block
            } catch (Exception e) {
                log.error("", e);
                closeChannel();
            } finally {
                group.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isConnected() {
        if (channel == null) {
            return false;
        }
        return channel.isActive();
    }

    public void sendMessage(String msg) {
        channel.writeAndFlush(msg);
    }

    public void sendCmd(Command command) {
        channel.writeAndFlush(command);
    }

    public void closeChannel() {
        channel.close();
    }

}