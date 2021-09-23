package qbrick.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Server {

    private static String ROOT_DIR = "server/root";

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;

    public Server() throws IOException {

        buffer = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        buffer.clear();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while (true) {
            if (read == -1) {
                channel.close();
                return;
            }
            read = channel.read(buffer);
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }

        String message = msg.toString();
        String[] tokens = message.trim().split("\\s");

        if (message.trim().startsWith("cat")) {
            if (Files.exists(Paths.get(ROOT_DIR + "/" + tokens[1]))){
                try {
                    List<String> strings = Files.readAllLines(Paths.get(ROOT_DIR + "/" + tokens[1]));
                    for (String st : strings) {
                        System.out.println(st);
                        channel.write(ByteBuffer.wrap((st + "\n" + "\r").getBytes(StandardCharsets.UTF_8)));
                    }
                } catch (MalformedInputException e) {
                    byte[] bytes = Files.readAllBytes(Paths.get(ROOT_DIR + "/" + tokens[1]));
                    System.out.println(Arrays.toString(bytes));
                    channel.write(ByteBuffer.wrap(bytes));
                }
            }

        } else if (message.trim().equals("ls")) {
            List<String> str = Files.list(Paths.get(ROOT_DIR)).map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            channel.write(ByteBuffer.wrap(("ls:" + "\n" + "\r").getBytes(StandardCharsets.UTF_8)));
            for (String st: str) {
                System.out.println(st);
                channel.write(ByteBuffer.wrap((st + "\n" + "\r").getBytes(StandardCharsets.UTF_8)));
            }

        } else {
            channel.write(ByteBuffer.wrap(("[" + LocalDateTime.now() + "]lolol123 " + message)
                    .getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
