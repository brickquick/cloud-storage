package qbrick;

public interface AuthService {
    void showTable();
    boolean isLoginBusy(String newLogin);
    boolean addAcc(String login, String pass);
    Integer getAccByLoginPass(String login, String pass);
    void start();
    void stop();
}
