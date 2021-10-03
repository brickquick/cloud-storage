package qbrick;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path HOME_PATH = Paths.get("server", "root");
    private static Path currentPath;

    private static int cnt = 0;
    private String name;

    private final WatchService watchService = FileSystems.getDefault().newWatchService();

    public FileMessageHandler() throws IOException {
        currentPath = HOME_PATH;
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

        Thread watchThread = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    if (key.isValid()) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event: events) {
                            log.debug("kind {}, context {}", event.kind(), event.context());
                            ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                            ctx.writeAndFlush(new ListResponse(currentPath));
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        watchThread.setDaemon(true);
        watchThread.start();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
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
//                ctx.writeAndFlush(new ListResponse(currentPath));
                break;
            case DELETE_REQUEST:
                DeleteRequest deleteRequest = (DeleteRequest) cmd;
                Files.deleteIfExists(currentPath.resolve(deleteRequest.getName()));
//                ctx.writeAndFlush(new ListResponse(currentPath));
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

        currentPath.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
    }

}