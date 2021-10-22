package qbrick;

import lombok.Getter;

@Getter
public class RenameRequest extends Command {

    private final String name;
    private final String newName;
    private boolean renamed = false;

    public RenameRequest(String name, String newName) {
        this.name = name;
        this.newName = newName;
    }

    public void setRenamed(boolean renamed) {
        this.renamed = renamed;
    }

    @Override
    public CommandType getType() {
        return CommandType.RENAME_REQUEST;
    }
}
