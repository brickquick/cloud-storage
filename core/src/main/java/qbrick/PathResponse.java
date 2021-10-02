package qbrick;

import java.nio.file.Paths;

public class PathResponse extends Command{
    private final String path;

    public PathResponse(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public CommandType getType() {
        return CommandType.PATH_RESPONSE;
    }

}