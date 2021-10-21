package qbrick;

public interface AuthService {
    void showTable();
    boolean addAcc(String login, String pass);
    boolean isAccExist(String login, String pass);
    void start();
    void stop();
}
