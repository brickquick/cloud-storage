package qbrick;

import java.nio.file.Path;
import java.nio.file.Paths;

import qbrick.Command;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server", "root");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        // TODO: 23.09.2021 Разработка системы команд
//        Files.write(
//                ROOT.resolve(fileMessage.getName()),
//                fileMessage.getBytes()
//        );
//
//        ctx.writeAndFlush("OK");
        switch (cmd.getType()) {
            case FILE_MESSAGE:

                ctx.writeAndFlush(String.format("[%s]", cmd.getType().toString()));

        }
//        ctx.writeAndFlush(String.format("[%s]", cmd.getType().toString()));
    }
}