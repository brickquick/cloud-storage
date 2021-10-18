package qbrick;

import lombok.Getter;

@Getter
public class PathInRequest extends Command {

    private final String dir;

    public PathInRequest(String dir) {
        this.dir = dir;
    }

    @Override
    public CommandType getType() {
        return CommandType.PATH_IN_REQUEST;
    }

}