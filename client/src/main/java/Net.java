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
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import qbrick.Command;

@Slf4j
public class Net {

    private static final int PORT = 8189;
    private static final String HOST = "localhost";

    private static Net INSTANCE;

    private SocketChannel channel;

    public static Net getInstance(Callback callback) {
        if (INSTANCE == null) {
            INSTANCE = new Net(callback);
        }
        return INSTANCE;
    }

    Net(Callback callback) {

        Thread thread = new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();

            try {
                Bootstrap bootstrap = new Bootstrap();

                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel c) {
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

                ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
                log.debug("Client connected");
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("", e);
                closeChannel();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось подключитья к серверу", ButtonType.OK);
                    alert.showAndWait();
                });

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
        channel = null;
    }

}