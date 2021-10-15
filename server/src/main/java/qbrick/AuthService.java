package qbrick;

public interface AuthService {
    void showTable();
    boolean isLoginBusy(String newLogin);
    Integer getAccByLoginPass(String login, String pass);
    void start();
    void stop();
}
