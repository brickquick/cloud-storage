import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;

@Getter
public class AuthForm {
    private final Stage dialogStage;
    private final Label topLabelAuth = new Label("Аутентификация");
    private final Label labelAuthLogin = new Label("Логин:");
    private final Label labelAuthPass = new Label("Пароль:");
    private final TextField loginAuthField = new TextField("lolk");
    private final PasswordField passAuthField = new PasswordField();
    private final Hyperlink regLink = new Hyperlink("Зарегистрироваться");

    private final Label topLabelReg = new Label("Регистрация");
    private final Label labelRegLogin = new Label("Придумайте логин:");
    private final TextField loginRegField = new TextField();
    private final Label labelRegPass1 = new Label("Придумайте пароль:");
    private final PasswordField passRegField1 = new PasswordField();
    private final Label labelRegPass2 = new Label("Повторите пароль:");
    private final PasswordField passRegField2 = new PasswordField();
    private final Hyperlink authLink = new Hyperlink("Войти в существующий аккаунт");

    public AuthForm() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setWidth(300);

        final VBox vBoxEnter = new VBox();
        vBoxEnter.setSpacing(5);
        vBoxEnter.setAlignment(Pos.CENTER);
        vBoxEnter.getChildren().add(topLabelAuth);
        vBoxEnter.getChildren().add(labelAuthLogin);
        vBoxEnter.getChildren().add(loginAuthField);
        vBoxEnter.getChildren().add(labelAuthPass);
        passAuthField.setText("123");
        vBoxEnter.getChildren().add(passAuthField);
        vBoxEnter.getChildren().add(regLink);

        Scene sceneAuth = new Scene(vBoxEnter);
        dialogStage.setScene(sceneAuth);

        final VBox vBoxReg = new VBox();
        vBoxReg.setSpacing(5);
        vBoxReg.setAlignment(Pos.CENTER);
        vBoxReg.getChildren().add(topLabelReg);
        vBoxReg.getChildren().add(labelRegLogin);
        vBoxReg.getChildren().add(loginRegField);
        vBoxReg.getChildren().add(labelRegPass1);
        vBoxReg.getChildren().add(passRegField1);
        vBoxReg.getChildren().add(labelRegPass2);
        vBoxReg.getChildren().add(passRegField2);
        vBoxReg.getChildren().add(authLink);

        Scene sceneReg = new Scene(vBoxReg);

        regLink.setOnAction(action -> dialogStage.setScene(sceneReg));

        authLink.setOnAction(action -> dialogStage.setScene(sceneAuth));

        loginAuthField.setOnAction(action -> {
            if (!loginAuthField.getText().equals("")) {
                passAuthField.requestFocus();
                labelAuthLogin.setTextFill(Color.color(0, 0, 0));
                labelAuthLogin.setText("Логин:");
            } else {
                labelAuthLogin.setTextFill(Color.color(1, 0, 0));
                labelAuthLogin.setText("Логин пуст:");
            }
        });

        loginRegField.setOnAction(action -> {
            if (!loginRegField.getText().equals("")) {
                passRegField1.requestFocus();
                labelRegLogin.setTextFill(Color.color(0, 0, 0));
                labelRegLogin.setText("Придумайте логин:");
            } else {
                labelRegLogin.setTextFill(Color.color(1, 0, 0));
                labelRegLogin.setText("Логин пуст:");
            }
        });

        passRegField1.setOnAction(action -> {
            if (!passRegField1.getText().equals("")) {
                passRegField2.requestFocus();
                labelRegPass1.setTextFill(Color.color(0, 0, 0));
                labelRegPass1.setText("Придумайте пароль:");
            } else {
                labelRegPass1.setTextFill(Color.color(1, 0, 0));
                labelRegPass1.setText("Пароль пуст:");
            }
        });

    }

    public void activateForm() {
        dialogStage.show();
    }

    public boolean isShowing() {
        return dialogStage.isShowing();
    }

    public void closeForm() {
        dialogStage.close();
    }
}
