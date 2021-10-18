package qbrick;

import lombok.Getter;

@Getter
public class PathResponse extends Command {

    private final String path;

    public PathResponse(String path) {
        this.path = path;
    }


    @Override
    public CommandType getType() {
        return CommandType.PATH_RESPONSE;
    }

}