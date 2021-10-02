package qbrick;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path INIT_PATH = Paths.get("server", "root");
    private static Path currentPath;

    private static int cnt = 0;
    private String name;

    public FileMessageHandler() throws IOException {
        currentPath = INIT_PATH;
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
        ctx.writeAndFlush(new ConsoleMessage(String.format("[%s]: %s", "Server", "connected successfully")));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("received: {}", cmd.getType());

        switch (cmd.getType()) {
            case LIST_REQUEST:
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) cmd;
                FileMessage msg = new FileMessage(currentPath.resolve(fileRequest.getName()));
                ctx.writeAndFlush(msg);
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case FILE_MESSAGE:
                FileMessage message = (FileMessage) cmd;
                Files.write(currentPath.resolve(message.getName()), message.getBytes());
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case DELETE_REQUEST:
                DeleteRequest deleteRequest = (DeleteRequest) cmd;
                Files.deleteIfExists(currentPath.resolve(deleteRequest.getName()));
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case PATH_UP_REQUEST:
                if (currentPath.getParent() != null) {
                    currentPath = currentPath.getParent();
                }
                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case PATH_IN_REQUEST:
                PathInRequest request = (PathInRequest) cmd;
                Path newPath = currentPath.resolve(request.getDir());
                if (Files.isDirectory(newPath)) {
                    currentPath = newPath;
                    ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                }
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case CONSOLE_MESSAGE:
                ConsoleMessage consoleMessage = (ConsoleMessage) cmd;
                ctx.writeAndFlush(new ConsoleMessage(String.format("[%s]: %s", name, consoleMessage.getMsg())));
                break;
            default:
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
        }

    }

}