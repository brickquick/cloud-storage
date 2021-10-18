import qbrick.Command;

public interface Callback {
    void call(Command cmd) throws Exception;
}