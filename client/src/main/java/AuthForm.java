import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;

@Getter
public class AuthForm {
    private final Stage dialogStage;
    private Label topLabel = new Label("Аутентификация");
    private Label labelLogin = new Label("Логин:");
    private Label labelPass = new Label("Пароль:");
    private final TextField loginField = new TextField("login1");
    private final TextField passField = new TextField("pass1");

    public AuthForm() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setWidth(300);

        final VBox vBox = new VBox();
        vBox.setSpacing(5);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(topLabel);
        vBox.getChildren().add(labelLogin);
        vBox.getChildren().add(loginField);
        vBox.getChildren().add(labelPass);
        vBox.getChildren().add(passField);

        Scene scene = new Scene(vBox);
        dialogStage.setScene(scene);
    }

    public void activateForm()  {
        dialogStage.show();
    }

    public void closeForm() {
        dialogStage.close();
    }
}
