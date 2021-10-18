package qbrick;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;

@Getter
@Setter
public class FileMessage extends Command {

    private String name;
    private byte[] bytes;
    private int starPos;
    private int endPos;
    private File file;
    private double progress;

    public FileMessage(Path path) {
        name = path.getFileName().toString();
        file = new File(String.valueOf(path));
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_MESSAGE;
    }

}