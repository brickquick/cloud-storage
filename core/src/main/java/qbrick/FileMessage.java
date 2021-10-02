package qbrick;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class FileMessage extends Command {

    private final String name;
    private final byte[] bytes;

    public FileMessage(Path path) throws IOException {
        name = path.getFileName().toString();
        bytes = Files.readAllBytes(path);
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_MESSAGE;
    }

}