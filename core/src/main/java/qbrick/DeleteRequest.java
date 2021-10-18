package qbrick;

import lombok.Getter;

@Getter
public class DeleteRequest extends Command {

    private final String name;

    public DeleteRequest(String name) {
        this.name = name;
    }

    @Override
    public CommandType getType() {
        return CommandType.DELETE_REQUEST;
    }

}
