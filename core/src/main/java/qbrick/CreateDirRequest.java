package qbrick;

public class CreateDirRequest extends Command {

    private final String name;
    private boolean possible;

    public CreateDirRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isPossible() {
        return possible;
    }

    public void setPossible(boolean possible) {
        this.possible = possible;
    }

    @Override
    public CommandType getType() {
        return CommandType.CREATE_DIR_REQUEST;
    }
}
