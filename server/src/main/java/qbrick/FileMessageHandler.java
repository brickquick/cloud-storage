package qbrick;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import qbrick.Command;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server", "root");
    private static int cnt = 0;
    private String name;


    private static Path currentPath;

    public FileMessageHandler() throws IOException {
        currentPath = Paths.get("server", "root");
        if (!Files.exists(currentPath)) {
            Files.createDirectory(currentPath);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        cnt++;
        name = "user#" + cnt;
        log.debug("Client {} connected!", name);
        ctx.writeAndFlush(String.format("[%s]: %s", "Server", "connected successfully"));
        ctx.writeAndFlush(new ListResponse(currentPath));
        ctx.writeAndFlush(new PathResponse(currentPath.toString()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
//        Files.write(
//                ROOT.resolve(fileMessage.getName()),
//                fileMessage.getBytes()
//        );
//
//        ctx.writeAndFlush("OK");
        log.debug("received: {}", cmd.getType());
        switch (cmd.getType()) {
            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) cmd;
                FileMessage msg = new FileMessage(currentPath.resolve(fileRequest.getName()));
                ctx.writeAndFlush(msg);
                break;
            case FILE_MESSAGE:
                FileMessage message = (FileMessage) cmd;
                Files.write(currentPath.resolve(message.getName()), message.getBytes());
                break;
            case PATH_UP_REQUEST:
                if (currentPath.getParent() != null) {
                    currentPath = currentPath.getParent();
                }
                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                break;
            case PATH_IN_REQUEST:
                PathInRequest request = (PathInRequest) cmd;
                Path newPath = currentPath.resolve(request.getDir());
                if (Files.isDirectory(newPath)) {
                    currentPath = newPath;
                    ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                }
                break;
//            default:
//                ctx.writeAndFlush(new ListResponse(currentPath));
//                break;
        }
        ctx.writeAndFlush(new ListResponse(currentPath));
    }
}