package qbrick;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyEchoServer {

    private static final int PORT = 8189;

    private final AuthService authService;

    public NettyEchoServer() {

        authService = new BaseAuthService();

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            ChannelFuture channelFuture = bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(
//                                    new StringEncoder(),
//                                    new StringDecoder(),
//                                    new EchoHandler()
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ServerFileMessageHandler(authService)
                            );
                        }
                    })
                    .bind(PORT)
                    .sync();
            log.debug("Server started...");
            channelFuture.channel().closeFuture().sync(); // block
        } catch (Exception e) {
            log.error("Server exception: Stacktrace: ", e);
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public static void main(String[] args) {
        new NettyEchoServer();
    }

}