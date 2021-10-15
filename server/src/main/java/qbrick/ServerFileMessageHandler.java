package qbrick;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
public class ServerFileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private Path HOME_PATH = Paths.get("server", "root");
    private Path currentPath;

    private AuthService authService;
    private boolean authOk = false;

    private static int cnt = 0;
    private String name;

    private volatile long downloadStart = 0;
    private volatile int lastLength = 0;
    private FileMessage fileUploadFile;

    private final WatchService watchService = FileSystems.getDefault().newWatchService();

    public ServerFileMessageHandler(AuthService authService) throws IOException {
        this.authService = authService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        cnt++;
        name = "user#" + cnt;
        log.debug("Client {} connected!", name);
//        ctx.writeAndFlush(new PathResponse(currentPath.toString()));
//        ctx.writeAndFlush(new ListResponse(currentPath));
        ctx.writeAndFlush(new ConsoleMessage(String.format("[%s]: %s", "Server", "connected successfully")));
        Authentication startAuth = new Authentication(null, null);
        startAuth.setAuthOk(false);
        ctx.writeAndFlush(startAuth);

        Thread watchThread = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    if (key.isValid()) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for (WatchEvent<?> event : events) {
                            log.debug("kind {}, context {}", event.kind(), event.context());
                            try {
                                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                                ctx.writeAndFlush(new ListResponse(currentPath));
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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

        if (authOk) {
            int byteRead;
            switch (cmd.getType()) {
                case FILE_REQUEST:
                    FileRequest fileRequest = (FileRequest) cmd;
                    fileUploadFile = new FileMessage(currentPath.resolve(fileRequest.getName()));
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(), "r")) {
                        randomAccessFile.seek(fileUploadFile.getStarPos());
//                    lastLength = (int) randomAccessFile.length() / 10;
//                    lastLength = 1024 * 10;
                        lastLength = 1048576 / 2;
                        if (randomAccessFile.length() < lastLength) {
                            lastLength = (int) randomAccessFile.length();
                        }
                        byte[] bytes = new byte[lastLength];
                        if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                            fileUploadFile.setEndPos(byteRead);
                            fileUploadFile.setBytes(bytes);
                            ctx.writeAndFlush(fileUploadFile);
                        } else {
                        }
                        log.debug("channelActive: " + byteRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case DOWNLOAD_STATUS:
                    DownloadStatus downloadStatus = (DownloadStatus) cmd;
                    long uploadStart = downloadStatus.getStart();
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(), "r");) {
                        double pr = (double) uploadStart * 100L / randomAccessFile.length();
                        log.debug("start: " + uploadStart + "; % = " + pr);
                        fileUploadFile.setProgress(pr);
                        if (uploadStart != -1) {
                            randomAccessFile.seek(uploadStart);
                            log.debug("(randomAccessFile.length() - start)：" + (randomAccessFile.length() - uploadStart));
                            long a = randomAccessFile.length() - uploadStart;
                            if (a < lastLength) {
                                lastLength = (int) a;
                            }
                            log.debug("randomAccessFile.length()：" + randomAccessFile.length() + ",start:" + uploadStart + ",a:" + a + ",lastLength:" + lastLength);
                            byte[] bytes = new byte[lastLength];
                            log.debug("bytes.length=" + bytes.length);
                            if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - uploadStart) > 0) {
                                log.debug("byteRead = " + byteRead);
                                fileUploadFile.setEndPos(byteRead);
                                fileUploadFile.setBytes(bytes);
                                try {
                                    ctx.writeAndFlush(fileUploadFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                log.debug("byteRead channelRead()--------" + byteRead);
                                fileUploadFile.setEndPos(-1);
                                ctx.writeAndFlush(fileUploadFile);
//                                randomAccessFile.close();
                                fileUploadFile = null;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case FILE_MESSAGE:
                    FileMessage fileMessage = (FileMessage) cmd;
                    byte[] bytes = fileMessage.getBytes();
                    int endPos = fileMessage.getEndPos();
                    String fileName = fileMessage.getName();
                    String path = currentPath + File.separator + fileName;
                    File file = new File(path);
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                        if (endPos >= 0) {
                            randomAccessFile.seek(downloadStart);
                            randomAccessFile.write(bytes);
                            downloadStart = downloadStart + endPos;
                            log.debug("start: " + downloadStart + "; % = " + (float) (downloadStart * 100L / randomAccessFile.length()));
                            ctx.writeAndFlush(new DownloadStatus(downloadStart));
                        } else {
                            downloadStart = 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                Files.write(currentPath.resolve(message.getName()), message.getBytes());
//                ctx.writeAndFlush(new ListResponse(currentPath));
                    break;
                case DELETE_REQUEST:
                    DeleteRequest deleteRequest = (DeleteRequest) cmd;
                    Path srcPath = Paths.get(String.valueOf(currentPath), deleteRequest.getName());
                    File srcFile = new File(srcPath.normalize().toAbsolutePath().toString());
                    System.out.println(srcFile.getAbsoluteFile().getPath());
                    if (srcFile.isDirectory()) {
                        if (isDirectoryEmpty(srcFile)) {
                            Files.deleteIfExists(srcPath);
                        } else {
                            deleteDirRecursively(srcFile);
                            Thread.sleep(1000);
                            if (isDirectoryEmpty(srcFile)) {
                                Files.deleteIfExists(srcPath);
                            }
                        }
//                    Files.walk(srcFile.toPath())
//                            .sorted(Comparator.reverseOrder())
//                            .map(Path::toFile)
//                            .forEach(File::delete);
                    } else {
                        Files.deleteIfExists(srcPath);
                    }
                    downloadStart = 0;
                    ctx.writeAndFlush(new ListResponse(currentPath));
                    break;
                case PATH_UP_REQUEST:
                    if (currentPath.getParent() != null) {
                        if (!currentPath.equals(HOME_PATH)) {
                            currentPath = currentPath.getParent();
                        }
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
                case LIST_REQUEST:
                    currentPath = HOME_PATH;
                    if (!Files.exists(currentPath)) {
                        Files.createDirectory(currentPath);
                    }
                    ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                    ctx.writeAndFlush(new ListResponse(currentPath));
                    break;
                case CREATE_DIR_REQUEST:
                    CreateDirRequest createDirRequest = (CreateDirRequest) cmd;
                    try {
                        Files.createDirectory(Paths.get(currentPath.toString(),
                                createDirRequest.getName()));
                        createDirRequest.setPossible(true);
                        ctx.writeAndFlush(createDirRequest);
                        ctx.writeAndFlush(new ListResponse(currentPath));
                    } catch (FileAlreadyExistsException ex) {
                        createDirRequest.setPossible(false);
                        ctx.writeAndFlush(createDirRequest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CONSOLE_MESSAGE:
                    ConsoleMessage consoleMessage = (ConsoleMessage) cmd;
                    String message = consoleMessage.getMsg().trim();
                    ctx.writeAndFlush(new ConsoleMessage(String.format("[%s]: %s", name, message)));

                    if (message.equals("ls")) {
                        ctx.writeAndFlush(new ConsoleMessage(getFilesInfo()));
                    } else if (message.startsWith("cat")) {
                        try {
                            String fName = message.split(" ")[1];
                            ctx.writeAndFlush(new ConsoleMessage(getFileDataAsString(fName)));
                        } catch (Exception e) {
                            ctx.writeAndFlush(new ConsoleMessage("Command cat should be have only two args\n"));
                        }
                    } else {
                        ctx.writeAndFlush(new ConsoleMessage("Wrong command. Use cat fileName or ls\n"));
                    }
                    break;
                default:
                    ctx.writeAndFlush(new ListResponse(currentPath));
                    break;
            }
            currentPath.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
        } else {
            switch (cmd.getType()) {
                case AUTHENTICATION:
                    Authentication authentication = (Authentication) cmd;
                    authOk = authService.getAccByLoginPass(authentication.getLogin(), authentication.getPass()) != null;
                    authentication.setAuthOk(authOk);
                    ctx.writeAndFlush(authentication);
                    if (authOk) {
                        HOME_PATH = Paths.get("server", "root", authentication.getLogin());
                        currentPath = HOME_PATH;
                        if (!Files.exists(currentPath)) {
                            Files.createDirectory(currentPath);
                        }
                    }
                    break;
                case REGISTRATION:
                    break;
            }
        }
    }

    public void deleteDirRecursively(File baseDirectory) throws IOException {
        File[] files = baseDirectory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isFile()) {
                Files.deleteIfExists(Paths.get(file.getPath()));
                System.out.println(file.getName() + " файл удален");
            } else {
                while (!isDirectoryEmpty(file)) {
                    log.debug(file.getName() + " Not empty");
                    System.out.println(file.getName() + " визит");
                    deleteDirRecursively(file);
                }
                log.debug(file.getName() + " empty");
                Files.deleteIfExists(Paths.get(file.getPath()));
                System.out.println(file.getName() + " каталог удален");
            }
        }
    }

    public boolean isDirectoryEmpty(File directory) {
        String[] files = directory.list();
        assert files != null;
        return files.length == 0;
    }

    private String getFileDataAsString(String fileName) throws IOException {
        if (Files.isDirectory(currentPath.resolve(fileName))) {
            return "[ERROR] Command Cat cannot be applied to " + fileName + "\n";
        } else {
            return new String(Files.readAllBytes(currentPath.resolve(fileName))) + "\n";
        }
    }

    private String getFilesInfo() throws IOException {
        try (Stream<String> list = Files.list(currentPath).map(this::resolveFileType)) {
            return list.collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String resolveFileType(Path path) {
        if (Files.isDirectory(path)) {
            return String.format("%s\t%s", "[DIR]  ", path.getFileName().toString());
        } else {
            return String.format("%s\t%s", "[FILE]", path.getFileName().toString());
        }
    }
}