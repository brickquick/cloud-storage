package qbrick;

public class ConsoleMessage extends Command {

    private final String msg;

    public ConsoleMessage(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public CommandType getType() {
        return CommandType.CONSOLE_MESSAGE;
    }
}
