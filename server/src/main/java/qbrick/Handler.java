package qbrick;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

    private final Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    @SneakyThrows
    @Override
    public void run() {
        try (DataOutputStream os = new DataOutputStream(socket.getOutputStream());
             DataInputStream is = new DataInputStream(socket.getInputStream())) {
            while (true) {
                String s = is.readUTF();
                log.debug("Received: {}", s);
                os.writeUTF(s);
                os.flush();
            }
        } catch (Exception e) {
            log.error("stacktrace: ", e);
        }
    }
}