package qbrick;

import lombok.Getter;

@Getter
public class FileRequest extends Command {

    private final String name;

    public FileRequest(String name) {
        this.name = name;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_REQUEST;
    }

}