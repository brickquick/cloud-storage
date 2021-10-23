package qbrick;

import lombok.Getter;

@Getter
public class Authentication extends Command {
    private final String login;
    private final String pass;
    private boolean authOk = false;
    private boolean accBusy;

    public Authentication(String login, String pass) {
        this.login = login;
        this.pass = pass;
    }

    public void setAuthOk(boolean authOk) {
        this.authOk = authOk;
    }

    public void setAccBusy(boolean accBusy) {
        this.accBusy = accBusy;
    }

    @Override
    public CommandType getType() {
        return CommandType.AUTHENTICATION;
    }
}
