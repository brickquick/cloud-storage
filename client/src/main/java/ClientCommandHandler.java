import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import qbrick.Command;
import qbrick.FileMessage;
import qbrick.ListResponse;
import qbrick.PathResponse;

import java.nio.file.Files;
import java.util.List;

@Slf4j
public class ClientCommandHandler extends SimpleChannelInboundHandler<Command> {

    private final Callback callback;

    public ClientCommandHandler(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("received: {}", cmd.getType().toString());
        callback.call(cmd);

//        switch (cmd.getType()) {
//            case LIST_RESPONSE:
//                ListResponse response = (ListResponse) cmd;
//
//
//                break;
//            case PATH_RESPONSE:
//                PathResponse pathResponse = (PathResponse) cmd;
//                String path = pathResponse.getPath();
//                Platform.runLater(() -> serverPath.setText(path));
//                break;
//            case FILE_MESSAGE:
//                FileMessage message = (FileMessage) cmd;
//                Files.write(currentDir.resolve(message.getName()), message.getBytes());
//                refreshClientView();
//                break;
//        }

    }
}
