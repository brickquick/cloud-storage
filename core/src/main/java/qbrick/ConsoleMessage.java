package qbrick;

import lombok.Getter;

@Getter
public class ConsoleMessage extends Command {

    private final String msg;

    public ConsoleMessage(String msg) {
        this.msg = msg;
    }

    @Override
    public CommandType getType() {
        return CommandType.CONSOLE_MESSAGE;
    }
}
