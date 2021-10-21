package qbrick;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class FileMessage extends Command {

    private String name;
    private byte[] bytes;
    private int starPos;
    private int endPos;
    private double progress;
    private String strPath;

    public FileMessage(Path path) {
        name = path.getFileName().toString();
        this.strPath = path.toString();
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_MESSAGE;
    }

}