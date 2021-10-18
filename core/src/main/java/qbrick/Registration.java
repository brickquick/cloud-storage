package qbrick;

import lombok.Getter;

@Getter
public class Registration extends Command {
    private final String login;
    private final String pass;
    private boolean loginBusy;

    public Registration(String login, String pass) {
        this.login = login;
        this.pass = pass;
    }

    public boolean isLoginBusy() {
        return loginBusy;
    }

    public void setLoginBusy(boolean loginBusy) {
        this.loginBusy = loginBusy;
    }

    @Override
    public CommandType getType() {
        return CommandType.REGISTRATION;
    }
}
