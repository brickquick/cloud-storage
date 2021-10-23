package qbrick;

public interface AuthService {
    void showTable();
    boolean addAcc(String login, String pass);
    boolean isAccExist(String login, String pass);
    boolean isAccBusy(String login);
    void releaseAcc(String login);
    void start();
    void stop();
}
